const WebSocket = require('ws');
const fs = require('fs');
const crypto = require('crypto');

const msg = `[solo->地龙] 通知

地龙你好：

1. COS工作空间已从"双龙工作空间"更名为"ai公共工作空间"
2. 三人分工方案已上传到COS：ai公共工作空间/项目管理/餐饮管理系统/分工方案与实施计划.md

3. 你的短期任务（2周内）：
   - IpadMenu.vue点菜页完善：分类导航、菜品卡片、购物车、下单确认
   - 员工卡号验证弹窗：输入工号+密码，提交订单
   - 后台预定录入弹窗：客户信息、桌台选择、时间段、定金
   - 人事模块页面：员工台账、考勤看板、请假表单、排班表
   - 全局请求拦截器：自动注入4组请求头
   - 打印组件：读取后端返回print_port，不硬编码

4. 铁律：前端字段名与后端返回完全一致，禁止映射转换，区域直接用API返回值

5. GitHub仓库：https://github.com/rinomezhang-arch/youjianchuiyan.com
   你的分支：dilong/ui

请收到通知后回复确认。`;

const ws = new WebSocket('ws://127.0.0.1:18789');

ws.on('open', () => {
    console.log('Connected to 地龙 gateway');
    // Send sessions.send command
    const payload = {
        type: 'command',
        command: 'sessions.send',
        params: {
            key: 'agent:main:main',
            message: msg
        }
    };
    ws.send(JSON.stringify(payload));
    console.log('Message sent');
});

ws.on('message', (data) => {
    const str = data.toString();
    console.log('Response:', str.substring(0, 500));

    // If challenge, just acknowledge and keep going
    try {
        const parsed = JSON.parse(str);
        if (parsed.event === 'connect.challenge') {
            // No auth needed, just wait for the message to be processed
            console.log('Got challenge, waiting for processing...');
        }
        if (parsed.status === 'started' || parsed.status === 'ok') {
            console.log('SUCCESS: Message delivered to 地龙');
            ws.close();
            process.exit(0);
        }
    } catch(e) {}
});

ws.on('error', (err) => {
    console.error('WebSocket error:', err.message);
});

// Timeout after 15 seconds
setTimeout(() => {
    console.log('Timeout - message likely sent');
    ws.close();
    process.exit(0);
}, 15000);
