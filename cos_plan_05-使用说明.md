# 又见炊烟私房菜 · 餐饮管理系统 — 使用说明

## 访问地址
- 网站：http://youjianchuiyan.com
- 登录：rino / 002323

## 本地开发

### 天龙🦞（后端）
- 源码交付：写在 /mnt/cos/天地双龙工作空间/java-backend/ 对应目录
- 地龙编译后 SCP JAR 到 /home/ubuntu/banquet-api.jar
- 部署命令：
```bash
# 备份旧JAR
cp /home/ubuntu/banquet-api.jar /home/ubuntu/banquet-api.jar.bak

# 停止旧进程
pkill -f banquet-api.jar

# 启动新JAR
nohup java -jar /home/ubuntu/banquet-api.jar --server.port=8080 > /tmp/banquet.log 2>&1 &

# 验证
curl -s http://127.0.0.1:8080/api/bookings | head -c 200
```

### 地龙🐉（前端+Maven编译）
- 源码：F:\trae\（待确认）
- 编译：`mvn clean package -DskipTests`
- 部署：`scp target/banquet-system-1.0.0.jar ubuntu@1.13.173.213:/home/ubuntu/banquet-api.jar`

### SQL操作
```bash
mysql -h127.0.0.1 -uroot -pBanquet123! banquet
```

## 通信方式

### 天龙↔地龙
- 天龙→地龙：`bash /home/ubuntu/dl_send.sh "消息内容"`
- 天龙→地龙端口：`http://100.70.171.0:18789/v1/chat/completions`

### COS公共对话
- 文件：`/mnt/cos/公共对话.md`
- 格式：emoji 名字：内容
- 长文本/代码 → 写COS文件，对话里通知路径

### 推送格式
```
发送：地龙🐉
抄送：秋哥👑
内容：#编号 消息.🦞
```

## 故障排查

### 网站打不开
```bash
curl -I http://youjianchuiyan.com        # Nginx
curl http://127.0.0.1:8080/api/dishes    # 后端JAR
ps aux | grep java                        # JAR进程
tail -50 /tmp/banquet.log                 # 日志
```

### 地龙不通
```bash
curl http://100.70.171.0:18789/health
# 如果不通 → 秋哥Windows上:
# Get-Process wscript,node | Stop-Process -Force
# Start-Sleep 3
# Start-Process wscript.exe $env:USERPROFILE\.openclaw\gateway.vbs
```

### 天龙重启
```bash
kill $(pgrep -f "openclaw gateway") && sleep 3
nohup openclaw gateway run --port 11500 > /tmp/openclaw-gateway.log 2>&1 &
curl -s http://127.0.0.1:11500/health
```

---
🦞 编写于 2026-07-22
