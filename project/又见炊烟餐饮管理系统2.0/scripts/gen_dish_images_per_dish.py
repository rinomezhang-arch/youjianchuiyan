#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
为每道菜生成独立的真实菜品图片。
从数据库读取所有菜品，逐道调用AI生图API，保存到 public/dish/{dish_id}.jpg，
然后更新数据库 image_url 字段。
"""
import os
import sys
import json
import time
import urllib.parse
import urllib.request
import ssl
import subprocess

DB_USER = "rino"
DB_PASS = "Wo002323"
DB_HOST = "127.0.0.1"
DB_PORT = "3307"
DB_NAME = "banquet"

OUT_DIR = r"F:\solo\project\又见炊烟餐饮管理系统2.0\frontend_v3\public\dish"
os.makedirs(OUT_DIR, exist_ok=True)

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

def run_sql(sql):
    """执行SQL查询，返回结果列表"""
    cmd = [
        "docker", "exec", "-i", "youjian-mysql-local",
        "mysql", f"-u{DB_USER}", f"-p{DB_PASS}", DB_NAME,
        "-e", sql, "--batch", "--raw", "--skip-column-names"
    ]
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
        if result.returncode != 0:
            print(f"[SQL ERROR] {result.stderr.strip()}")
            return []
        lines = [l for l in result.stdout.strip().split('\n') if l]
        return lines
    except Exception as e:
        print(f"[SQL ERROR] {e}")
        return []

def get_all_dishes():
    """从数据库获取所有菜品"""
    sql = "SELECT dish_id, dish_name, dish_category, english_name FROM dish_master WHERE store_id = 1 AND (is_active IS NULL OR is_active = 1) ORDER BY dish_id;"
    lines = run_sql(sql)
    dishes = []
    for line in lines:
        parts = line.split('\t')
        if len(parts) >= 3:
            dishes.append({
                'dish_id': parts[0],
                'dish_name': parts[1],
                'dish_category': parts[2],
                'english_name': parts[3] if len(parts) > 3 else ''
            })
    return dishes

def gen_image(prompt, out_path):
    """调用AI生图API"""
    url = "https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=" + urllib.parse.quote(prompt) + "&image_size=landscape_4_3"
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=120) as resp:
            data = resp.read()
        if len(data) < 1024:
            print(f"  [WARN] Image too small ({len(data)} bytes), retrying...")
            return False
        with open(out_path, "wb") as f:
            f.write(data)
        return True
    except Exception as e:
        print(f"  [FAIL] {e}")
        return False

def build_prompt(dish):
    """根据菜名和分类构建生图prompt"""
    name = dish['dish_name']
    cat = dish['dish_category']
    en = dish.get('english_name', '')
    
    # 基础prompt：写实美食摄影
    base = "Professional Chinese food photography, top-down 45-degree angle, warm studio lighting, shallow depth of field, white ceramic plate, dark wooden table background, high resolution, appetizing"
    
    # 根据分类添加特色描述
    cat_prompts = {
        '凉菜刺身': 'cold appetizer, sliced and arranged elegantly, fresh ingredients',
        '点心甜品': 'dim sum dessert, delicate pastry, bamboo steamer or elegant plate',
        '热菜小炒': 'stir-fried hot dish, wok hei, glossy sauce, vibrant colors',
        '美味汤羹': 'hot soup in ceramic bowl, steaming, rich broth visible',
        '干锅煲仔': 'dry pot or clay pot dish, sizzling, spicy chili oil',
        '尊享珍馔': 'premium banquet dish, luxurious plating, gold accents',
        '水产海鲜': 'seafood dish, fresh fish or shrimp, elegant presentation',
        '麻辣川湘': 'Sichuan spicy dish, red chili oil, Sichuan peppercorns, bright red',
        '卤水烧腊': 'Cantonese roast meat, glossy glaze, sliced on plate',
        '蒸蒸日上': 'steamed dish, bamboo steamer, fresh and delicate',
        '滋补汤羹': 'tonic herbal soup, clay pot, rich dark broth',
        '开胃小碟': 'appetizer small plate, delicate arrangement, colorful',
        '田园时蔬': 'stir-fried green vegetables, fresh and vibrant',
        '香脆煎炸': 'crispy fried dish, golden brown, crunchy texture',
        '时令水果': 'fresh fruit platter, seasonal fruits, elegant arrangement',
        '健康主食': 'staple food, rice or noodles, simple and wholesome',
        '铁板生啫': 'iron plate sizzling dish, hot and aromatic',
    }
    
    cat_desc = cat_prompts.get(cat, 'Chinese cuisine dish')
    
    if en:
        prompt = f"{en} - {cat_desc}. {base}"
    else:
        prompt = f"Chinese dish '{name}', {cat_desc}. {base}"
    
    return prompt

def main():
    print("=" * 70)
    print("开始为每道菜生成独立图片")
    print("=" * 70)
    
    # 1. 获取所有菜品
    print("\n[1/3] 从数据库读取菜品...")
    dishes = get_all_dishes()
    print(f"  共 {len(dishes)} 道菜")
    
    if not dishes:
        print("  未获取到菜品，退出")
        return
    
    # 2. 逐道生成图片
    print("\n[2/3] 生成图片...")
    ok_count = 0
    skip_count = 0
    fail_count = 0
    update_sqls = []
    
    for i, dish in enumerate(dishes):
        dish_id = dish['dish_id']
        dish_name = dish['dish_name']
        filename = f"{dish_id}.jpg"
        out_path = os.path.join(OUT_DIR, filename)
        
        # 跳过已存在的图片
        if os.path.exists(out_path) and os.path.getsize(out_path) > 1024:
            skip_count += 1
            update_sqls.append(f"UPDATE dish_master SET image_url = '/dish/{filename}' WHERE dish_id = '{dish_id}' AND store_id = 1;")
            if (i + 1) % 50 == 0:
                print(f"  进度: {i+1}/{len(dishes)} (OK:{ok_count} SKIP:{skip_count} FAIL:{fail_count})")
            continue
        
        prompt = build_prompt(dish)
        print(f"  [{i+1}/{len(dishes)}] {dish_name} -> {filename}")
        
        # 重试机制
        success = False
        for retry in range(3):
            if gen_image(prompt, out_path):
                success = True
                break
            print(f"    重试 {retry+1}/3...")
            time.sleep(2)
        
        if success:
            ok_count += 1
            update_sqls.append(f"UPDATE dish_master SET image_url = '/dish/{filename}' WHERE dish_id = '{dish_id}' AND store_id = 1;")
        else:
            fail_count += 1
            print(f"    [FAIL] {dish_name}")
        
        # 每50道打印进度
        if (i + 1) % 50 == 0:
            print(f"  进度: {i+1}/{len(dishes)} (OK:{ok_count} SKIP:{skip_count} FAIL:{fail_count})")
        
        # 限速：每张图片间隔1秒，避免API限流
        time.sleep(1)
    
    print(f"\n  完成: OK={ok_count}, SKIP={skip_count}, FAIL={fail_count}")
    
    # 3. 输出SQL
    print("\n[3/3] 输出更新SQL...")
    sql_file = os.path.join(OUT_DIR, "update_image_url.sql")
    with open(sql_file, 'w', encoding='utf-8') as f:
        f.write("-- 更新每道菜的 image_url\n")
        f.write("USE banquet;\n\n")
        for sql in update_sqls:
            f.write(sql + "\n")
    print(f"  SQL文件: {sql_file}")
    print(f"  共 {len(update_sqls)} 条UPDATE语句")
    
    # 4. 执行SQL更新数据库
    print("\n[4/4] 执行SQL更新数据库...")
    try:
        with open(sql_file, 'r', encoding='utf-8') as f:
            sql_content = f.read()
        cmd = [
            "docker", "exec", "-i", "youjian-mysql-local",
            "mysql", f"-u{DB_USER}", f"-p{DB_PASS}", DB_NAME,
            "-e", sql_content
        ]
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=60)
        if result.returncode == 0:
            print(f"  [OK] 数据库更新成功，{len(update_sqls)} 条记录已更新")
        else:
            print(f"  [FAIL] {result.stderr.strip()}")
    except Exception as e:
        print(f"  [FAIL] {e}")
    
    print("\n" + "=" * 70)
    print("全部完成")
    print("=" * 70)

if __name__ == "__main__":
    main()
