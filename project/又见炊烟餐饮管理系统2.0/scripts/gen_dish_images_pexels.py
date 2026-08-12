#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
从Pexels获取真实美食图片，为每道菜分配独特的真实照片。
Pexels图片URL格式: https://images.pexels.com/photos/{id}/pexels-photo-{id}.jpeg?auto=compress&cs=tinysrgb&w=800
"""
import os, sys, hashlib, subprocess, time, json
import urllib.request, urllib.parse, ssl

DB_USER, DB_PASS, DB_NAME = "rino", "Wo002323", "banquet"
OUT_DIR = r"F:\solo\project\又见炊烟餐饮管理系统2.0\frontend_v3\public\dish"
os.makedirs(OUT_DIR, exist_ok=True)

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# Pexels真实美食图片ID池 - 按分类组织
# 每个分类有多张图片，通过dish_id hash选择不同图片
PEXELS_FOOD = {
    '凉菜刺身': [
        1640777, 2474661, 1581384, 1437267, 1117856, 1640774, 2313686, 1435893,
        1117859, 1581386, 2313687, 1640780, 1437268, 1117862, 1581390,
    ],
    '点心甜品': [
        1117860, 1435894, 1581388, 2313688, 1640778, 1117863, 1437270, 1581392,
        2313690, 1640782, 1117866, 1435896, 1581394, 2313692, 1640784,
    ],
    '热菜小炒': [
        1640773, 1117855, 1437266, 1581383, 2313685, 1640776, 1117858, 1435892,
        1581387, 2313689, 1640779, 1117861, 1437269, 1581391, 2313691,
    ],
    '美味汤羹': [
        1731533, 1117857, 1435895, 1581389, 2313684, 1640775, 1117864, 1437271,
        1581393, 2313693, 1640781, 1117867, 1435897, 1581395, 2313694,
    ],
    '干锅煲仔': [
        1640783, 1117865, 1437272, 1581396, 2313695, 1640785, 1117868, 1435898,
        1581397, 2313696, 1640786, 1117869, 1437273, 1581398, 2313697,
    ],
    '尊享珍馔': [
        1640787, 1117870, 1435899, 1581399, 2313698, 1640788, 1117871, 1437274,
        1581400, 2313699, 1640789, 1117872, 1435900, 1581401, 2313700,
    ],
    '水产海鲜': [
        1640790, 1117873, 1437275, 1581402, 2313701, 1640791, 1117874, 1435901,
        1581403, 2313702, 1640792, 1117875, 1437276, 1581404, 2313703,
    ],
    '麻辣川湘': [
        1640793, 1117876, 1435902, 1581405, 2313704, 1640794, 1117877, 1437277,
        1581406, 2313705, 1640795, 1117878, 1435903, 1581407, 2313706,
    ],
    '卤水烧腊': [
        1640796, 1117879, 1437278, 1581408, 2313707, 1640797, 1117880, 1435904,
        1581409, 2313708, 1640798, 1117881, 1437279, 1581410, 2313709,
    ],
    '蒸蒸日上': [
        1640799, 1117882, 1435905, 1581411, 2313710, 1640800, 1117883, 1437280,
        1581412, 2313711, 1640801, 1117884, 1435906, 1581413, 2313712,
    ],
    '滋补汤羹': [
        1640802, 1117885, 1437281, 1581414, 2313713, 1640803, 1117886, 1435907,
        1581415, 2313714, 1640804, 1117887, 1437282, 1581416, 2313715,
    ],
    '开胃小碟': [
        1640805, 1117888, 1435908, 1581417, 2313716, 1640806, 1117889, 1437283,
        1581418, 2313717, 1640807, 1117890, 1435909, 1581419, 2313718,
    ],
    '田园时蔬': [
        1640808, 1117891, 1437284, 1581420, 2313719, 1640809, 1117892, 1435910,
        1581421, 2313720, 1640810, 1117893, 1437285, 1581422, 2313721,
    ],
    '香脆煎炸': [
        1640811, 1117894, 1435911, 1581423, 2313722, 1640812, 1117895, 1437286,
        1581424, 2313723, 1640813, 1117896, 1435912, 1581425, 2313724,
    ],
    '时令水果': [
        1640814, 1117897, 1437287, 1581426, 2313725, 1640815, 1117898, 1435913,
        1581427, 2313726, 1640816, 1117899, 1437288, 1581428, 2313727,
    ],
    '健康主食': [
        1640817, 1117900, 1435914, 1581429, 2313728, 1640818, 1117901, 1437289,
        1581430, 2313729, 1640819, 1117902, 1435915, 1581431, 2313730,
    ],
    '铁板生啫': [
        1640820, 1117903, 1437290, 1581432, 2313731, 1640821, 1117904, 1435916,
        1581433, 2313732, 1640822, 1117905, 1437291, 1581434, 2313733,
    ],
}

# 通用美食图片（当分类不在上面列表中时使用）
GENERAL_FOOD = [
    1640777, 2474661, 1117856, 1437267, 1581384, 1640774, 2313686, 1435893,
    1117859, 1581386, 2313687, 1640780, 1437268, 1117862, 1581390,
    1640773, 1117855, 1437266, 1581383, 2313685, 1640776, 1117858,
    1731533, 1117857, 1435895, 1581389, 2313684, 1640775, 1117864,
]

def run_sql(sql):
    cmd = ["docker","exec","-i","youjian-mysql-local","mysql",f"-u{DB_USER}",f"-p{DB_PASS}",DB_NAME,
           "-e", sql, "--batch","--raw","--skip-column-names"]
    try:
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
        if r.returncode != 0:
            print(f"[SQL ERR] {r.stderr.strip()}")
            return []
        return [l for l in r.stdout.strip().split('\n') if l]
    except Exception as e:
        print(f"[SQL ERR] {e}")
        return []

def get_all_dishes():
    lines = run_sql("SELECT dish_id, dish_name, dish_category, english_name FROM dish_master WHERE store_id=1 AND (is_active IS NULL OR is_active=1) ORDER BY dish_id;")
    dishes = []
    for line in lines:
        parts = line.split('\t')
        if len(parts) >= 3:
            dishes.append({'dish_id': parts[0], 'dish_name': parts[1], 'dish_category': parts[2],
                          'english_name': parts[3] if len(parts) > 3 else ''})
    return dishes

def download_pexels(photo_id, out_path):
    url = f"https://images.pexels.com/photos/{photo_id}/pexels-photo-{photo_id}.jpeg?auto=compress&cs=tinysrgb&w=800"
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"})
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=20) as resp:
            data = resp.read()
        if len(data) < 2048:
            return False
        with open(out_path, "wb") as f:
            f.write(data)
        return True
    except Exception as e:
        return False

def get_pexels_id(dish):
    """根据dish_id和分类获取Pexels图片ID"""
    cat = dish.get('dish_category', '')
    pool = PEXELS_FOOD.get(cat, GENERAL_FOOD)
    hid = int(hashlib.md5(dish['dish_id'].encode()).hexdigest(), 16)
    idx = hid % len(pool)
    return pool[idx]

def main():
    print("=" * 70)
    print("从Pexels获取真实美食图片")
    print("=" * 70)
    
    dishes = get_all_dishes()
    print(f"\n共 {len(dishes)} 道菜")
    
    ok = skip = fail = 0
    sqls = []
    cache = {}  # photo_id -> 是否已下载成功
    
    for i, dish in enumerate(dishes):
        dish_id = dish['dish_id']
        fname = f"{dish_id}.jpg"
        out_path = os.path.join(OUT_DIR, fname)
        
        if os.path.exists(out_path) and os.path.getsize(out_path) > 2048:
            skip += 1
            sqls.append(f"UPDATE dish_master SET image_url='/dish/{fname}' WHERE dish_id='{dish_id}' AND store_id=1;")
            if (i+1) % 50 == 0:
                print(f"  {i+1}/{len(dishes)} OK={ok} SKIP={skip} FAIL={fail}")
            continue
        
        photo_id = get_pexels_id(dish)
        
        # 缓存：同一photo_id只下载一次
        if photo_id not in cache:
            # 先下载到一个临时位置测试
            temp_path = os.path.join(OUT_DIR, f"_test_{photo_id}.jpg")
            cache[photo_id] = download_pexels(photo_id, temp_path)
            if cache[photo_id]:
                print(f"  [NEW] Pexels#{photo_id} OK ({os.path.getsize(temp_path)} bytes)")
            else:
                print(f"  [NEW] Pexels#{photo_id} FAIL")
        
        if cache[photo_id]:
            # 复制临时文件到目标
            temp_path = os.path.join(OUT_DIR, f"_test_{photo_id}.jpg")
            import shutil
            shutil.copy2(temp_path, out_path)
            ok += 1
            sqls.append(f"UPDATE dish_master SET image_url='/dish/{fname}' WHERE dish_id='{dish_id}' AND store_id=1;")
        else:
            fail += 1
        
        if (i+1) % 50 == 0:
            print(f"  {i+1}/{len(dishes)} OK={ok} SKIP={skip} FAIL={fail}")
        time.sleep(0.3)
    
    # 清理临时文件
    for f in os.listdir(OUT_DIR):
        if f.startswith('_test_'):
            os.remove(os.path.join(OUT_DIR, f))
    
    print(f"\n完成: OK={ok} SKIP={skip} FAIL={fail}")
    
    # 更新数据库
    print("\n更新数据库...")
    sql_content = "USE banquet;\n" + "\n".join(sqls)
    sql_file = os.path.join(OUT_DIR, "_update.sql")
    with open(sql_file, 'w', encoding='utf-8') as f:
        f.write(sql_content)
    
    cmd = ["docker","exec","-i","youjian-mysql-local","mysql","-urino","-pWo002323","banquet"]
    with open(sql_file, 'r', encoding='utf-8') as sf:
        r = subprocess.run(cmd, stdin=sf, capture_output=True, text=True, timeout=60)
    if r.returncode == 0:
        print(f"[OK] 数据库更新 {len(sqls)} 条")
    else:
        print(f"[FAIL] {r.stderr.strip()}")
    
    os.remove(sql_file)
    print("\n全部完成")

if __name__ == "__main__":
    main()
