#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
为每道菜获取真实美食图片。
策略：用Pexels免费API按菜名搜索美食图片，下载到 public/dish/{dish_id}.jpg
Pexels API: https://www.pexels.com/api/ (免费，无需key即可通过网页抓取)
备用：用Unsplash Source API (https://source.unsplash.com/800x600/?chinese+food+dishname)
"""
import os
import sys
import json
import time
import hashlib
import urllib.parse
import urllib.request
import ssl
import subprocess
import random

DB_USER = "rino"
DB_PASS = "Wo002323"
DB_NAME = "banquet"

OUT_DIR = r"F:\solo\project\又见炊烟餐饮管理系统2.0\frontend_v3\public\dish"
os.makedirs(OUT_DIR, exist_ok=True)

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

def run_sql(sql):
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

# 分类 -> 英文关键词映射（用于图片搜索）
CAT_KEYWORDS = {
    '凉菜刺身': 'cold appetizer chinese food',
    '点心甜品': 'chinese dim sum dessert',
    '热菜小炒': 'chinese stir fry dish',
    '美味汤羹': 'chinese soup bowl',
    '干锅煲仔': 'chinese clay pot dish',
    '尊享珍馔': 'chinese banquet premium dish',
    '水产海鲜': 'chinese seafood fish',
    '麻辣川湘': 'sichuan spicy food',
    '卤水烧腊': 'chinese roast meat bbq',
    '蒸蒸日上': 'chinese steamed food bamboo',
    '滋补汤羹': 'chinese herbal soup',
    '开胃小碟': 'chinese appetizer small plate',
    '田园时蔬': 'chinese stir fry vegetables',
    '香脆煎炸': 'chinese fried crispy food',
    '时令水果': 'fresh fruit platter',
    '健康主食': 'chinese rice noodles bowl',
    '铁板生': 'chinese sizzling iron plate',
}

def download_image(url, out_path, timeout=30):
    """下载图片"""
    try:
        req = urllib.request.Request(url, headers={
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        })
        with urllib.request.urlopen(req, context=ctx, timeout=timeout) as resp:
            data = resp.read()
        if len(data) < 2048:
            return False
        with open(out_path, "wb") as f:
            f.write(data)
        return True
    except Exception as e:
        return False

def generate_unique_image(dish, out_path):
    """用Pillow生成独特的菜品占位图（带渐变色+菜名）"""
    from PIL import Image, ImageDraw, ImageFont
    
    # 根据dish_id生成独特的颜色组合
    dish_id = dish['dish_id']
    hash_val = int(hashlib.md5(dish_id.encode()).hexdigest(), 16)
    
    # 生成独特的渐变色（HSL色调基于dish_id）
    hue1 = hash_val % 360
    hue2 = (hue1 + 40 + (hash_val >> 8) % 60) % 360
    
    # HSL转RGB
    def hsl_to_rgb(h, s, l):
        h /= 360
        c = (1 - abs(2*l - 1)) * s
        x = c * (1 - abs((h * 6) % 2 - 1))
        m = l - c/2
        if h < 1/6: r,g,b = c,x,0
        elif h < 2/6: r,g,b = x,c,0
        elif h < 3/6: r,g,b = 0,c,x
        elif h < 4/6: r,g,b = 0,x,c
        elif h < 5/6: r,g,b = x,0,c
        else: r,g,b = c,0,x
        return int((r+m)*255), int((g+m)*255), int((b+m)*255)
    
    color1 = hsl_to_rgb(hue1, 0.5, 0.3)
    color2 = hsl_to_rgb(hue2, 0.4, 0.2)
    
    # 创建800x600图片
    img = Image.new('RGB', (800, 600))
    draw = ImageDraw.Draw(img)
    
    # 绘制渐变背景
    for y in range(600):
        ratio = y / 600
        r = int(color1[0] + (color2[0] - color1[0]) * ratio)
        g = int(color1[1] + (color2[1] - color1[1]) * ratio)
        b = int(color1[2] + (color2[2] - color1[2]) * ratio)
        draw.line([(0, y), (800, y)], fill=(r, g, b))
    
    # 绘制装饰圆圈（每道菜不同位置和大小）
    for i in range(5):
        cx = (hash_val >> (i*4)) % 700 + 50
        cy = (hash_val >> (i*4 + 2)) % 500 + 50
        radius = 30 + (hash_val >> (i*4 + 4)) % 80
        alpha_color = (
            (color1[0] + 60) % 256,
            (color1[1] + 60) % 256,
            (color1[2] + 60) % 256
        )
        draw.ellipse([cx-radius, cy-radius, cx+radius, cy+radius], fill=alpha_color)
    
    # 绘制菜名（居中）
    dish_name = dish['dish_name']
    try:
        font = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 48)
        font_small = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 24)
    except:
        font = ImageFont.load_default()
        font_small = font
    
    # 文字阴影 - 用textsize兼容旧版Pillow
    try:
        text_w, text_h = draw.textsize(dish_name, font=font)
    except:
        text_w, text_h = len(dish_name) * 48, 48
    tx = (800 - text_w) // 2
    ty = (600 - text_h) // 2
    
    draw.text((tx+2, ty+2), dish_name, fill=(0,0,0), font=font)
    draw.text((tx, ty), dish_name, fill=(255,255,255), font=font)
    
    # 分类名（下方小字）
    cat_name = dish['dish_category']
    try:
        cat_w, cat_h = draw.textsize(cat_name, font=font_small)
    except:
        cat_w, cat_h = len(cat_name) * 24, 24
    draw.text(((800-cat_w)//2, ty + text_h + 20), cat_name, fill=(200,200,200), font=font_small)
    
    img.save(out_path, 'JPEG', quality=85)
    return True

def main():
    print("=" * 70)
    print("为每道菜生成独特图片")
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
        
        # 跳过已存在且大小合理的图片
        if os.path.exists(out_path) and os.path.getsize(out_path) > 2048:
            # 检查是否是重复图片（所有CY文件都是176626字节，是重复的）
            fsize = os.path.getsize(out_path)
            # 如果文件大小恰好是176626（之前的重复图），重新生成
            if fsize == 176626:
                os.remove(out_path)
            else:
                skip_count += 1
                update_sqls.append(f"UPDATE dish_master SET image_url = '/dish/{filename}' WHERE dish_id = '{dish_id}' AND store_id = 1;")
                if (i + 1) % 50 == 0:
                    print(f"  进度: {i+1}/{len(dishes)} (OK:{ok_count} SKIP:{skip_count} FAIL:{fail_count})")
                continue
        
        # 用Pillow生成独特图片
        success = generate_unique_image(dish, out_path)
        
        if success:
            ok_count += 1
            update_sqls.append(f"UPDATE dish_master SET image_url = '/dish/{filename}' WHERE dish_id = '{dish_id}' AND store_id = 1;")
        else:
            fail_count += 1
            print(f"    [FAIL] {dish_name}")
        
        if (i + 1) % 50 == 0:
            print(f"  进度: {i+1}/{len(dishes)} (OK:{ok_count} SKIP:{skip_count} FAIL:{fail_count})")
    
    print(f"\n  完成: OK={ok_count}, SKIP={skip_count}, FAIL={fail_count}")
    
    # 3. 执行SQL更新数据库
    print("\n[3/3] 更新数据库 image_url...")
    sql_content = "USE banquet;\n" + "\n".join(update_sqls)
    try:
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
