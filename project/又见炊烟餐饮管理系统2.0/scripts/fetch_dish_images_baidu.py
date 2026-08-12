#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
从百度图片抓取真实菜品照片，替换本地生成的占位图。
- 数据源：百度图片搜索 acjson API（真实照片，含用户上传的菜品实拍）
- 反爬弱：只需 Referer + UA
- 输出：frontend_v3/public/dish/CY000XXX.jpg
- 数据库同步更新 image_url 字段
"""
import os, sys, time, json, ssl, hashlib, re
import urllib.request, urllib.parse
import pymysql

# ===== 配置 =====
DB_USER, DB_PASS, DB_NAME = "rino", "Wo002323", "banquet"
DB_HOST, DB_PORT = "localhost", 3307
OUT_DIR = r"F:\solo\project\又见炊烟餐饮管理系统2.0\frontend_v3\public\dish"
os.makedirs(OUT_DIR, exist_ok=True)

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Accept": "application/json, text/plain, */*",
    "Accept-Language": "zh-CN,zh;q=0.9",
    "Referer": "https://image.baidu.com/",
}

# 每道菜最多尝试多少张图（从第1张开始按顺序尝试，跳过不合适的）
MAX_CANDIDATES = 8
# 每道菜之间间隔（秒），避免被百度限流
SLEEP_BETWEEN_DISHES = 0.8
# 请求超时
TIMEOUT = 15

def search_baidu_images(keyword, rn=15):
    """调用百度图片 acjson API 搜索图片，返回候选URL列表"""
    url = ("https://image.baidu.com/search/acjson?tn=resultjson_com&logid=123&ipn=rj"
           "&ct=201326592&is=&fm=result&qp=&cl=2&lm=-1&ie=utf-8&oe=utf-8"
           "&adpicid=&st=&z=&ic=&hd=&latest=&copyright=&se=&tab=&width=&height=&face="
           "&istype=&qc=&nc=1&expermode=&nojc=&isAsync=&pn=0&rn={rn}&word={kw}").format(
        rn=rn, kw=urllib.parse.quote(keyword))
    req = urllib.request.Request(url, headers=HEADERS)
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=TIMEOUT) as resp:
            data = resp.read().decode('utf-8', errors='ignore')
        j = json.loads(data)
        candidates = []
        for item in j.get('data', []):
            if not item:
                continue
            # 优先用 middleURL（百度CDN压缩版，稳定可下载）
            murl = item.get('middleURL') or item.get('thumbURL') or item.get('hoverURL')
            if not murl or not murl.startswith('http'):
                continue
            w = item.get('width', 0) or 0
            h = item.get('height', 0) or 0
            # 跳过太小的图
            if w < 300 or h < 300:
                continue
            # 跳过极端长条图（如长截图）
            if w > 0 and h > 0:
                ratio = max(w, h) / min(w, h)
                if ratio > 2.2:
                    continue
            candidates.append({
                'url': murl,
                'w': w, 'h': h,
                'type': item.get('type', ''),
                'from': item.get('fromURL', ''),
            })
        return candidates
    except Exception as e:
        print(f"    [搜索失败] {keyword}: {e}")
        return []

def download_image(url, out_path):
    """下载图片到本地，返回是否成功"""
    try:
        req = urllib.request.Request(url, headers=HEADERS)
        with urllib.request.urlopen(req, context=ctx, timeout=TIMEOUT) as resp:
            raw = resp.read()
        if len(raw) < 3000:
            return False, "too small"
        # 校验是否为有效图片（检查文件头）
        if raw[:3] == b'\xff\xd8\xff':  # JPEG
            ext = '.jpg'
        elif raw[:8] == b'\x89PNG\r\n\x1a\n':  # PNG
            ext = '.png'
        elif raw[:4] == b'RIFF' and raw[8:12] == b'WEBP':  # WEBP
            ext = '.webp'
        elif raw[:3] == b'GIF':  # GIF
            ext = '.gif'
        else:
            # 不是有效图片
            return False, "not image"
        # 写入文件（统一保存为.jpg后缀，但内容保持原格式，浏览器能识别）
        with open(out_path, 'wb') as f:
            f.write(raw)
        return True, f"{len(raw)} bytes, {ext}, {raw[:8].hex()}"
    except Exception as e:
        return False, str(e)

def pick_keyword(dish_name, dish_category):
    """根据菜名构造搜索关键词。
    很多菜名带有修饰词（如"皖南秘汁黑猪肉"），直接搜可能搜不到好的图。
    策略：先搜全名；若失败则搜"分类+核心词"。
    """
    # 简化关键词：去掉一些修饰词
    name = dish_name
    # 去掉常见修饰前缀
    for prefix in ['皖南', '徽州', '农家', '农村', '生态', '招牌', '吉祥', '喜庆',
                   '港式', '广式', '湘味', '川味', '金陵', '芜湖', '绩溪',
                   '皇家', '鼎汤', '砂锅', '炆火', '炭烤', '北京果木']:
        if name.startswith(prefix):
            name = name[len(prefix):]
            break
    return name

def main():
    # 连数据库，读取所有菜品
    conn = pymysql.connect(host=DB_HOST, port=DB_PORT, user=DB_USER, password=DB_PASS,
                           database=DB_NAME, charset='utf8mb4')
    cur = conn.cursor()
    cur.execute("SELECT dish_id, dish_name, dish_category FROM dish_master ORDER BY dish_id")
    dishes = cur.fetchall()
    print(f"数据库共 {len(dishes)} 道菜")

    success = 0
    failed = []
    skipped = 0

    for idx, (dish_id, dish_name, dish_category) in enumerate(dishes, 1):
        out_path = os.path.join(OUT_DIR, f"{dish_id}.jpg")

        # 搜索关键词
        kw = pick_keyword(dish_name, dish_category)
        print(f"[{idx}/{len(dishes)}] {dish_id} | {dish_name} | 搜索: {kw}")

        candidates = search_baidu_images(kw, rn=15)
        if not candidates:
            # 退而求其次：只搜分类
            candidates = search_baidu_images(dish_category, rn=10)

        if not candidates:
            print(f"    [无候选] {dish_name}")
            failed.append((dish_id, dish_name, "无候选图"))
            continue

        # 按候选顺序尝试下载
        downloaded = False
        for i, cand in enumerate(candidates[:MAX_CANDIDATES]):
            ok, info = download_image(cand['url'], out_path)
            if ok:
                print(f"    [成功] 第{i+1}张 {info} ({cand['w']}x{cand['h']})")
                # 更新数据库 image_url
                cur.execute("UPDATE dish_master SET image_url=%s WHERE dish_id=%s",
                            (f"/dish/{dish_id}.jpg", dish_id))
                conn.commit()
                success += 1
                downloaded = True
                break
            else:
                print(f"    [失败] 第{i+1}张: {info}")

        if not downloaded:
            print(f"    [全部失败] {dish_name}")
            failed.append((dish_id, dish_name, "候选图全部下载失败"))

        time.sleep(SLEEP_BETWEEN_DISHES)

    cur.close()
    conn.close()

    print(f"\n===== 完成 =====")
    print(f"成功: {success} / {len(dishes)}")
    print(f"失败: {len(failed)}")
    if failed:
        print(f"\n失败列表（前20条）:")
        for d in failed[:20]:
            print(f"  {d[0]} | {d[1]} | {d[2]}")
        # 保存失败列表
        with open(os.path.join(os.path.dirname(__file__), 'meituan_img_failed.txt'), 'w', encoding='utf-8') as f:
            for d in failed:
                f.write(f"{d[0]}|{d[1]}|{d[2]}\n")
        print(f"\n失败列表已保存到 scripts/meituan_img_failed.txt")

if __name__ == '__main__':
    main()
