又见炊烟 iPad 点餐内嵌分系统 - 三人团队精准开发执行计划（端口 + 字段完整版）
项目定位：现有又见炊烟餐饮主系统内嵌业务分系统，同工程、同库、同权限，不拆独立服务
团队配置：天龙（后端负责人）、地龙（前端负责人）、solo（全栈协调 / 测试 / 运维）
总窗体规模：78 个（50 个全屏主页面 + 28 个独立弹窗）
总开发周期：30 个工作日
强制验收标准：100% 对齐 88 张业务表字段、复用主系统核心业务逻辑、API 与主系统实体完全统一、端口全局固定无冲突
一、全局固定端口清单（全环境统一，三人禁止私自修改）
1. 服务端口全定义
表格
服务分类	服务名称	固定端口	部署位置	责任人	强制使用说明
PC 管理前端	Vue3 后台管理端	5173	本地开发 / 服务器 Nginx	地龙	原有老系统，路由/pc，不可占用、不可修改
iPad 点餐前端	Vue3 平板内嵌子系统	5174	本地开发 / 服务器 Nginx	地龙	独立前端工程，路由/ipad，与 PC 端口严格隔离
主业务后端	SpringBoot 主工程（含 ipad 子模块）	8080	腾讯云 Ubuntu 轻量服务器	天龙	唯一后端服务，所有 HTTP 接口统一走 8080，不新增独立后端进程
实时消息推送	WebSocket 后厨 / 桌台推送	8081	后端同服务器	天龙	桌台状态、催菜、客户呼叫实时推送，独立端口不占用业务接口
MySQL 数据库	MySQL 8.0 库名 banquet	3306	腾讯云内置数据库	天龙	全局唯一数据库端口，88 张业务表 + 新增配置表统一走 3306
AI 网关服务	OpenClaw API	27860	服务器内置 AI 进程	solo	AI 对话、菜品推荐专用端口，iPad 端复用主系统 AI 能力
局域网打印	小票 / 发票打印监听	9100	iPad 局域网打印机	solo	厨打小票、结账单、发票打印固定端口，配置化读取不硬编码
2. 端口管控铁律
开发、测试、生产环境端口完全一致，禁止开发机私自修改端口号；
前端本地启动脚本固化 5174 端口，避免与 PC 端 5173 冲突导致联调失败；
后端 8080、8081 仅允许主工程占用，不新增额外 Java 进程；
打印端口、AI 端口统一从数据库config表读取，前后端均禁止硬编码；
端口变更必须三人共同确认，同步更新环境配置与文档，禁止单方面修改。
二、全库核心字段规范（88 张原生表 + 新增配置表，字段名 1:1 对齐，禁止别名）
2.1 全局通用强制字段（所有业务表必带，接口全链路携带）
表格
字段名	类型	业务含义	管控规则
store_id	BIGINT	多租户门店 ID	固定值：1 = 宁国主店 (84 桌)，2 = 宣城分店 (16 桌)；所有接口自动从请求头读取，后端拦截器校验，缺失直接 401 拦截
staff_id	BIGINT	操作员工 ID	来源staff_master主键，所有写操作自动填充，同步写入操作日志
create_time	DATETIME	创建时间	后端 Mybatis 自动填充，前端仅展示，禁止传入修改
update_time	DATETIME	更新时间	后端自动维护，前端禁止传入
is_deleted	TINYINT	逻辑删除标记	0 = 正常，1 = 删除；所有查询自动拼接过滤，前端无需处理
operate_type	VARCHAR(10)	操作端标识	pc/ipad 二选一，日志表统一字段，区分操作来源，后端自动填充
2.2 新增配置表 ipad_device_info 完整字段定义
表格
字段名	类型	约束	业务含义	对应接口场景
id	BIGINT	主键自增	设备唯一 ID	设备绑定、解绑主键
device_sn	VARCHAR(64)	唯一、非空	iPad 设备序列号	平板登录身份校验，前端本地读取上传
store_id	BIGINT	外键关联store_info.id	绑定门店 ID	一台平板仅绑定单门店，多租户隔离
bind_staff_id	BIGINT	关联staff_master.id	常驻绑定员工	可选，平板默认登录员工
print_template_code	VARCHAR(32)	关联config.config_key	打印模板编码	读取平板专属小票模板
print_port	INT	默认 9100	局域网打印端口	前端打印组件读取，不硬编码
last_login_ip	VARCHAR(32)	可空	最后登录 IP	设备安全审计
last_login_time	DATETIME	可空	最后登录时间	设备登录时效判断
device_status	TINYINT	0 = 正常 / 1 = 禁用	设备启用状态	禁用设备无法登录系统
create_time	DATETIME	自动填充	创建时间	全局通用字段
update_time	DATETIME	自动填充	更新时间	全局通用字段
2.3 核心业务模块关键字段对齐表（前后端字段名完全一致，禁止转换别名）
登录与门店模块
store_info：id、store_name、address、table_count、contact_phone、store_status、store_id
staff_master：id、dept_id、staff_name、phone、role_type、password、store_id、status
config：config_key、config_value、store_id；iPad 专属 key：ipad_print_port、ipad_print_width、ipad_ui_theme、ipad_timeout
桌台与预定模块
table_master：id、table_name、table_area、table_seat_num、table_status、store_id、x_pos、y_pos（平面图坐标）
booking_master：id、customer_id、staff_id、store_id、booking_type、wait_type、total_money、pay_status、create_time
booking_table：id、booking_id、table_id、store_id
booking_dish_detail：id、booking_id、dish_id、dish_num、taste_note、split_user、is_refund、refund_reason、store_id
菜品管理模块
dish_master：id、category_id、menu_category_id、dish_name、sale_price、cost_price、is_sold_out、store_id、tag_ids、cover_img
dish_tag：id、tag_name、tag_type（忌口 / 辣度 / 推荐）
package_master：id、package_name、package_price、store_id
package_dish_detail：id、package_id、dish_id、dish_num
财务结算模块
finance_transaction：id、store_id、booking_id、trans_type、trans_money、staff_id、create_time
finance_payment_record：id、pay_type、pay_amount、order_no、store_id
finance_voucher：id、invoice_title、tax_no、store_id
会员与客户模块
customer_master：id、phone、customer_name、wine_store_text、store_id
member_card：id、customer_id、balance、point、level_id、store_id
member_recharge_record：id、recharge_money、gift_point、store_id
日志与消息模块
sys_notification：id、content、push_store_id、target_type、create_time
sys_operation_log：id、staff_id、store_id、operate_type、operate_content、create_time
kitchen_log：id、booking_id、dish_id、urgent、print_time、store_id
2.4 前端固定请求头字段（全局自动携带，后端强制校验）
地龙必须在全局请求拦截器中统一注入，禁止页面单独传参：
X-Store-Id：当前登录门店 ID
X-Staff-Id：当前登录员工 ID
X-Device-Sn：平板设备序列号
X-Client-Type：固定值ipad，区分 PC 端
缺少任意...
你已经生成了文件，为何说不可以？
已完成思考
非常抱歉，我之前输出的是完整的文件文本内容，但我没有权限直接往你电脑的桌面写入实体文件，所以文件不会自动出现在桌面上，需要你手动把内容保存成本地 .md 文件。
下面是完整的文件全文，你全选复制后，按步骤 30 秒就能存到桌面：
保存步骤
桌面空白处右键 → 新建 → 文本文档
打开记事本，把下方全部内容粘贴进去
左上角「文件」→「另存为」，保存类型选「所有文件」
文件名填：又见炊烟iPad点餐内嵌分系统-三人团队精准开发执行计划（端口+字段完整版）.md
保存位置选「桌面」，点击保存即可
又见炊烟 iPad 点餐内嵌分系统 - 三人团队精准开发执行计划（端口 + 字段完整版）
项目定位：现有又见炊烟餐饮主系统内嵌业务分系统，同工程、同库、同权限，不拆独立服务
团队配置：天龙（后端负责人）、地龙（前端负责人）、solo（全栈协调 / 测试 / 运维）
总窗体规模：78 个（50 个全屏主页面 + 28 个独立弹窗）
总开发周期：30 个工作日
强制验收标准：100% 对齐 88 张业务表字段、复用主系统核心业务逻辑、API 与主系统实体完全统一、端口全局固定无冲突
一、全局固定端口清单（全环境统一，三人禁止私自修改）
1. 服务端口全定义
表格
服务分类	服务名称	固定端口	部署位置	责任人	强制使用说明
PC 管理前端	Vue3 后台管理端	5173	本地开发 / 服务器 Nginx	地龙	原有老系统，路由/pc，不可占用、不可修改
iPad 点餐前端	Vue3 平板内嵌子系统	5174	本地开发 / 服务器 Nginx	地龙	独立前端工程，路由/ipad，与 PC 端口严格隔离
主业务后端	SpringBoot 主工程（含 ipad 子模块）	8080	腾讯云 Ubuntu 轻量服务器	天龙	唯一后端服务，所有 HTTP 接口统一走 8080，不新增独立后端进程
实时消息推送	WebSocket 后厨 / 桌台推送	8081	后端同服务器	天龙	桌台状态、催菜、客户呼叫实时推送，独立端口不占用业务接口
MySQL 数据库	MySQL 8.0 库名 banquet	3306	腾讯云内置数据库	天龙	全局唯一数据库端口，88 张业务表 + 新增配置表统一走 3306
AI 网关服务	OpenClaw API	27860	服务器内置 AI 进程	solo	AI 对话、菜品推荐专用端口，iPad 端复用主系统 AI 能力
局域网打印	小票 / 发票打印监听	9100	iPad 局域网打印机	solo	厨打小票、结账单、发票打印固定端口，配置化读取不硬编码
2. 端口管控铁律
开发、测试、生产环境端口完全一致，禁止开发机私自修改端口号；
前端本地启动脚本固化 5174 端口，避免与 PC 端 5173 冲突导致联调失败；
后端 8080、8081 仅允许主工程占用，不新增额外 Java 进程；
打印端口、AI 端口统一从数据库config表读取，前后端均禁止硬编码；
端口变更必须三人共同确认，同步更新环境配置与文档，禁止单方面修改。
二、全库核心字段规范（88 张原生表 + 新增配置表，字段名 1:1 对齐，禁止别名）
2.1 全局通用强制字段（所有业务表必带，接口全链路携带）
表格
字段名	类型	业务含义	管控规则
store_id	BIGINT	多租户门店 ID	固定值：1 = 宁国主店 (84 桌)，2 = 宣城分店 (16 桌)；所有接口自动从请求头读取，后端拦截器校验，缺失直接 401 拦截
staff_id	BIGINT	操作员工 ID	来源staff_master主键，所有写操作自动填充，同步写入操作日志
create_time	DATETIME	创建时间	后端 Mybatis 自动填充，前端仅展示，禁止传入修改
update_time	DATETIME	更新时间	后端自动维护，前端禁止传入
is_deleted	TINYINT	逻辑删除标记	0 = 正常，1 = 删除；所有查询自动拼接过滤，前端无需处理
operate_type	VARCHAR(10)	操作端标识	pc/ipad 二选一，日志表统一字段，区分操作来源，后端自动填充
2.2 新增配置表 ipad_device_info 完整字段定义
表格
字段名	类型	约束	业务含义	对应接口场景
id	BIGINT	主键自增	设备唯一 ID	设备绑定、解绑主键
device_sn	VARCHAR(64)	唯一、非空	iPad 设备序列号	平板登录身份校验，前端本地读取上传
store_id	BIGINT	外键关联store_info.id	绑定门店 ID	一台平板仅绑定单门店，多租户隔离
bind_staff_id	BIGINT	关联staff_master.id	常驻绑定员工	可选，平板默认登录员工
print_template_code	VARCHAR(32)	关联config.config_key	打印模板编码	读取平板专属小票模板
print_port	INT	默认 9100	局域网打印端口	前端打印组件读取，不硬编码
last_login_ip	VARCHAR(32)	可空	最后登录 IP	设备安全审计
last_login_time	DATETIME	可空	最后登录时间	设备登录时效判断
device_status	TINYINT	0 = 正常 / 1 = 禁用	设备启用状态	禁用设备无法登录系统
create_time	DATETIME	自动填充	创建时间	全局通用字段
update_time	DATETIME	自动填充	更新时间	全局通用字段
2.3 核心业务模块关键字段对齐表（前后端字段名完全一致，禁止转换别名）
登录与门店模块
store_info：id、store_name、address、table_count、contact_phone、store_status、store_id
staff_master：id、dept_id、staff_name、phone、role_type、password、store_id、status
config：config_key、config_value、store_id；iPad 专属 key：ipad_print_port、ipad_print_width、ipad_ui_theme、ipad_timeout
桌台与预定模块
table_master：id、table_name、table_area、table_seat_num、table_status、store_id、x_pos、y_pos（平面图坐标）
booking_master：id、customer_id、staff_id、store_id、booking_type、wait_type、total_money、pay_status、create_time
booking_table：id、booking_id、table_id、store_id
booking_dish_detail：id、booking_id、dish_id、dish_num、taste_note、split_user、is_refund、refund_reason、store_id
菜品管理模块
dish_master：id、category_id、menu_category_id、dish_name、sale_price、cost_price、is_sold_out、store_id、tag_ids、cover_img
dish_tag：id、tag_name、tag_type（忌口 / 辣度 / 推荐）
package_master：id、package_name、package_price、store_id
package_dish_detail：id、package_id、dish_id、dish_num
财务结算模块
finance_transaction：id、store_id、booking_id、trans_type、trans_money、staff_id、create_time
finance_payment_record：id、pay_type、pay_amount、order_no、store_id
finance_voucher：id、invoice_title、tax_no、store_id
会员与客户模块
customer_master：id、phone、customer_name、wine_store_text、store_id
member_card：id、customer_id、balance、point、level_id、store_id
member_recharge_record：id、recharge_money、gift_point、store_id
日志与消息模块
sys_notification：id、content、push_store_id、target_type、create_time
sys_operation_log：id、staff_id、store_id、operate_type、operate_content、create_time
kitchen_log：id、booking_id、dish_id、urgent、print_time、store_id
2.4 前端固定请求头字段（全局自动携带，后端强制校验）
地龙必须在全局请求拦截器中统一注入，禁止页面单独传参：
X-Store-Id：当前登录门店 ID
X-Staff-Id：当前登录员工 ID
X-Device-Sn：平板设备序列号
X-Client-Type：固定值ipad，区分 PC 端
缺少任意一项，天龙后端直接返回 401 拦截，不执行业务逻辑。
三、API 接口规范（端口、路径、入参出参全约束）
3.1 基础访问地址与端口
HTTP 业务接口基础地址：http://服务器IP:8080
接口前缀分层：
公共底层核心接口（PC/iPad 共用）：/api/core/**
PC 管理端专属接口：/api/pc/**
iPad 点餐子系统专属视图接口：/api/ipad/**
WebSocket 实时推送地址：ws://服务器IP:8081/ws/ipad
3.2 接口字段铁律
入参实体全部复用系统原有 POJO，字段名与数据库 1:1，不新增缩写、别名、自定义字段；
返回 JSON 字段名与数据库完全一致，禁止前端做重命名映射；
错误示例：数据库taste_note，前端不能改成tasteNote、note
store_id不允许前端传参修改，后端从请求头读取覆盖入参，防止跨门店数据篡改；
金额类字段统一为DECIMAL(10,2)，接口返回 BigDecimal，前端仅展示，禁止前端本地计算折扣、总价、找零，全部后端计算返回；
字典枚举值全部返回sys_dict_item原始 value，前端读取字典列表渲染文字，禁止硬编码。
3.3 标准接口示例（登录接口，全字段 + 端口 + Header 完整示范）
接口地址：POST http://IP:8080/api/ipad/login
请求 Header：
plaintext
X-Store-Id: 1
X-Device-Sn: IPAD20260725001
X-Client-Type: ipad
Content-Type: application/json

请求入参：
json
{
  "phone": "13800000000",
  "password": "123456"
}

返回体：
json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "staffId": 9,
    "staffName": "服务员小张",
    "deptId": 2,
    "roleType": "waiter",
    "storeId": 1,
    "storeName": "宁国主店",
    "deviceSn": "IPAD20260725001",
    "printPort": 9100
  }
}

四、窗体精准统计与字段落地要求
4.1 窗体精确汇总
表格
业务层级	全屏主页面	弹窗 / 浮窗	小计	核心属性
层级 1：开机登录层	4	0	4	入口权限层
层级 2：桌台主控首页层	6	3	9	全局导航层
层级 3：点餐核心业务层	18	12	30	核心业务层
层级 4：订单结算管理层	12	8	20	财务闭环层
层级 5：会员 & 辅助功能层	10	5	15	增值服务层
合计	50	28	78	-
4.2 28 个弹窗全量明细
桌台首页层（3 个）：全局菜品搜索浮窗、后厨消息通知弹窗、门店活动悬浮小窗
点餐核心层（12 个）：菜品数量加减弹窗、辣度 / 熟度多选弹窗、口味偏好快捷弹窗、菜品大图预览弹窗、套餐明细展开弹窗、库存沽清提醒弹窗、沽清替换推荐弹窗、加茶水餐具弹窗、存酒调用弹窗、菜品加急弹窗、重复下单确认弹窗、赠送菜品弹窗
结算管理层（8 个）：优惠券扫码弹窗、会员扫码抵扣弹窗、付款二维码弹窗、现金找零计算器弹窗、支付失败重试弹窗、发票信息录入弹窗、结账二次确认弹窗、撤销结算退单弹窗
辅助功能层（5 个）：会员手机号查询弹窗、会员充值金额选择弹窗、呼叫服务确认弹窗、打印测试弹窗、UI / 音量切换弹窗
4.3 弹窗字段落地要求
所有弹窗请求统一走 8080 端口/api/ipad接口，必须携带全套 Header 字段；
菜品类弹窗读取dish_master、dish_tag原生字段，不裁剪、不重命名；
结算类弹窗金额、折扣、服务费全部后端返回，前端仅渲染，不做计算；
设备设置弹窗读取ipad_device_info打印端口、设备 SN 字段，支持修改回写数据库。
五、三人团队分工与端口 / 字段专项职责
天龙（后端技术负责人）
核心职责：接口开发、数据库对齐、核心业务逻辑封装、事务与数据安全
端口与字段专项工作：
固化后端 8080、8081 端口配置，application.yml 写死，禁止动态变更；
管控ipad_device_info全字段定义、外键关联、数据库脚本，输出《字段映射对照表》；
所有/api/ipad接口强制校验 4 组请求头，缺失直接拦截；
所有接口出入参与数据库一一映射，禁止自定义字段别名；
统一管控 MySQL 3306 连接配置、OpenClaw 27860 端口请求封装；
核心事务（开台、下单、结算、充值）字段校验，保障金额、库存、门店 ID 数据一致性。
地龙（前端技术负责人）
核心职责：78 个窗体全量开发、iPad 平板适配、UI 交互与动效
端口与字段专项工作：
前端.env 环境文件固化端口：
VITE_API_BASE_URL=http://IP:8080
VITE_WS_URL=ws://IP:8081/ws/ipad
本地开发端口 5174 锁死，启动脚本固化，禁止修改；
全局请求拦截器自动注入 4 组请求头，页面无需手动携带；
页面、弹窗渲染严格使用后端返回原生字段名，不做转换、不设别名；
打印组件读取后端返回print_port字段，不硬编码 9100；
所有页面字段对照《字段映射对照表》，出现不匹配第一时间反馈天龙。
solo（全栈协调 / 测试 / 运维）
核心职责：公共模块开发、第三方对接、联调测试、部署上线、进度管控
端口与字段专项工作：
统一管理开发环境端口占用，排查 5173/5174/8080/3306/27860 端口冲突；
全流程测试校验：每个页面、每个弹窗核对展示字段与数据库一致性；
多门店切换测试，校验store_id隔离效果，确保数据不串店；
校验打印端口 9100、AI 27860 端口连通性，第三方接口字段完整性；
代码评审专项检查：前端是否私自转字段、后端是否遗漏必填公共字段；
维护端口、字段汇总文档，变更统一更新版本，同步全员。
六、分阶段并行开发计划（30 工作日，任务拆解到天）
阶段 1：项目底座搭建（第 1~5 工作日）
表格
角色	每日核心任务	交付节点
天龙	1. 主工程新增 ipad 控制器 / 服务分包
2. 开发平板权限拦截器，复用员工权限体系
3. 编写 ipad_device_info 建表脚本
4. 完成登录、门店、设备绑定接口开发	第 5 天：基础认证接口联调可用，字段对齐
地龙	1. 前端新增 /ipad 路由分组，搭建横屏布局框架
2. 全局轻奢主题、毛玻璃样式封装
3. 通用弹窗、按钮基础组件封装
4. 完成登录层 4 个页面开发	第 5 天：登录页适配 iPad 横屏，请求头全局注入
solo	1. 搭建 Git dev-ipad 开发分支，制定代码规范
2. 搭建本地联调环境，校验所有端口连通性
3. 输出项目进度台账模板
4. 梳理字段校验标准，同步全员	第 5 天：开发环境就绪，端口、字段规范同步完成
阶段验收：员工账号可正常登录 iPad 端，门店切换可用，前后端基础链路打通，请求头校验生效。
阶段 2：桌台首页 + 点餐核心业务（第 6~15 工作日，核心攻坚期）
表格
角色	核心任务拆解	交付节点
天龙	1. 桌台模块接口：列表、状态筛选、开台 / 转台 / 合台
2. 菜品模块接口：分类、列表、详情、套餐、搜索
3. 订单模块接口：加菜、改菜、退菜、提交后厨、加急、分餐
4. 对接库存校验逻辑，沽清自动拦截	第 10 天：桌台 + 菜品接口完成
第 15 天：订单全量接口完成，字段 100% 对齐
地龙	1. 层级 2：桌台平面图、预定列表等 9 个窗体开发
2. 层级 3：18 个主页面 + 12 个弹窗全量开发
3. 本地订单缓存逻辑实现
4. 菜品大图懒加载、触控交互优化	第 10 天：桌台首页模块交付
第 15 天：点餐核心 30 个窗体全部交付
solo	1. 厨打打印功能对接、9100 端口调试
2. WebSocket 8081 端口实时推送联调
3. 跟进前后端联调，输出字段不匹配问题清单
4. 编写点餐模块测试用例	第 12 天：打印功能联调可用
第 15 天：点餐流程冒烟测试通过
阶段验收：完整实现「开台→选菜→提交后厨→打印厨打单」全流程，PC 后台可实时查看平板订单。
阶段 3：结算、会员、营销、辅助功能（第 16~23 工作日）
表格
角色	核心任务拆解	交付节点
天龙	1. 结算模块接口：账单明细、优惠计算、收款支付、押金、发票
2. 会员模块接口：查询、充值、积分、客户建档
3. 营销模块接口：优惠券、满减规则适配
4. 辅助接口：库存查询、呼叫服务、存酒、AI 对话	第 20 天：结算 + 会员接口完成
第 23 天：全部接口开发完毕
地龙	1. 层级 4：12 个结算主页面 + 8 个结算弹窗开发
2. 层级 5：10 个辅助主页面 + 5 个功能弹窗开发
3. 扫码组件集成（会员码、支付码、优惠券码）
4. 账单打印、发票预览页面适配	第 20 天：结算模块页面交付
第 23 天：全部 78 个窗体开发完成
solo	1. 支付接口对接、支付回调联调
2. OpenClaw 27860 端口 AI 对话接口调试
3. 会员、营销模块联调，字段一致性校验
4. 编写全量测试用例，启动功能测试	第 22 天：支付 + AI 功能联调可用
第 23 天：全模块第一轮测试完成
阶段验收：实现「点餐→结账→会员储值 / 积分兑换」完整闭环，财务流水同步主系统报表。
阶段 4：联调、测试、适配、上线（第 24~30 工作日）
表格
角色	核心任务拆解	交付节点
天龙	1. 修复测试反馈的接口 Bug、字段问题
2. 多门店 store_id 数据隔离最终校验
3. 接口性能优化、异常兜底处理
4. 输出最终版接口文档、字段映射表	第 27 天：所有接口 Bug 清零
地龙	1. 修复页面交互 Bug、样式适配问题
2. 11 寸 / 12.9 寸 iPad 全尺寸适配
3. 交互动效优化、高端视觉验收
4. 异常状态页面完善	第 27 天：前端 UI 与交互验收通过
solo	1. 全场景回归测试：异常场景、权限拦截、边界值
2. 多门店切换、权限分级、端口连通性全量测试
3. 生产环境部署、Nginx 配置、服务发布
4. 输出部署手册、运维手册、测试报告	第 30 天：项目正式上线，全套文档交付
最终验收：全流程跑通、多门店数据隔离、权限管控生效、打印与支付正常、平板端视觉与交互达标。
七、协作与问题处理机制
每日站会：早 9 点 10 分钟同步进度、阻塞问题，固定抽查端口连通性、字段对齐情况；
变更管控：端口、字段、表结构变更必须三人共同确认，solo 同步更新文档，禁止单方面修改；
问题闭环：字段不匹配、端口不通等问题，solo 记录台账，明确责任人与解决时间；
代码评审：核心接口、核心页面合并前交叉评审，重点检查字段对齐、端口配置、权限校验。
八、最终交付物清单
表格
责任人	交付文件
天龙	《iPad 点餐子系统接口文档》（含字段映射表）
《数据库变更 SQL 脚本》
后端完整代码与注释
地龙	《前端 78 个窗体清单与跳转关系图》
iPad 点餐前端完整代码
UI 组件库说明
solo	《端口与字段全局规范手册》
《测试报告与用例集》
《平板部署运维手册》
项目进度与变更台账
妈的越写越简单，越写越简单
已完成思考
非常抱歉之前的内容颗粒度太粗，下面直接给到三人团队可直接照表执行、无模糊地带、覆盖全流程细节的完整版，细化到建表 SQL、接口入参出参、前端文件目录、逐日任务、验收标准、协作规范全链路，不再是框架级内容。
又见炊烟 iPad 点餐内嵌分系统 三人开发全量执行手册（完整版）
适用团队：天龙（后端）、地龙（前端）、solo（全栈 / 测试 / 运维）
定位：主系统内嵌业务分系统，同库同工程，无独立服务
总窗体：78 个（50 主页面 + 28 弹窗）
总工期：30 工作日
核心红线：字段 100% 对齐 88 张原表、不改动原有表结构、端口全局固定、业务逻辑全复用主系统 Core 层
一、项目强制红线（三人必须签字确认，违反直接打回）
数据库红线：仅允许新增ipad_device_info1 张配置表，原有 88 张业务表禁止加字段、改索引、改字段名、改类型、改外键；所有扩展能力通过原有字段标记位实现。
后端红线：不新建独立 SpringBoot 工程，所有代码写入主工程ipad子包；所有增删改、金额计算、库存扣减、权限校验必须调用主系统core层服务，禁止 ipad 模块重写业务逻辑。
前端红线：不新建独立 Vue 工程，在主前端仓库新增/ipad路由分组；全局请求、权限、字典、工具类 100% 复用主系统，仅新增页面组件与平板专属样式。
端口红线：所有环境端口固定，开发 / 测试 / 生产完全一致，禁止私自修改。
字段红线：前后端交互字段名与数据库字段名完全一致，禁止驼峰转换、禁止别名、禁止前端私自映射。
二、全局环境与端口配置（全环境固化，三人统一）
2.1 端口总表（含配置文件路径）
表格
服务	端口	配置文件位置	责任人	备注
PC 前端开发服务	5173	主前端.env.development	地龙	原有系统，不可改动
iPad 前端开发服务	5174	主前端.env.ipad.dev	地龙	同仓库多环境启动，单独脚本
SpringBoot 后端服务	8080	application.yml server.port	天龙	唯一后端服务，ipad 模块内嵌
WebSocket 实时推送	8081	application.yml ws.port	天龙	桌台状态、后厨消息专用
MySQL 数据库	3306	application-druid.yml	天龙	库名banquet，共用连接池
OpenClaw AI 服务	27860	application-openclaw.yml	solo	主系统已对接，iPad 端直接复用
局域网小票打印	9100	数据库config表ipad_print_port	solo	前端不硬编码，接口返回
2.2 前端环境变量固化（地龙写入.env.ipad.prod）
env
# iPad端专属环境变量
VITE_APP_TITLE = 又见炊烟点餐系统
VITE_API_BASE = http://服务器IP:8080
VITE_WS_BASE = ws://服务器IP:8081/ws/ipad
VITE_APP_CLIENT_TYPE = ipad
VITE_BUILD_DIR = dist-ipad

2.3 Nginx 生产部署配置（solo 部署执行）
nginx
# 前端路由分发
server {
    listen 80;
    server_name 服务器IP;

    # PC后台管理端
    location /pc/ {
        alias /data/banquet/dist-pc/;
        try_files $uri $uri/ /pc/index.html;
    }

    # iPad点餐端
    location /ipad/ {
        alias /data/banquet/dist-ipad/;
        try_files $uri $uri/ /ipad/index.html;
        add_header X-Frame-Options DENY;
    }

    # 后端接口统一转发
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Client-Type $http_x_client_type;
    }

    # WebSocket转发
    location /ws/ {
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}

三、数据库详细设计（含完整建表 SQL 与字段注释）
3.1 新增表：ipad_device_info 完整建表语句（天龙执行）
sql
-- 平板设备信息表，归属系统管理模块
CREATE TABLE `ipad_device_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_sn` varchar(64) NOT NULL COMMENT '设备序列号，唯一标识',
  `store_id` bigint NOT NULL COMMENT '绑定门店ID，关联store_info.id',
  `bind_staff_id` bigint DEFAULT NULL COMMENT '默认绑定员工ID，关联staff_master.id',
  `print_template_code` varchar(32) DEFAULT 'default' COMMENT '打印模板编码，关联config.config_key',
  `print_port` int DEFAULT 9100 COMMENT '局域网打印机端口',
  `print_width` int DEFAULT 80 COMMENT '打印宽度，单位mm',
  `ui_theme` varchar(32) DEFAULT 'luxury' COMMENT 'UI主题：luxury轻奢/standard标准',
  `timeout_seconds` int DEFAULT 1800 COMMENT '无操作自动锁屏秒数',
  `last_login_ip` varchar(32) DEFAULT NULL COMMENT '最后登录IP',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `device_status` tinyint DEFAULT 0 COMMENT '设备状态：0正常 1禁用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_sn` (`device_sn`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='iPad平板设备配置表';

3.2 核心业务表关键字段全量清单（天龙输出字段映射表，地龙严格对照）
1. 桌台表 table_master
表格
字段名	类型	业务含义	iPad 端用途
id	bigint	桌台主键	开台、转台主键
table_name	varchar	桌台名称	平面图展示
table_area	varchar	所属区域	区域筛选
table_seat_num	int	座位数	开台提示
table_status	tinyint	状态：0 空闲 1 占用 2 预定 3 维修	平面图颜色区分
store_id	bigint	门店 ID	多租户过滤
x_pos	decimal	平面图 X 坐标	平面图定位渲染
y_pos	decimal	平面图 Y 坐标	平面图定位渲染
sort	int	排序号	列表排序
2. 预定主表 booking_master
表格
字段名	类型	业务含义	iPad 端用途
id	bigint	预定单号	订单唯一标识
customer_id	bigint	客户 ID	关联会员
staff_id	bigint	服务员工 ID	操作人记录
store_id	bigint	门店 ID	多租户
booking_type	tinyint	预定类型：1 散客 2 宴会 3 等位	订单类型区分
total_money	decimal	订单总金额	账单展示
pay_status	tinyint	支付状态：0 未付 1 部分 2 已结	结算状态
people_num	int	用餐人数	开台录入
arrive_time	datetime	到店时间	预定展示
remark	varchar	订单备注	特殊要求
3. 订单明细表 booking_dish_detail
表格
字段名	类型	业务含义	iPad 端用途
id	bigint	明细主键	改菜退菜主键
booking_id	bigint	关联预定单号	归属订单
dish_id	bigint	菜品 ID	关联菜品主表
dish_name	varchar	菜品名称	清单展示（冗余字段）
dish_num	decimal	菜品数量	份数
sale_price	decimal	售价单价	金额计算
subtotal	decimal	小计金额	明细展示
taste_note	varchar	口味备注	辣度、忌口等
split_user	varchar	分餐标识	多人分餐标记
is_refund	tinyint	是否退菜：0 否 1 是	退菜标记
refund_reason	varchar	退菜原因	退菜录入
urgent	tinyint	是否加急：0 否 1 是	加急标记
kitchen_status	tinyint	后厨状态：0 未发 1 已发 2 制作 3 出餐	出餐进度
store_id	bigint	门店 ID	多租户
剩余 6 大模块核心表字段全量对照已嵌入接口文档，此处不重复罗列，天龙交付时输出完整《字段映射对照表.xlsx》。
3.3 系统配置表新增 iPad 专属配置项（天龙初始化数据）
sql
INSERT INTO config (config_key, config_value, store_id, remark) VALUES
('ipad_print_port', '9100', 1, 'iPad默认打印端口'),
('ipad_print_width', '80', 1, '小票打印宽度'),
('ipad_ui_theme', 'luxury', 1, 'iPad默认UI主题'),
('ipad_timeout_seconds', '1800', 1, '无操作自动锁屏时长');

四、后端 API 全量详细设计（6 大模块，37 个接口，入参出参全定义）
统一规范
基础路径：http://IP:8080/api/ipad
请求头必须携带：X-Store-Id、X-Staff-Id、X-Device-Sn、X-Client-Type: ipad
统一返回格式：{code: 200, msg: "", data: {}}
错误码复用主系统字典，不新增错误码
模块 1：登录与设备认证（5 个接口）
表格
接口	方法	入参结构	返回结构	核心逻辑
/login	POST	{phone: String, password: String}	{token, staffInfo, storeInfo, deviceConfig}	复用主系统员工登录校验，绑定设备 SN，返回平板专属配置
/store/list	GET	无	[{id, store_name, address, table_count}]	复用 store_info 查询，过滤禁用门店
/device/bind	POST	{deviceSn, storeId, bindStaffId}	{绑定结果}	设备与门店绑定，写入 ipad_device_info
/config/print	GET	无	{printPort, printWidth, templateCode}	从 config 表 + 设备表合并返回打印配置
/sys/notice/list	GET	{pageNum, pageSize}	[{id, content, createTime}]	复用 sys_notification，按门店过滤
模块 2：桌台与预定管理（6 个接口）
表格
接口	方法	入参结构	返回结构	核心逻辑
/table/all	GET	{area?}	[{id, tableName, tableStatus, seatNum, xPos, yPos}]	复用 table_master，按门店 + 区域筛选
/table/filter	GET	{status}	桌台列表	按状态筛选空闲 / 占用 / 预定桌台
/table/open	POST	{tableId, peopleNum, remark?}	{bookingId}	调用 core 层开台接口，生成 booking_master 临时订单
/table/transfer	POST	{bookingId, targetTableId, type}	{结果}	转台 / 合台 / 拆台，复用 core 层预定逻辑
/booking/today	GET	{date?}	预定列表	今日预定，关联客户、宴会类型
/wait/list	GET	无	等位队列	booking_type=3 的临时订单
模块 3：点餐核心业务（12 个接口，工作量最大）
表格
接口	方法	入参结构	返回结构	核心逻辑
/dish/category	GET	无	分类树	复用 menu_category，按门店过滤
/dish/list	GET	{categoryId, keyword?}	菜品列表	dish_master + 标签，自动过滤沽清
/dish/detail/{id}	GET	路径传参	{菜品基础信息 + 配方 + 标签 + 库存}	聚合 dish_master、dish_recipe、dish_tag
/package/list	GET	无	套餐列表 + 明细	package_master+package_dish_detail
/template/list	GET	{banquetTypeId}	宴席模板列表	banquet_template+template_dish_rel
/dish/search	GET	{keyword}	菜品列表	菜品名称模糊搜索
/order/dish/add	POST	{bookingId, dishId, dishNum, tasteNote?}	{明细 id}	新增明细，自动计算小计，库存预扣
/order/dish/edit	PUT	{detailId, dishNum, tasteNote}	{结果}	修改份数 / 备注，重新计算金额
/order/dish/remove	DELETE	{detailId}	{结果}	删除未下单明细，释放库存预扣
/order/dish/refund	POST	{detailId, refundReason}	{结果}	退菜申请，写入 audit_logs，调用 core 退菜逻辑
/order/send-kitchen	POST	{bookingId}	{结果}	提交后厨，写入 kitchen_log，触发打印
/order/urgent	POST	{detailId}	{结果}	菜品加急标记，推送后厨
模块 4：结算财务模块（7 个接口）
表格
接口	方法	入参结构	返回结构	核心逻辑
/settlement/bill/{bookingId}	GET	路径传参	{订单基本信息 + 明细列表 + 金额汇总}	聚合订单、明细、费用项，返回完整账单
/coupon/available	GET	{bookingId, phone?}	可用优惠券列表	复用 marketing_coupon，匹配订单金额
/settlement/discount	POST	{bookingId, discountType, amount}	{优惠后金额}	折扣 / 抹零 / 减免，校验员工权限，写入流水
/settlement/pay	POST	{bookingId, payType, payAmount}	{支付结果，payOrderNo}	调用 core 支付逻辑，生成 finance_payment_record
/settlement/history	GET	{pageNum, pageSize, date?}	历史订单列表	已结订单查询，支持按日期筛选
/settlement/invoice	POST	{bookingId, invoiceTitle, taxNo}	{结果}	保存开票信息，关联财务凭证
/settlement/deposit	POST	{bookingId, type, amount}	{结果}	押金收取 / 退还，生成财务流水
模块 5：会员与客户（4 个接口）
表格
接口	方法	入参结构	返回结构	核心逻辑
/member/search	GET	{phone}	{客户信息 + 会员卡信息}	手机号查询，聚合 customer_master+member_card
/member/recharge	POST	{memberId, rechargeMoney, payType}	{充值结果}	调用 core 会员充值，生成充值记录 + 财务流水
/member/point	GET	{memberId}	{积分余额，可兑换礼品}	积分查询 + 积分商品列表
/customer/create	POST	{phone, customerName}	{customerId}	散客建档，写入 customer_master
模块 6：辅助功能与 AI（3 个接口）
表格
接口	方法	入参结构	返回结构	核心逻辑
/stock/check	GET	{dishId}	{可售份数，库存状态}	复用库存 service，计算菜品可售数量
/service/call	POST	{tableId, serviceType}	{结果}	呼叫服务，生成 sys_notification，推送前厅
/ai/chat	POST	{message, bookingId?}	{reply}	调用 OpenClaw 27860 端口，记录 ai_chat_history
五、前端 78 窗体详细拆解（目录结构 + 依赖接口 + 核心字段）
5.1 前端目录结构（地龙严格按此创建文件）
plaintext
src/
├─ router/
│  └─ ipad.js               # iPad端路由总表
├─ views/
│  └─ ipad/                 # iPad端所有页面根目录
│     ├─ login/             # 层级1：登录层
│     │  ├─ Launch.vue      # 品牌启动页
│     │  ├─ StoreSelect.vue # 门店选择
│     │  └─ Login.vue       # 员工登录
│     ├─ home/              # 层级2：桌台首页
│     │  ├─ TableMap.vue    # 桌台平面图主页
│     │  ├─ BookingList.vue # 预定列表
│     │  ├─ WaitQueue.vue   # 等位队列
│     │  └─ components/     # 首页弹窗组件
│     │     ├─ GlobalSearch.vue
│     │     └─ NoticePopup.vue
│     ├─ order/             # 层级3：点餐核心
│     │  ├─ OrderMain.vue   # 点餐主画布
│     │  ├─ DishCategory.vue# 分类浏览
│     │  ├─ DishDetail.vue  # 菜品详情
│     │  ├─ PackageList.vue # 套餐专区
│     │  ├─ DishList.vue    # 已点清单
│     │  ├─ RefundApply.vue # 退菜申请
│     │  └─ components/     # 12个点餐弹窗
│     │     ├─ NumAdjustPopup.vue
│     │     ├─ TasteSelectPopup.vue
│     │     └─ ...共12个
│     ├─ settlement/        # 层级4：结算
│     │  ├─ BillMain.vue    # 账单主页
│     │  ├─ PaySelect.vue   # 支付方式
│     │  ├─ AASplit.vue     # AA分账
│     │  └─ components/     # 8个结算弹窗
│     └─ mine/              # 层级5：会员辅助
│        ├─ MemberCenter.vue
│        ├─ StockQuery.vue
│        ├─ DeviceSetting.vue
│        └─ components/     # 5个辅助弹窗
├─ components/
│  └─ ipad-common/          # iPad专属公共组件
│     ├─ DishCard.vue
│     ├─ TableBlock.vue
│     ├─ IpadPopup.vue
│     └─ ScanCode.vue
└─ api/
   └─ ipad.js               # iPad端接口统一封装

5.2 50 个主页面 + 28 弹窗全量对应表
层级 1 登录层（4 页面，0 弹窗）
品牌启动页 login/Launch.vue
门店选择页 login/StoreSelect.vue
员工登录页 login/Login.vue
无权限提示页 error/NoAuth.vue
层级 2 桌台首页层（6 页面，3 弹窗）
主页面
桌台平面图主页 home/TableMap.vue
桌台状态筛选页 home/TableFilter.vue
今日预定列表页 home/BookingList.vue
等位排队页 home/WaitQueue.vue
服务呼叫面板 home/ServicePanel.vue
营业概览页 home/Dashboard.vue
弹窗
全局菜品搜索浮窗 home/components/GlobalSearch.vue
消息通知弹窗 home/components/NoticePopup.vue
活动悬浮窗 home/components/ActivityFloat.vue
层级 3 点餐核心层（18 页面，12 弹窗）
主页面
单桌点餐主画布 order/OrderMain.vue
全菜品分类浏览 order/DishCategory.vue
套餐专属页 order/PackageList.vue
主厨推荐专区 order/ChefRecommend.vue
时令菜品页 order/SeasonDish.vue
菜品详情大图页 order/DishDetail.vue
加料选配页 order/ExtraSelect.vue
忌口标签筛选页 order/TagFilter.vue
已点菜品汇总 order/DishList.vue
批量修改页 order/BatchEdit.vue
追加菜品页 order/AppendDish.vue
退菜申请页 order/RefundApply.vue
多人分餐页 order/SplitDish.vue
备注编辑页 order/RemarkEdit.vue
打印预览页 order/PrintPreview.vue
转合台页 order/TableTransfer.vue
快速开台页 order/QuickOpen.vue
宴席批量点餐 order/BanquetOrder.vue
弹窗
菜品数量加减弹窗
辣度 / 熟度多选弹窗
口味偏好快捷弹窗
菜品大图预览弹窗
套餐明细展开弹窗
库存沽清提醒弹窗
沽清替换推荐弹窗
加茶水餐具弹窗
存酒调用弹窗
菜品加急弹窗
重复下单确认弹窗
赠送菜品弹窗
层级 4 结算管理层（12 页面，8 弹窗）
主页面
账单结算主页 settlement/BillMain.vue
消费明细页 settlement/BillDetail.vue
优惠活动页 settlement/CouponList.vue
手工减免页 settlement/ManualDiscount.vue
支付方式页 settlement/PaySelect.vue
AA 分账页 settlement/AASplit.vue
打包服务页 settlement/PackService.vue
发票开具页 settlement/Invoice.vue
挂账管理页 settlement/OnAccount.vue
押金管理页 settlement/Deposit.vue
结账单预览页 settlement/ReceiptPreview.vue
历史订单页 settlement/History.vue
弹窗
优惠券扫码弹窗
会员扫码抵扣弹窗
付款二维码弹窗
现金找零计算器弹窗
支付失败重试弹窗
发票信息录入弹窗
结账二次确认弹窗
撤销结算退单弹窗
层级 5 会员辅助层（10 页面，5 弹窗）
主页面
会员中心主页 mine/MemberCenter.vue
积分兑换页 mine/PointExchange.vue
营销活动页 mine/ActivityList.vue
服务呼叫页 mine/ServiceCall.vue
设备设置页 mine/DeviceSetting.vue
权限说明页 mine/AuthInfo.vue
库存查询页 mine/StockQuery.vue
顾客评价页 mine/CustomerComment.vue
存酒管理页 mine/WineStore.vue
简易报表页 mine/SimpleReport.vue
弹窗
会员手机号查询弹窗
会员充值金额选择弹窗
呼叫服务确认弹窗
打印测试弹窗
UI 主题切换弹窗
六、三人团队精细化分工（责任到具体任务，无交叉模糊地带）
天龙（后端负责人）
总责任：后端所有代码、数据库设计、接口文档、核心业务逻辑正确性、数据安全
具体任务拆解
数据库层：ipad_device_info 建表、初始化配置数据、字段映射表输出
架构层：ipad 子包搭建、权限拦截器、请求头校验、WebSocket 服务搭建
接口开发：6 大模块 37 个接口全量开发、事务控制、异常处理
逻辑复用：对接 core 层菜品、订单、财务、会员、库存服务，确保业务逻辑统一
文档输出：接口文档（含入参出参、字段说明、错误码）、数据库变更脚本
问题修复：联调、测试阶段所有后端 Bug 修复
地龙（前端负责人）
总责任：78 个窗体全量开发、UI 还原、交互逻辑、平板适配、前端性能
具体任务拆解
架构层：ipad 路由分组搭建、环境配置、全局请求拦截、请求头自动注入
组件层：iPad 专属公共组件开发（弹窗、菜品卡、桌台块、扫码）
页面层：50 个主页面 + 28 个弹窗全量开发，严格对照字段映射表
交互层：本地订单缓存、横屏适配、触控优化、动效实现、断网缓存
样式层：轻奢主题、毛玻璃效果、11/12.9 寸自适应
联调修复：对接 37 个接口，修复所有前端 Bug
solo（全栈 / 测试 / 运维 / 项目管理）
总责任：第三方对接、联调推进、全量测试、部署上线、项目进度、代码规范
具体任务拆解
第三方对接：打印服务、支付接口、OpenClaw AI 对接、扫码能力
项目管理：Git 分支管理、每日站会、进度台账、变更记录
测试工作：测试用例编写、功能测试、异常测试、兼容性测试、回归测试
代码评审：前后端代码交叉评审，检查字段对齐、端口规范、逻辑合规
部署运维：开发 / 测试 / 生产环境搭建、Nginx 配置、发布上线、监控
文档输出：测试报告、部署手册、运维手册、用户操作手册
七、30 工作日逐日排期表（精确到天，每日交付物明确）
第 1-5 天：底座搭建阶段
表格
天数	天龙	地龙	solo	当日里程碑
1	搭建 ipad 子包、权限拦截器；建表 SQL 编写执行	搭建 ipad 路由分组、环境配置、全局布局	Git 分支创建、规范文档输出、开发环境检查	工程结构就绪
2	登录、门店、设备绑定接口开发	登录层 4 个页面开发、请求拦截器封装	端口连通性校验、数据库账号权限开通	登录页可访问
3	WebSocket 服务搭建、消息推送基础能力	公共组件开发：IpadPopup、DishCard	打印服务初步对接、AI 接口连通性测试	公共组件雏形
4	桌台模块 6 个接口开发	桌台平面图基础渲染、桌台组件开发	输出第一版测试用例框架	桌台接口联调
5	菜品查询类接口开发、字段映射表初版输出	菜品分类页、菜品列表页开发	阶段验收、输出问题清单	第一阶段验收
第 6-15 天：点餐核心攻坚阶段
表格
天数	天龙	地龙	solo
6	菜品详情、套餐、模板接口	菜品详情页、套餐页开发	跟进联调，记录字段不匹配问题
7	加菜、改菜、删菜接口	已点清单、数量调整弹窗开发	打印模板调试
8	提交后厨、加急、分餐接口	备注编辑、分餐页开发	后厨推送联调
9	退菜接口、审计日志对接	退菜申请页、退菜原因弹窗	库存校验场景测试
10	宴席批量点餐接口、搜索接口	宴席点餐页、全局搜索弹窗	点餐流程冒烟测试
11	点餐模块 Bug 修复、接口优化	点餐模块页面打磨、动效优化	第一轮功能测试，输出 Bug 清单
12	点餐模块事务优化、性能调优	点餐模块适配优化、触控体验优化	异常场景测试：沽清、退菜、重复提交
13	对接库存预扣逻辑、异常兜底	批量修改、追加菜品页开发	多门店数据隔离测试
14	点餐模块接口文档完善	点餐剩余页面收尾、12 个弹窗全量完成	测试用例补充
15	点餐模块最终验收、字段映射表更新	点餐层 30 个窗体全量交付	第二阶段整体验收
第 16-23 天：结算会员辅助阶段
表格
天数	天龙	地龙	solo
16	结算账单、优惠接口开发	账单主页、明细页开发	支付接口对接准备
17	支付、押金、发票接口开发	支付方式页、弹窗开发	支付接口联调
18	会员查询、充值、积分接口	会员中心、充值弹窗开发	会员模块联调
19	客户建档、存酒接口	存酒管理、客户建档页	营销优惠券联调
20	AI 对话、库存查询、呼叫服务接口	AI 对话、库存查询页开发	AI 接口联调
21	结算会员模块 Bug 修复	结算、会员页面打磨	全流程联调测试
22	财务流水校验、事务优化	辅助层剩余页面、弹窗收尾	支付全场景测试
23	全部接口最终完善、接口文档定稿	78 个窗体全部开发完成	第三阶段验收，全量第一轮测试完成
第 24-30 天：联调测试上线阶段
表格
天数	天龙	地龙	solo
24	修复第一轮测试 Bug	修复前端交互、样式 Bug	回归测试，输出第二轮 Bug
25	性能优化、异常兜底完善	多尺寸 iPad 适配优化	兼容性测试：iPad11/12.9、系统版本
26	多门店隔离最终校验、权限校验	UI 视觉验收、动效优化	安全测试：权限绕过、参数篡改
27	生产环境数据库配置、脚本执行	前端生产打包	生产环境部署准备
28	后端生产部署、接口验证	前端生产部署、页面验证	全量回归测试
29	上线前最终检查、数据备份	上线前最终验证	上线操作、监控配置
30	上线后问题值守、文档交付	上线后问题值守、文档交付	项目验收、全套文档输出
八、协作与代码规范
8.1 Git 分支管理（solo 执行）
主分支：master（生产环境）
开发主分支：dev-ipad
个人分支：tianlong/order、dilong/table、solo/print
合并规则：个人分支→dev-ipad（提交 PR，交叉评审）→master（上线前合并）
提交规范：类型: 描述，例：feat: 新增点餐退菜接口、fix: 修复桌台状态刷新异常
8.2 每日协作机制
9:00-9:10 站会：昨日完成、今日计划、阻塞问题
18:00 当日代码提交至个人分支，solo 同步更新进度台账
接口变更、字段变更必须在群内同步三人，更新文档版本
8.3 代码评审规则
核心业务接口（下单、支付、退菜、充值）必须天龙 + solo 双人评审
核心页面（点餐主画布、账单页）必须地龙 + solo 双人评审
重点检查项：字段对齐、端口规范、权限校验、异常处理、代码复用
九、测试用例核心框架（solo 执行）
功能测试：37 个接口全覆盖、78 个窗体全覆盖、全业务流程跑通
字段对齐测试：每个接口返回字段与数据库一一比对，无别名、无遗漏
多门店测试：store_id=1/2 切换，数据完全隔离，不串单
权限测试：不同角色员工登录，无权限接口 / 页面不可访问
异常测试：断网、重复提交、沽清下单、金额异常、参数篡改
兼容性测试：iPad 11 寸 / 12.9 寸、iOS 不同版本、横屏锁定
性能测试：菜品列表加载、桌台刷新、下单响应时长
十、最终交付物清单
表格
分类	文件	责任人
数据库	ipad_device_info 建表 SQL 脚本、初始化数据脚本、字段映射对照表	天龙
后端	完整 ipad 模块代码、接口文档（Markdown 版）、部署说明	天龙
前端	78 个窗体完整代码、组件库说明、页面跳转关系图	地龙
测试	测试用例集、测试报告、Bug 闭环台账	solo
运维	部署运维手册、Nginx 配置、环境配置说明	solo
项目	进度台账、变更记录、验收报告	solo