#!/usr/bin/env python3
"""为每道菜生成独特的渐变+菜名图片（Pillow本地生成，不依赖AI API）"""
import os, sys, hashlib, subprocess
from PIL import Image, ImageDraw, ImageFont

DB_USER, DB_PASS, DB_NAME = "rino", "Wo002323", "banquet"
OUT_DIR = r"F:\solo\project\又见炊烟餐饮管理系统2.0\frontend_v3\public\dish"
os.makedirs(OUT_DIR, exist_ok=True)

# 分类色调映射（徽派雅致配色）
CAT_HUES = {
    '凉菜刺身': (150, 170), '点心甜品': (30, 50), '热菜小炒': (10, 30),
    '美味汤羹': (40, 60), '干锅煲仔': (15, 35), '尊享珍馔': (35, 55),
    '水产海鲜': (200, 220), '麻辣川湘': (0, 20), '卤水烧腊': (20, 40),
    '蒸蒸日上': (120, 140), '滋补汤羹': (30, 50), '开胃小碟': (45, 65),
    '田园时蔬': (100, 130), '香脆煎炸': (35, 55), '时令水果': (340, 360),
    '健康主食': (40, 60), '铁板生啫': (10, 30),
}

def hsl(h, s, l):
    h /= 360; c = (1 - abs(2*l-1)) * s; x = c * (1 - abs((h*6)%2 - 1)); m = l - c/2
    if h < 1/6: r,g,b = c,x,0
    elif h < 2/6: r,g,b = x,c,0
    elif h < 3/6: r,g,b = 0,c,x
    elif h < 4/6: r,g,b = 0,x,c
    elif h < 5/6: r,g,b = x,0,c
    else: r,g,b = c,0,x
    return int((r+m)*255), int((g+m)*255), int((b+m)*255)

def gen_dish_image(dish, out_path):
    hid = int(hashlib.md5(dish['dish_id'].encode()).hexdigest(), 16)
    cat = dish.get('dish_category', '')
    hues = CAT_HUES.get(cat, (0, 360))
    h1 = hues[0] + (hid % 20)
    h2 = hues[1] + ((hid >> 8) % 20)
    c1 = hsl(h1 % 360, 0.45, 0.25)
    c2 = hsl(h2 % 360, 0.35, 0.15)
    
    img = Image.new('RGB', (800, 600))
    draw = ImageDraw.Draw(img)
    
    # 渐变背景
    for y in range(600):
        r = int(c1[0] + (c2[0]-c1[0]) * y/600)
        g = int(c1[1] + (c2[1]-c1[1]) * y/600)
        b = int(c1[2] + (c2[2]-c1[2]) * y/600)
        draw.line([(0,y),(800,y)], fill=(r,g,b))
    
    # 装饰圆
    for i in range(6):
        cx = 80 + ((hid >> (i*5)) % 640)
        cy = 60 + ((hid >> (i*5+3)) % 480)
        rad = 40 + ((hid >> (i*5+6)) % 100)
        ac = ((c1[0]+80)%256, (c1[1]+80)%256, (c1[2]+80)%256)
        draw.ellipse([cx-rad,cy-rad,cx+rad,cy+rad], fill=ac)
    
    # 菜名
    name = dish['dish_name']
    try:
        font = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 52)
        font2 = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 22)
    except:
        font = ImageFont.load_default(); font2 = font
    
    try:
        tw, th = draw.textsize(name, font=font)
    except:
        tw, th = len(name)*52, 52
    tx, ty = (800-tw)//2, (600-th)//2 - 20
    draw.text((tx+2,ty+2), name, fill=(0,0,0), font=font)
    draw.text((tx,ty), name, fill=(255,255,255), font=font)
    
    # 分类名
    cat_name = cat
    try:
        cw, ch = draw.textsize(cat_name, font=font2)
    except:
        cw, ch = len(cat_name)*22, 22
    draw.text(((800-cw)//2, ty+th+15), cat_name, fill=(200,200,200), font=font2)
    
    img.save(out_path, 'JPEG', quality=85)
    return True

def run_sql_file(sql_content):
    cmd = ["docker","exec","-i","youjian-mysql-local","mysql",f"-u{DB_USER}",f"-p{DB_PASS}",DB_NAME]
    r = subprocess.run(cmd, input=sql_content, capture_output=True, text=True, timeout=60)
    return r.returncode == 0

def main():
    # 获取菜品
    cmd = ["docker","exec","-i","youjian-mysql-local","mysql",f"-u{DB_USER}",f"-p{DB_PASS}",DB_NAME,
           "-e","SELECT dish_id,dish_name,dish_category FROM dish_master WHERE store_id=1 AND (is_active IS NULL OR is_active=1) ORDER BY dish_id;","--batch","--raw","--skip-column-names"]
    r = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
    dishes = []
    for line in r.stdout.strip().split('\n'):
        if not line: continue
        parts = line.split('\t')
        if len(parts) >= 2:
            dishes.append({'dish_id': parts[0], 'dish_name': parts[1], 'dish_category': parts[2] if len(parts)>2 else ''})
    
    print(f"共 {len(dishes)} 道菜")
    
    ok = skip = fail = 0
    sqls = []
    
    for i, dish in enumerate(dishes):
        fname = f"{dish['dish_id']}.jpg"
        path = os.path.join(OUT_DIR, fname)
        
        if os.path.exists(path) and os.path.getsize(path) > 2048:
            skip += 1
            sqls.append(f"UPDATE dish_master SET image_url='/dish/{fname}' WHERE dish_id='{dish['dish_id']}' AND store_id=1;")
            continue
        
        if gen_dish_image(dish, path):
            ok += 1
            sqls.append(f"UPDATE dish_master SET image_url='/dish/{fname}' WHERE dish_id='{dish['dish_id']}' AND store_id=1;")
        else:
            fail += 1
        
        if (i+1) % 50 == 0:
            print(f"  {i+1}/{len(dishes)} OK={ok} SKIP={skip} FAIL={fail}")
    
    print(f"\n生成完成: OK={ok} SKIP={skip} FAIL={fail}")
    
    # 更新数据库
    sql = "USE banquet;\n" + "\n".join(sqls)
    if run_sql_file(sql):
        print(f"[OK] 数据库更新 {len(sqls)} 条")
    else:
        print("[FAIL] 数据库更新失败")

if __name__ == "__main__":
    main()
