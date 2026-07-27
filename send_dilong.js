const { execSync } = require('child_process');
const fs = require('fs');

const msg = `[solo->地龙] 通知

地龙你好：

1. COS工作空间已从"双龙工作空间"更名为"ai公共工作空间"
2. 三人分工方案已上传到COS：
   ai公共工作空间/项目管理/餐饮管理系统/分工方案与实施计划.md

3. 你的短期任务（2周内）：
   - IpadMenu.vue 点菜页完善：分类导航、菜品卡片、购物车、下单确认
   - 员工卡号验证弹窗：输入工号+密码，提交订单
   - 后台预定录入弹窗：客户信息、桌台选择、时间段、定金
   - 人事模块页面：员工台账、考勤看板、请假表单、排班表
   - 全局请求拦截器：自动注入4组请求头
   - 打印组件：读取后端返回print_port，不硬编码

4. 铁律：
   - 前端字段名与后端返回完全一致，禁止映射转换
   - 区域直接用API返回的table_area值，不映射
   - 所有金额、折扣、总价后端计算，前端不本地计算

5. GitHub仓库：https://github.com/rinomezhang-arch/youjianchuiyan.com
   你的分支：dilong/ui

请收到通知后回复确认。`;

const params = JSON.stringify({key: 'agent:main:main', message: msg});
fs.writeFileSync('/tmp/dilong_params.json', params);

try {
    const out = execSync(`node "C:\\Users\\rinom\\AppData\\Roaming\\npm\\node_modules\\openclaw\\openclaw.mjs" gateway call sessions.send --params @C:\\Users\\rinom\\AppData\\Local\\Temp\\dilong_params.json`, {encoding: 'utf-8', timeout: 30000});
    console.log(out);
} catch(e) {
    console.error('Error:', e.message);
    // Try alternate approach with stdin
    try {
        const out2 = execSync(`node "C:\\Users\\rinom\\AppData\\Roaming\\npm\\node_modules\\openclaw\\openclaw.mjs" gateway call sessions.send`, {encoding: 'utf-8', timeout: 30000, input: params});
        console.log(out2);
    } catch(e2) {
        console.error('Error2:', e2.message);
    }
}
