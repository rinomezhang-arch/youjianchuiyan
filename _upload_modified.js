import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('1.13.173.213', username='ubuntu', key_filename=r'f:\solo\cos_server_key.pem', timeout=10)

# 先上传到临时目录
sftp = ssh.open_sftp()
sftp.put('f:\\solo\\index.js', '/home/ubuntu/index.js')
sftp.close()

print("文件已上传到临时目录")

# 使用sudo复制到目标位置
def run(cmd):
    stdin, stdout, stderr = ssh.exec_command(cmd)
    return stdout.read().decode('utf-8', errors='ignore') + stderr.read().decode('utf-8', errors='ignore')

result = run("sudo cp /home/ubuntu/index.js /var/www/html/canyin/assets/index-B7KUq2yZ.js")
print("文件已复制到目标位置")

# 验证修改
result = run("grep -c '团队聊天' /var/www/html/canyin/assets/index-B7KUq2yZ.js")
print(f"\n服务器上'团队聊天'出现次数: {result}")

ssh.close()
