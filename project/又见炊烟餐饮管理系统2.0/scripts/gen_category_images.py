#!/usr/bin/env python3
"""为17个菜品分类生成不同的渐变图片，每张图片有分类名称。"""
import os
from PIL import Image, ImageDraw, ImageFont

OUT_DIR = r'F:\solo\project\又见炊烟餐饮管理系统2.0\frontend_v3\public\dish'
FONT_PATH = r'C:\Windows\Fonts\simhei.ttf'
W, H = 800, 600  # 4:3

# 17个分类: (文件名, 中文名, 英文名, 起始色, 结束色, 装饰emoji)
CATEGORIES = [
    ('cold_dish',   '凉菜刺身', 'Cold Dish',      (45, 90, 78),   (26, 58, 46),   '🥗'),
    ('dessert',     '点心甜品', 'Dessert',        (180, 130, 100),(120, 80, 60),  '🍰'),
    ('stir_fry',    '热菜小炒', 'Stir Fry',       (160, 70, 50),  (100, 40, 30),  '🔥'),
    ('soup',        '美味汤羹', 'Soup',           (200, 160, 80), (140, 100, 40), '🍲'),
    ('clay_pot',    '干锅煲仔', 'Clay Pot',       (130, 60, 40),  (80, 35, 20),   '🫕'),
    ('premium',     '尊享珍馔', 'Premium',        (180, 140, 60), (120, 80, 30),  '👑'),
    ('seafood',     '水产海鲜', 'Seafood',        (50, 110, 130), (30, 70, 90),   '🦐'),
    ('spicy',       '麻辣川湘', 'Spicy',          (190, 50, 50),  (120, 30, 30),  '🌶'),
    ('roast_meat',  '卤水烧腊', 'Roast Meat',     (140, 70, 30),  (90, 45, 15),   '🍖'),
    ('steamed',     '蒸蒸日上', 'Steamed',        (80, 140, 100), (50, 100, 70),  '♨'),
    ('tonic_soup',  '滋补汤羹', 'Tonic Soup',     (170, 130, 50), (110, 80, 25),  '🍵'),
    ('appetizer',   '开胃小碟', 'Appetizer',      (130, 110, 60), (85, 70, 35),   '🥢'),
    ('vegetable',   '田园时蔬', 'Vegetable',      (80, 130, 60),  (45, 90, 30),   '🥬'),
    ('fried',       '香脆煎炸', 'Fried',          (210, 160, 50), (150, 110, 25), '🍗'),
    ('fruit',       '时令水果', 'Fruit',          (180, 80, 100), (120, 50, 70),  '🍇'),
    ('staple',      '健康主食', 'Staple',         (200, 175, 100),(140, 115, 50), '🍚'),
    ('iron_plate',  '铁板生啫', 'Iron Plate',     (120, 50, 40),  (70, 25, 20),   '🍽'),
]

def make_gradient(draw, w, h, c1, c2):
    """绘制对角线渐变。"""
    for y in range(h):
        t = y / h
        r = int(c1[0] + (c2[0] - c1[0]) * t)
        g = int(c1[1] + (c2[1] - c1[1]) * t)
        b = int(c1[2] + (c2[2] - c1[2]) * t)
        draw.line([(0, y), (w, y)], fill=(r, g, b))

def add_texture(draw, w, h, color):
    """添加细微纹理。"""
    import random
    random.seed(42)
    for _ in range(500):
        x = random.randint(0, w)
        y = random.randint(0, h)
        r = random.randint(1, 3)
        alpha = random.randint(10, 30)
        overlay = color
        draw.ellipse([x-r, y-r, x+r, y+r], fill=overlay)

def draw_dish_icon(draw, cx, cy, size, color):
    """绘制简单的碗碟图标。"""
    # 碗外形（椭圆）
    draw.ellipse([cx-size, cy-size//3, cx+size, cy+size//3],
                 fill=color, outline=(255,255,255,80), width=3)
    # 碗内部（椭圆）
    draw.ellipse([cx-size+10, cy-size//3+5, cx+size-10, cy+size//3-5],
                 fill=(color[0]//2, color[1]//2, color[2]//2))

def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    font_big = ImageFont.truetype(FONT_PATH, 72)
    font_small = ImageFont.truetype(FONT_PATH, 32)

    for code, cn, en, c1, c2, emoji in CATEGORIES:
        img = Image.new('RGB', (W, H))
        draw = ImageDraw.Draw(img)

        # 渐变背景
        make_gradient(draw, W, H, c1, c2)
        add_texture(draw, W, H, c1)

        # 绘制碗碟图标（中间偏上）
        icon_color = (min(c1[0]+60, 255), min(c1[1]+60, 255), min(c1[2]+60, 255))
        draw_dish_icon(draw, W//2, H//2 - 80, 120, icon_color)

        # 半透明遮罩（底部文字区）
        for y in range(H - 200, H):
            t = (y - (H - 200)) / 200
            alpha = int(180 * t)
            draw.line([(0, y), (W, y)], fill=(0, 0, 0))

        # 中文分类名
        tw, th = font_big.getsize(cn)
        tx = (W - tw) // 2
        draw.text((tx, H - 130), cn, fill=(255, 255, 255), font=font_big)

        # 英文分类名
        tw, th = font_small.getsize(en)
        tx = (W - tw) // 2
        draw.text((tx, H - 50), en, fill=(255, 220, 150), font=font_small)

        # 装饰边框
        draw.rectangle([0, 0, W-1, H-1], outline=(255, 255, 255, 30), width=4)

        out_path = os.path.join(OUT_DIR, f'{code}.jpg')
        img.save(out_path, 'JPEG', quality=85)
        print(f'OK: {code}.jpg ({os.path.getsize(out_path)} bytes)')

    print(f'\nDone! {len(CATEGORIES)} images generated.')

if __name__ == '__main__':
    main()
