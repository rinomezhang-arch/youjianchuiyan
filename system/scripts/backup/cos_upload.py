# -*- coding: utf-8 -*-
"""
腾讯云 COS XML API 通用上传工具（仅使用 Python 标准库，无需第三方 SDK）。

用法:
    python cos_upload.py <本地文件路径> <COS对象键>

凭证:
    自动从用户目录 .cos.conf 读取（INI 格式），或通过环境变量 COS_CONFIG 指定路径。
    .cos.conf 格式:
        [common]
        secret_id  = AKIDxxxx
        secret_key = xxxx
        bucket     = your-bucket-name
        region     = ap-nanjing

示例:
    python cos_upload.py backup.sql "ai公共工作空间/项目管理/餐饮管理系统/备份/backup.sql"
"""
import sys
import os
import hmac
import hashlib
import urllib.parse
import http.client
import ssl
import configparser

DEFAULT_COS_CONF = os.path.join(os.path.expanduser("~"), ".cos.conf")


def load_cos_config():
    """从 .cos.conf 或环境变量加载 COS 凭证。"""
    conf_path = os.environ.get("COS_CONFIG", DEFAULT_COS_CONF)
    if not os.path.isfile(conf_path):
        print(f"[ERROR] COS 配置文件不存在: {conf_path}")
        print(f"[INFO] 请创建 {conf_path}，格式见脚本顶部注释")
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
    """生成 COS XML API v1 签名。"""
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


def upload_file(local_path, cos_key):
    """上传本地文件到 COS。"""
    secret_id, secret_key, bucket, region = load_cos_config()
    host = f"{bucket}.cos.{region}.myqcloud.com"

    if not os.path.isfile(local_path):
        print(f"[ERROR] 本地文件不存在: {local_path}")
        return False

    file_size = os.path.getsize(local_path)
    print(f"[INFO] 本地文件: {local_path}")
    print(f"[INFO] 文件大小: {file_size} bytes ({file_size/1024/1024:.2f} MB)")
    print(f"[INFO] COS 桶: {bucket} ({region})")
    print(f"[INFO] COS 对象键: {cos_key}")

    with open(local_path, "rb") as f:
        body = f.read()

    sign_headers = {
        "Content-Length": str(file_size),
        "Content-Type": "application/octet-stream",
    }
    sign_params = {}

    auth = sign_v1("put", "/" + cos_key, sign_headers, sign_params, secret_id, secret_key)

    encoded_key = urllib.parse.quote(cos_key, safe="/")
    ctx = ssl.create_default_context()
    conn = http.client.HTTPSConnection(host, context=ctx)
    headers = {
        "Authorization": auth,
        "Content-Type": "application/octet-stream",
        "Content-Length": str(file_size),
    }

    print(f"[INFO] 上传中... https://{host}/{encoded_key}")
    try:
        conn.request("PUT", "/" + encoded_key, body=body, headers=headers)
        resp = conn.getresponse()
        resp_body = resp.read().decode("utf-8", errors="ignore")
        if resp.status == 200:
            print(f"[OK] 上传成功! HTTP {resp.status}")
            print(f"[OK] URL: https://{host}/{encoded_key}")
            return True
        else:
            print(f"[ERROR] 上传失败 HTTP {resp.status}")
            if resp_body:
                print(f"[ERROR] 响应: {resp_body[:500]}")
            return False
    except Exception as e:
        print(f"[ERROR] 网络异常: {e}")
        return False
    finally:
        conn.close()


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("用法: python cos_upload.py <本地文件路径> <COS对象键>")
        print("示例: python cos_upload.py backup.sql 'ai公共工作空间/项目管理/餐饮管理系统/备份/backup.sql'")
        sys.exit(1)
    local_file = sys.argv[1]
    cos_object_key = sys.argv[2]
    ok = upload_file(local_file, cos_object_key)
    sys.exit(0 if ok else 1)
