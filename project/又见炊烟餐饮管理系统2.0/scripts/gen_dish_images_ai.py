#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
用 trae-api 内置 AI 文生图接口，为每道菜生成贴合菜名的精致菜品图。
- 接口：https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image
- 仅在会员/额度有效时可用；非会员会返回同一张默认占位图
- 脚本会校验每张图 MD5，若与默认占位图相同则跳过并记录
"""
import os, sys, time, json, ssl, hashlib, urllib.request, urllib.parse
import pymysql

DB_USER, DB_PASS, DB_NAME = "rino", "Wo002323", "banquet"
DB_HOST, DB_PORT = "localhost", 3307
OUT_DIR = r"F:\solo\project\又见炊烟餐饮管理系统2.0\frontend_v3\public\dish"
os.makedirs(OUT_DIR, exist_ok=True)

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

API_URL = "https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image"
# 已知的默认占位图 MD5（非会员限流时返回的同一张图）
KNOWN_DEFAULT_MD5 = "19a0b822edb11957055e4588c2159058"
SLEEP_BETWEEN = 1.2  # 每张图之间间隔，避免限流
TIMEOUT = 90

def build_prompt(dish_name, dish_category):
    """根据菜名+分类构造生图prompt"""
    return f"{dish_name} {dish_category} 中式菜品 餐厅级美食摄影 俯拍 精致摆盘 高清特写"

def gen_image(prompt, out_path):
    """调用AI生图接口，返回 (success, md5, msg)"""
    url = f"{API_URL}?prompt={urllib.parse.quote(prompt)}&image_size=square_hd"
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=TIMEOUT) as resp:
            ct = resp.headers.get("Content-Type", "")
            raw = resp.read()
        if "image" not in ct and not (raw[:3] == b"\xff\xd8\xff" or raw[:8] == b"\x89PNG\r\n\x1a\n"):
            return False, "", f"非图片响应 ct={ct}"
        if len(raw) < 5000:
            return False, "", f"图片太小 {len(raw)} bytes"
        md5 = hashlib.md5(raw).hexdigest()
        # 检查是否为默认占位图
        if md5 == KNOWN_DEFAULT_MD5:
            return False, md5, "默认占位图（非会员限流）"
        with open(out_path, "wb") as f:
            f.write(raw)
        return True, md5, f"{len(raw)} bytes"
    except Exception as e:
        return False, "", str(e)

def main():
    conn = pymysql.connect(host=DB_HOST, port=DB_PORT, user=DB_USER, password=DB_PASS,
                           database=DB_NAME, charset="utf8mb4")
    cur = conn.cursor()
    cur.execute("SELECT dish_id, dish_name, dish_category FROM dish_master ORDER BY dish_id")
    dishes = cur.fetchall()
    print(f"共 {len(dishes)} 道菜")

    success = 0
    default_blocked = 0
    failed = []
    default_streak = 0  # 连续默认图计数

    for idx, (dish_id, dish_name, dish_category) in enumerate(dishes, 1):
        out_path = os.path.join(OUT_DIR, f"{dish_id}.jpg")
        prompt = build_prompt(dish_name, dish_category)
        print(f"[{idx}/{len(dishes)}] {dish_id} | {dish_name} | {prompt[:40]}...")

        ok, md5, info = gen_image(prompt, out_path)
        if ok:
            print(f"    [成功] {info} md5={md5[:12]}")
            cur.execute("UPDATE dish_master SET image_url=%s WHERE dish_id=%s",
                        (f"/dish/{dish_id}.jpg", dish_id))
            conn.commit()
            success += 1
            default_streak = 0
        else:
            print(f"    [失败] {info}")
            failed.append((dish_id, dish_name, info))
            if "默认占位图" in info:
                default_blocked += 1
                default_streak += 1
                # 连续5张默认图，说明会员没生效，直接停止
                if default_streak >= 5:
                    print("\n!!! 连续5张默认占位图，会员可能未生效，停止生成 !!!")
                    break
            else:
                default_streak = 0

        time.sleep(SLEEP_BETWEEN)

    cur.close()
    conn.close()

    print(f"\n===== 完成 =====")
    print(f"成功: {success}")
    print(f"被默认图拦截: {default_blocked}")
    print(f"其他失败: {len(failed) - default_blocked}")
    if failed:
        with open(os.path.join(os.path.dirname(__file__), "ai_img_failed.txt"), "w", encoding="utf-8") as f:
            for d in failed:
                f.write(f"{d[0]}|{d[1]}|{d[2]}\n")

if __name__ == "__main__":
    main()
