#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""小批量测试：抓5道菜的图片"""
import sys, os, time
sys.path.insert(0, r'f:\solo\project\又见炊烟餐饮管理系统2.0\scripts')
from fetch_dish_images_baidu import search_baidu_images, download_image, pick_keyword

OUT_DIR = r'f:\solo\project\又见炊烟餐饮管理系统2.0\frontend_v3\public\dish'
tests = [
    ('CY000001', '精美四冷盘', '凉菜刺身'),
    ('CY000018', '家常红烧肉锅', '干锅煲仔'),
    ('CY000025', '白灼北海大虾皇', '水产海鲜'),
    ('CY000100', '北京果木碳烤鸭', '卤水烧腊'),
    ('CY000040', '蒜蓉粉丝蒸鲍鱼', '蒸蒸日上'),
]
for dish_id, dish_name, cat in tests:
    kw = pick_keyword(dish_name, cat)
    print(f'=== {dish_id} | {dish_name} | kw={kw} ===')
    cands = search_baidu_images(kw, rn=10)
    print(f'  候选: {len(cands)} 张')
    out_path = os.path.join(OUT_DIR, dish_id + '.jpg')
    got = False
    for i, c in enumerate(cands[:5]):
        w, h = c['w'], c['h']
        ok, info = download_image(c['url'], out_path)
        if ok:
            print(f'  [OK] 第{i+1}张 {info} ({w}x{h})')
            got = True
            break
        else:
            print(f'  [X] 第{i+1}张: {info}')
    if not got:
        print('  [全部失败]')
    time.sleep(1)
