#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
按分类生成菜品代表图，下载到 frontend_v3/public/dish/ 目录。
然后输出 SQL 用于更新 dish_master.image_url 字段（同分类共用一张图）。
"""
import os
import sys
import urllib.parse
import urllib.request
import ssl

OUT_DIR = r"F:\solo\project\又见炊烟餐饮管理系统2.0\frontend_v3\public\dish"
os.makedirs(OUT_DIR, exist_ok=True)

# 分类 -> (文件名, prompt)  prompt遵循SDXL最佳实践，写实美食摄影
CATEGORIES = [
    ("凉菜刺身", "cold_dish", "Chinese cold appetizer platter, sliced sashimi, elegant plating, dark wooden table, top view, professional food photography, natural light"),
    ("点心甜品", "dessert", "Chinese dim sum dessert, bamboo steamer, delicate pastries, warm tone, professional food photography"),
    ("热菜小炒", "stir_fry", "Chinese stir fry hot dish, wok cooking, glossy red brown sauce, white ceramic plate, professional food photography"),
    ("美味汤羹", "soup", "Chinese hot soup bowl, ceramic bowl, steaming, clear broth, professional food photography"),
    ("干锅煲仔", "clay_pot", "Chinese dry pot clay pot dish, spicy chili oil, iron pot, professional food photography"),
    ("尊享珍馔", "premium", "premium Chinese banquet dish, abalone sea cucumber fish maw, golden soup, elegant plating, professional food photography"),
    ("水产海鲜", "seafood", "Chinese seafood dish, steamed fish, fresh shrimp shellfish, white plate, professional food photography"),
    ("麻辣川湘", "spicy", "Sichuan spicy dish, red chili oil, Sichuan peppercorn, bright red, professional food photography"),
    ("卤水烧腊", "roast_meat", "Cantonese roast meat, roast duck goose, glossy red skin, hanging, professional food photography"),
    ("蒸蒸日上", "steamed", "Chinese steamed dish, bamboo steamer, fresh ingredients, professional food photography"),
    ("滋补汤羹", "tonic_soup", "Chinese tonic soup, herbal ingredients, clay pot, dark broth, professional food photography"),
    ("开胃小碟", "appetizer", "Chinese appetizer small plates, delicate small dishes, professional food photography"),
    ("田园时蔬", "vegetable", "Chinese stir fried vegetables, green leafy vegetables, clear wok, white plate, professional food photography"),
    ("香脆煎炸", "fried", "Chinese fried food, golden crispy, tempura style, professional food photography"),
    ("时令水果", "fruit", "fresh fruit platter, seasonal fruits, elegant arrangement, professional food photography"),
    ("健康主食", "staple", "Chinese healthy staple food, rice noodles, simple bowl, professional food photography"),
    ("铁板生啫", "iron_plate", "Chinese iron plate sizzling dish, hot iron plate, ingredients, professional food photography"),
]

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

def gen_image(prompt, out_path):
    url = "https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=" + urllib.parse.quote(prompt) + "&image_size=landscape_4_3"
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=120) as resp:
            data = resp.read()
        with open(out_path, "wb") as f:
            f.write(data)
        print(f"[OK] {os.path.basename(out_path)} ({len(data)} bytes)")
        return True
    except Exception as e:
        print(f"[FAIL] {os.path.basename(out_path)}: {e}")
        return False

def main():
    ok_list = []
    for cn_name, code, prompt in CATEGORIES:
        out_path = os.path.join(OUT_DIR, code + ".jpg")
        if os.path.exists(out_path) and os.path.getsize(out_path) > 1024:
            print(f"[SKIP] {code}.jpg already exists")
            ok_list.append((cn_name, code))
            continue
        if gen_image(prompt, out_path):
            ok_list.append((cn_name, code))

    # 输出SQL
    print("\n" + "=" * 70)
    print("-- SQL: 按分类更新 image_url")
    print("=" * 70)
    for cn_name, code in ok_list:
        url = f"/dish/{code}.jpg"
        # 转义单引号
        safe_cn = cn_name.replace("'", "''")
        print(f"UPDATE banquet.dish_master SET image_url = '{url}' WHERE dish_category = '{safe_cn}' AND (image_url IS NULL OR image_url = '');")
    print("=" * 70)
    print(f"Done: {len(ok_list)}/{len(CATEGORIES)} categories")

if __name__ == "__main__":
    main()
