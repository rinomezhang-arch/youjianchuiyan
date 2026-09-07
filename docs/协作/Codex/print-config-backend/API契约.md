# 给Trae的第一阶段接口契约

前缀/api/restaurant-print；沿用现有JWT认证。返回`{code,message,data}`，错误HTTP状态与code一致。GET `/printers?storeId=1`、`/rules?storeId=1`返回数组，含归档记录。普通门店可省storeId自动限定本店；GM必须显式正整数storeId。无身份/无门店/越店403，格式及未知字段400，引用冲突409。

复核补充：通过权限校验后，storeId还必须存在于真实门店表store_info；正整数但不存在的门店返回HTTP400“门店不存在”，GET/POST均一致。打印机和规则store_id外键引用该表，不能创建孤儿配置。

POST /printers：`{"storeId":1,"name":"前台","type":"network","paperWidth":"80","copies":1,"address":"","archive":false}`。新增省id；更新须id，允许部分更新；archive:true归档，记录保留。type为network/usb/bluetooth/browser，paperWidth字符串58/80/A4，copies整数1-5，name必填1-80字符，address可选最多255字符，仅保存不连接设备。响应data包含id/storeId/name/type/paperWidth/copies/address/archive/createdAt/updatedAt及connectionStatus:"unverified"。

POST /rules：`{"storeId":1,"name":"前台小票","printerId":1,"documentType":"receipt","configuredEnabled":true,"archive":false}`。新增省id；更新可部分更新。documentType只允许receipt/kitchen/refund/daily_report（配置分类，尚无执行器），printerId必须同店未归档。响应data包含id/storeId/name/printerId/documentType/configuredEnabled/archive/createdAt/updatedAt，并固定effectiveEnabled:false、inactiveReason:"device adapter not connected"。

两个POST均拒未知字段，包括connectionStatus/effectiveEnabled等服务端字段；更新不能直接回传整个GET对象。字段不能显式null。布尔值必须JSON true/false。被未归档规则引用的打印机归档返回409，先归档相关规则；归档规则可保留原同店打印机历史引用。归档不可通过普通更新恢复（本阶段无恢复入口）。不提供删除、测试打印或在线成功接口。此阶段不是完整打印系统验收。
