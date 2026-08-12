import urllib.request, ssl, hashlib

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

urls = [
    'https://images.pexels.com/photos/1640777/pexels-photo-1640777.jpeg?auto=compress&cs=tinysrgb&w=800',
    'https://images.pexels.com/photos/2474661/pexels-photo-2474661.jpeg?auto=compress&cs=tinysrgb&w=800',
    'https://images.pexels.com/photos/45170/kitchen-stove-cooking-chef-45170.jpeg?auto=compress&cs=tinysrgb&w=800',
]
for url in urls:
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=15) as resp:
            data = resp.read()
        md5 = hashlib.md5(data).hexdigest()
        print(f'OK: {len(data)} bytes MD5={md5} from {url.split("/")[-1][:40]}')
    except Exception as e:
        print(f'FAIL: {e}')
