#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""解析美团H5搜索结果，提取菜品图片"""
import urllib.request, urllib.parse, ssl, re, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Accept": "text/html",
    "Accept-Language": "zh-CN,zh;q=0.9",
}

# 美团H5搜索
url = "https://h5.waimai.meituan.com/waimai/mindex/search?keyword=" + urllib.parse.quote("红烧肉")
req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req, context=ctx, timeout=15) as resp:
    html = resp.read().decode('utf-8', errors='ignore')

# 搜索图片URL（美团CDN）
img_patterns = [
    r'https?://p\d+\.meituan\.net/[^\s"\'<>]+\.(?:jpg|jpeg|png|webp)',
    r'https?://p\d+\.meituan\.net/[^\s"\'<>]+',
    r'"imageUrl"\s*:\s*"(https?://[^"]+)"',
    r'"pic"\s*:\s*"(https?://[^"]+)"',
    r'"img"\s*:\s*"(https?://[^"]+)"',
    r'"photo"\s*:\s*"(https?://[^"]+)"',
]

found = set()
for p in img_patterns:
    matches = re.findall(p, html)
    for m in matches:
        found.add(m)

print(f"HTML length: {len(html)}")
print(f"Found {len(found)} image URLs:")
for u in list(found)[:20]:
    print(f"  {u}")

# 搜索菜品数据
data_patterns = [
    r'window\.__INITIAL_STATE__\s*=\s*(\{.*?\});',
    r'window\.__data__\s*=\s*(\{.*?\});',
    r'"dishList"\s*:\s*(\[.*?\])',
    r'"foodList"\s*:\s*(\[.*?\])',
    r'"items"\s*:\s*(\[.*?\])',
]
for p in data_patterns:
    m = re.search(p, html, re.DOTALL)
    if m:
        print(f"\nFound data pattern: {p[:30]}...")
        print(f"  Length: {len(m.group(1))}")
        print(f"  Preview: {m.group(1)[:200]}...")
        break

# 搜索所有包含"红烧肉"的上下文
for m in re.finditer(r'.{0,100}红烧肉.{0,100}', html):
    print(f"\nContext: ...{m.group()}...")
    if len(found) > 5:
        break
