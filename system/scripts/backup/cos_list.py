#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
腾讯云 COS 对象列表/下载工具（仅使用 Python 标准库）。
用法:
    python cos_list.py                       # 列出消息板目录
    python cos_list.py <前缀>                # 列出指定前缀
    python cos_list.py download <对象键> <本地文件>  # 下载对象
"""
import sys
import os
import hmac
import hashlib
import urllib.parse
import http.client
import ssl
import configparser
import xml.etree.ElementTree as ET

DEFAULT_COS_CONF = os.path.join(os.path.expanduser("~"), ".cos.conf")
MSG_BOARD_PREFIX = "ai公共工作空间/项目管理/餐饮管理系统/"


def load_cos_config():
    conf_path = os.environ.get("COS_CONFIG", DEFAULT_COS_CONF)
    if not os.path.isfile(conf_path):
        print(f"[ERROR] COS 配置文件不存在: {conf_path}")
        sys.exit(1)
    cfg = configparser.ConfigParser()
    cfg.read(conf_path, encoding="utf-8")
    section = "common"
    if section not in cfg:
        print(f"[ERROR] .cos.conf 缺少 [{section}] 段")
        sys.exit(1)
    secret_id = cfg[section].get("secret_id", "").strip()
    secret_key = cfg[section].get("secret_key", "").strip()
    bucket = cfg[section].get("bucket", "").strip()
    region = cfg[section].get("region", "ap-nanjing").strip()
    if not secret_id or not secret_key or not bucket:
        print("[ERROR] .cos.conf 缺少必填项: secret_id / secret_key / bucket")
        sys.exit(1)
    return secret_id, secret_key, bucket, region


def sign_v1(method, uri, headers_dict, params_dict, secret_id, secret_key, expire=600):
    import time
    start = int(time.time())
    end = start + expire
    key_time = f"{start};{end}"
    sign_key = hmac.new(secret_key.encode("utf-8"), key_time.encode("utf-8"), hashlib.sha1).hexdigest()
    header_list = "&".join(
        f"{k.lower()}={urllib.parse.quote(str(v), safe='')}"
        for k, v in sorted(headers_dict.items())
    )
    url_param_list = "&".join(
        f"{k.lower()}={urllib.parse.quote(str(v), safe='')}"
        for k, v in sorted(params_dict.items())
    )
    format_string = f"{method.lower()}\n{uri}\n{url_param_list}\n{header_list}\n"
    format_sha1 = hashlib.sha1(format_string.encode("utf-8")).hexdigest()
    string_to_sign = f"sha1\n{key_time}\n{format_sha1}\n"
    signature = hmac.new(
        sign_key.encode("utf-8"), string_to_sign.encode("utf-8"), hashlib.sha1
    ).hexdigest()
    auth = (
        f"q-sign-algorithm=sha1"
        f"&q-ak={secret_id}"
        f"&q-sign-time={key_time}"
        f"&q-key-time={key_time}"
        f"&q-header-list={';'.join(k.lower() for k in sorted(headers_dict))}"
        f"&q-url-param-list={';'.join(k.lower() for k in sorted(params_dict))}"
        f"&q-signature={signature}"
    )
    return auth


def list_objects(prefix="", delimiter=""):
    """列出 COS 对象"""
    secret_id, secret_key, bucket, region = load_cos_config()
    host = f"{bucket}.cos.{region}.myqcloud.com"

    params = {}
    if prefix:
        params["prefix"] = prefix
    if delimiter:
        params["delimiter"] = delimiter
    # max-keys=200 防止过多
    params["max-keys"] = "200"

    sign_headers = {"host": host}
    auth = sign_v1("get", "/", sign_headers, params, secret_id, secret_key)

    query = "&".join(f"{k}={urllib.parse.quote(str(v), safe='')}" for k, v in sorted(params.items()))
    path = f"/?{query}"

    ctx = ssl.create_default_context()
    conn = http.client.HTTPSConnection(host, context=ctx)
    headers = {"Authorization": auth, "Host": host}

    try:
        conn.request("GET", path, headers=headers)
        resp = conn.getresponse()
        body = resp.read().decode("utf-8", errors="ignore")
        if resp.status != 200:
            print(f"[ERROR] HTTP {resp.status}")
            print(body[:500])
            return [], [], False
        root = ET.fromstring(body)
        is_truncated = (root.findtext("IsTruncated") or "false").lower() == "true"
        contents = []
        for c in root.findall("Contents"):
            contents.append({
                "key": c.findtext("Key"),
                "size": int(c.findtext("Size") or "0"),
                "last_modified": c.findtext("LastModified"),
                "etag": c.findtext("ETag"),
            })
        common_prefixes = []
        for cp in root.findall("CommonPrefixes"):
            common_prefixes.append(cp.findtext("Prefix"))
        return contents, common_prefixes, is_truncated
    except Exception as e:
        print(f"[ERROR] 网络异常: {e}")
        return [], [], False
    finally:
        conn.close()


def download_object(cos_key, local_path):
    """下载 COS 对象"""
    secret_id, secret_key, bucket, region = load_cos_config()
    host = f"{bucket}.cos.{region}.myqcloud.com"

    sign_headers = {"host": host}
    encoded_key = urllib.parse.quote(cos_key, safe="/")
    auth = sign_v1("get", "/" + cos_key, sign_headers, {}, secret_id, secret_key)

    ctx = ssl.create_default_context()
    conn = http.client.HTTPSConnection(host, context=ctx)
    headers = {"Authorization": auth, "Host": host}

    try:
        encoded_key = urllib.parse.quote(cos_key, safe="/")
        conn.request("GET", "/" + encoded_key, headers=headers)
        resp = conn.getresponse()
        body = resp.read()
        if resp.status != 200:
            print(f"[ERROR] HTTP {resp.status}")
            print(body.decode("utf-8", errors="ignore")[:500])
            return False
        with open(local_path, "wb") as f:
            f.write(body)
        print(f"[OK] 下载完成: {cos_key} -> {local_path} ({len(body)} bytes)")
        return True
    except Exception as e:
        print(f"[ERROR] 网络异常: {e}")
        return False
    finally:
        conn.close()


def main():
    args = sys.argv[1:]
    if not args:
        # 默认列出消息板目录
        prefix = MSG_BOARD_PREFIX
        delimiter = "/"
    elif args[0] == "download":
        if len(args) != 3:
            print("用法: python cos_list.py download <对象键> <本地文件>")
            sys.exit(1)
        ok = download_object(args[1], args[2])
        sys.exit(0 if ok else 1)
    else:
        prefix = args[0]
        delimiter = "/"

    print(f"[INFO] 列出 COS 对象: {prefix}")
    print("=" * 70)
    contents, common_prefixes, is_truncated = list_objects(prefix, delimiter)

    if common_prefixes:
        print(f"\n[目录] ({len(common_prefixes)} 个)")
        for cp in common_prefixes:
            print(f"  📁 {cp}")

    if contents:
        print(f"\n[文件] ({len(contents)} 个)")
        for c in contents:
            size_str = f"{c['size']}B" if c['size'] < 1024 else f"{c['size']/1024:.1f}KB"
            ts = (c.get('last_modified') or '').replace('T', ' ').replace('Z', '')
            print(f"  📄 {c['key']}  ({size_str}, {ts})")

    if is_truncated:
        print("\n[WARN] 列表被截断，仅显示前 200 项")
    print("=" * 70)


if __name__ == "__main__":
    main()