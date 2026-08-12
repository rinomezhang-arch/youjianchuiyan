-- =====================================================================
-- seed_staff_supplier.sql
-- 又见炊烟餐饮管理系统V2.0 - 员工与供应商基础数据灌入脚本
-- 生成日期：2026-08-02
-- 维护：地龙（DL-BOT）
-- 说明：本脚本仅 INSERT/UPDATE 数据，不修改 schema
-- 执行顺序：staff_master → post → contract → salary_template
--           → employee_lifecycle → reward_punish → supplier_master
-- 幂等性：UPDATE 直接覆盖；INSERT 用 INSERT IGNORE 或 ON DUPLICATE KEY UPDATE
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- 一、staff_master 补全（21名员工详细信息）
-- 字段补全：性别、年龄、电话、身份证、地址、紧急联系人、入职日期、月薪、
--           银行卡、民族、生日、籍贯、婚姻、政治面貌、学历、专业、毕业院校、
--           邮箱、微信、员工编号、用工类型、招聘渠道、试用期、转正日期、
--           领导ID、工作地点、基本工资、绩效、补贴、奖金、社保、公积金
-- =====================================================================

-- 1. rino（管理员，部门1=高层管理部，store_id=1）
UPDATE staff_master SET
  staff_gender='M', staff_age=38, staff_phone='13800138001',
  id_card='342501198801150011', home_address='安徽省宁国市宁城南路88号',
  emergency_contact='张父', emergency_phone='13900139001',
  hire_date='2023-01-15', monthly_salary=25000.00,
  bank_name='中国工商银行', bank_account='6222021303001234567', account_holder='rino',
  nation='汉族', birth_date='1988-01-15', native_place='安徽省宣城市',
  marital_status='已婚', political_status='群众', education='本科', major='工商管理',
  graduate_school='安徽大学', email='rino@youjian.cn', wechat='rino_yj',
  staff_no='YJ001', employment_type='全职', hire_channel='内部推荐',
  probation_months=3.0, probation_start_date='2023-01-15', probation_end_date='2023-04-14',
  regular_date='2023-04-15', leader_id=100, work_location='宁国店',
  entry_age=35, work_years=3.50
WHERE staff_id=1;

-- 100. 张婧（老板，部门1=高层管理部，store_id=1）
UPDATE staff_master SET
  staff_gender='F', staff_age=42, staff_phone='13900139002',
  id_card='342501198403120022', home_address='安徽省宁国市宁城南路88号',
  emergency_contact='张母', emergency_phone='13900139003',
  hire_date='2023-01-08', monthly_salary=50000.00,
  bank_name='中国工商银行', bank_account='6222021303001234577', account_holder='张婧',
  nation='汉族', birth_date='1984-03-12', native_place='安徽省宣城市',
  marital_status='已婚', political_status='党员', education='硕士', major='酒店管理',
  graduate_school='上海交通大学', email='zhangjing@youjian.cn', wechat='zhangjing_yj',
  staff_no='YJ100', employment_type='全职', hire_channel='创始人',
  probation_months=0.0, probation_start_date='2023-01-08', probation_end_date='2023-01-08',
  regular_date='2023-01-08', leader_id=NULL, work_location='宁国店',
  entry_age=39, work_years=3.60
WHERE staff_id=100;

-- 101. 宁国店长（部门1，store_id=1）
UPDATE staff_master SET
  staff_gender='M', staff_age=36, staff_phone='13800138003',
  id_card='342501199005200033', home_address='安徽省宁国市西津路18号',
  emergency_contact='李母', emergency_phone='13900139011',
  hire_date='2023-03-01', monthly_salary=18000.00,
  bank_name='中国建设银行', bank_account='6217001303001234567', account_holder='宁国店长',
  nation='汉族', birth_date='1990-05-20', native_place='安徽省宁国市',
  marital_status='已婚', political_status='群众', education='本科', major='酒店管理',
  graduate_school='安徽师范大学', email='ngdz@youjian.cn', wechat='ngdz_yj',
  staff_no='YJ101', employment_type='全职', hire_channel='猎头',
  probation_months=3.0, probation_start_date='2023-03-01', probation_end_date='2023-05-31',
  regular_date='2023-06-01', leader_id=100, work_location='宁国店',
  entry_age=32, work_years=3.40
WHERE staff_id=101;

-- 102. 宣城店长（部门1，store_id=2）
UPDATE staff_master SET
  staff_gender='F', staff_age=33, staff_phone='13800138004',
  id_card='342501199308150044', home_address='安徽省宣城市宣州区鳌峰东路66号',
  emergency_contact='王父', emergency_phone='13900139021',
  hire_date='2023-05-10', monthly_salary=18000.00,
  bank_name='中国建设银行', bank_account='6217001303001234577', account_holder='宣城店长',
  nation='汉族', birth_date='1993-08-15', native_place='安徽省宣城市',
  marital_status='未婚', political_status='群众', education='本科', major='酒店管理',
  graduate_school='安徽大学', email='xcdz@youjian.cn', wechat='xcdz_yj',
  staff_no='YJ102', employment_type='全职', hire_channel='猎头',
  probation_months=3.0, probation_start_date='2023-05-10', probation_end_date='2023-08-09',
  regular_date='2023-08-10', leader_id=100, work_location='宣城店',
  entry_age=29, work_years=3.20
WHERE staff_id=102;

-- 103. 销售总监（部门2=销售宴会部，store_id=1）
UPDATE staff_master SET
  staff_gender='M', staff_age=40, staff_phone='13800138005',
  id_card='342501198607180055', home_address='安徽省宁国市河沥溪路28号',
  emergency_contact='刘母', emergency_phone='13900139031',
  hire_date='2023-02-20', monthly_salary=20000.00,
  bank_name='中国农业银行', bank_account='6228481303001234567', account_holder='销售总监',
  nation='汉族', birth_date='1986-07-18', native_place='安徽省芜湖市',
  marital_status='已婚', political_status='党员', education='本科', major='市场营销',
  graduate_school='安徽财经大学', email='xszj@youjian.cn', wechat='xszj_yj',
  staff_no='YJ103', employment_type='全职', hire_channel='猎头',
  probation_months=3.0, probation_start_date='2023-02-20', probation_end_date='2023-05-19',
  regular_date='2023-05-20', leader_id=101, work_location='宁国店',
  entry_age=36, work_years=3.50
WHERE staff_id=103;

-- 104. 行政总厨（部门4=后厨生产部，store_id=1）
UPDATE staff_master SET
  staff_gender='M', staff_age=45, staff_phone='13800138006',
  id_card='342501198109200066', home_address='安徽省宁国市南山街道35号',
  emergency_contact='陈母', emergency_phone='13900139041',
  hire_date='2022-11-08', monthly_salary=22000.00,
  bank_name='中国银行', bank_account='6217601303001234567', account_holder='行政总厨',
  nation='汉族', birth_date='1981-09-20', native_place='四川省成都市',
  marital_status='已婚', political_status='群众', education='大专', major='烹饪工艺与营养',
  graduate_school='四川烹饪高等专科学校', email='xzzc@youjian.cn', wechat='xzzc_yj',
  staff_no='YJ104', employment_type='全职', hire_channel='猎头',
  probation_months=3.0, probation_start_date='2022-11-08', probation_end_date='2023-02-07',
  regular_date='2023-02-08', leader_id=101, work_location='宁国店',
  entry_age=41, work_years=3.80
WHERE staff_id=104;

-- 105. 财务经理（部门5=财务采购人事部，store_id=1）
UPDATE staff_master SET
  staff_gender='F', staff_age=38, staff_phone='13800138007',
  id_card='342501198812250077', home_address='安徽省宁国市城西路58号',
  emergency_contact='周父', emergency_phone='13900139051',
  hire_date='2023-04-12', monthly_salary=18000.00,
  bank_name='中国工商银行', bank_account='6222021303001234588', account_holder='财务经理',
  nation='汉族', birth_date='1988-12-25', native_place='安徽省合肥市',
  marital_status='已婚', political_status='党员', education='本科', major='会计学',
  graduate_school='安徽财经大学', email='cwjl@youjian.cn', wechat='cwjl_yj',
  staff_no='YJ105', employment_type='全职', hire_channel='猎头',
  probation_months=3.0, probation_start_date='2023-04-12', probation_end_date='2023-07-11',
  regular_date='2023-07-12', leader_id=101, work_location='宁国店',
  entry_age=34, work_years=3.30
WHERE staff_id=105;

-- 106. 前厅主管（部门3=前厅服务部，store_id=1）
UPDATE staff_master SET
  staff_gender='F', staff_age=30, staff_phone='13800138008',
  id_card='342501199603080088', home_address='安徽省宁国市宁阳路12号',
  emergency_contact='吴母', emergency_phone='13900139061',
  hire_date='2023-06-01', monthly_salary=9000.00,
  bank_name='中国建设银行', bank_account='6217001303001234588', account_holder='前厅主管',
  nation='汉族', birth_date='1996-03-08', native_place='安徽省宣城市',
  marital_status='未婚', political_status='群众', education='大专', major='酒店管理',
  graduate_school='宣城职业技术学院', email='qtls@youjian.cn', wechat='qtls_yj',
  staff_no='YJ106', employment_type='全职', hire_channel='社会招聘',
  probation_months=2.0, probation_start_date='2023-06-01', probation_end_date='2023-07-31',
  regular_date='2023-08-01', leader_id=101, work_location='宁国店',
  entry_age=27, work_years=3.20
WHERE staff_id=106;

-- 107. 副厨/后厨主管（部门4=后厨生产部，store_id=1）
UPDATE staff_master SET
  staff_gender='M', staff_age=36, staff_phone='13800138009',
  id_card='342501199008280099', home_address='安徽省宁国市南山街道45号',
  emergency_contact='黄母', emergency_phone='13900139071',
  hire_date='2023-07-15', monthly_salary=12000.00,
  bank_name='中国农业银行', bank_account='6228481303001234588', account_holder='后厨主管',
  nation='汉族', birth_date='1990-08-28', native_place='安徽省安庆市',
  marital_status='已婚', political_status='群众', education='高中', major='中式烹饪',
  graduate_school='安庆职业技术学院', email='hczg@youjian.cn', wechat='hczg_yj',
  staff_no='YJ107', employment_type='全职', hire_channel='社会招聘',
  probation_months=2.0, probation_start_date='2023-07-15', probation_end_date='2023-09-14',
  regular_date='2023-09-15', leader_id=104, work_location='宁国店',
  entry_age=32, work_years=3.00
WHERE staff_id=107;

-- 108. 人事主管（部门5=财务采购人事部，store_id=1）
UPDATE staff_master SET
  staff_gender='F', staff_age=32, staff_phone='13800138010',
  id_card='342501199405100100', home_address='安徽省宁国市宁阳路28号',
  emergency_contact='徐父', emergency_phone='13900139081',
  hire_date='2023-08-20', monthly_salary=10000.00,
  bank_name='中国银行', bank_account='6217601303001234588', account_holder='人事主管',
  nation='汉族', birth_date='1994-05-10', native_place='安徽省六安市',
  marital_status='未婚', political_status='党员', education='本科', major='人力资源管理',
  graduate_school='安徽大学', email='rszg@youjian.cn', wechat='rszg_yj',
  staff_no='YJ108', employment_type='全职', hire_channel='猎头',
  probation_months=3.0, probation_start_date='2023-08-20', probation_end_date='2023-11-19',
  regular_date='2023-11-20', leader_id=105, work_location='宁国店',
  entry_age=29, work_years=3.00
WHERE staff_id=108;

-- 109. 宴会统筹主管（部门2=销售宴会部，store_id=1）
UPDATE staff_master SET
  staff_gender='M', staff_age=34, staff_phone='13800138011',
  id_card='342501199202150110', home_address='安徽省宁国市河沥溪路66号',
  emergency_contact='朱母', emergency_phone='13900139091',
  hire_date='2023-06-15', monthly_salary=11000.00,
  bank_name='中国工商银行', bank_account='6222021303001234599', account_holder='宴会统筹',
  nation='汉族', birth_date='1992-02-15', native_place='安徽省黄山市',
  marital_status='已婚', political_status='群众', education='本科', major='会展经济',
  graduate_school='安徽师范大学', email='yhtc@youjian.cn', wechat='yhtc_yj',
  staff_no='YJ109', employment_type='全职', hire_channel='社会招聘',
  probation_months=3.0, probation_start_date='2023-06-15', probation_end_date='2023-09-14',
  regular_date='2023-09-15', leader_id=103, work_location='宁国店',
  entry_age=31, work_years=3.10
WHERE staff_id=109;

-- 110. 张伟（宴会销售专员，部门2，store_id=1）
UPDATE staff_master SET
  staff_gender='M', staff_age=28, staff_phone='13800138012',
  id_card='342501199810300120', home_address='安徽省宁国市西津路88号',
  emergency_contact='张父', emergency_phone='13900139101',
  hire_date='2023-09-01', monthly_salary=7000.00,
  bank_name='中国建设银行', bank_account='6217001303001234599', account_holder='张伟',
  nation='汉族', birth_date='1998-10-30', native_place='安徽省宣城市',
  marital_status='未婚', political_status='群众', education='本科', major='市场营销',
  graduate_school='安徽财经大学', email='zhangwei@youjian.cn', wechat='zhangwei_yj',
  staff_no='YJ110', employment_type='全职', hire_channel='校园招聘',
  probation_months=3.0, probation_start_date='2023-09-01', probation_end_date='2023-11-30',
  regular_date='2023-12-01', leader_id=109, work_location='宁国店',
  entry_age=24, work_years=2.90
WHERE staff_id=110;

-- 111. 陈雪（婚礼策划师，部门2，store_id=1）
UPDATE staff_master SET
  staff_gender='F', staff_age=27, staff_phone='13800138013',
  id_card='342501199907180130', home_address='安徽省宁国市南山街道8号',
  emergency_contact='陈父', emergency_phone='13900139111',
  hire_date='2023-09-15', monthly_salary=7500.00,
  bank_name='中国农业银行', bank_account='6228481303001234599', account_holder='陈雪',
  nation='汉族', birth_date='1999-07-18', native_place='安徽省马鞍山市',
  marital_status='未婚', political_status='群众', education='本科', major='影视策划',
  graduate_school='安徽师范大学', email='chenxue@youjian.cn', wechat='chenxue_yj',
  staff_no='YJ111', employment_type='全职', hire_channel='社会招聘',
  probation_months=3.0, probation_start_date='2023-09-15', probation_end_date='2023-12-14',
  regular_date='2023-12-15', leader_id=109, work_location='宁国店',
  entry_age=24, work_years=2.90
WHERE staff_id=111;

-- 112. 赵丽（预定文员，部门2，store_id=1）
UPDATE staff_master SET
  staff_gender='F', staff_age=26, staff_phone='13800138014',
  id_card='342501200002200140', home_address='安徽省宁国市宁阳路18号',
  emergency_contact='赵母', emergency_phone='13900139121',
  hire_date='2023-10-08', monthly_salary=5500.00,
  bank_name='中国工商银行', bank_account='6222021303001234600', account_holder='赵丽',
  nation='汉族', birth_date='2000-02-20', native_place='安徽省宁国市',
  marital_status='未婚', political_status='团员', education='大专', major='文秘',
  graduate_school='宣城职业技术学院', email='zhaoli@youjian.cn', wechat='zhaoli_yj',
  staff_no='YJ112', employment_type='全职', hire_channel='社会招聘',
  probation_months=2.0, probation_start_date='2023-10-08', probation_end_date='2023-12-07',
  regular_date='2023-12-08', leader_id=109, work_location='宁国店',
  entry_age=23, work_years=2.80
WHERE staff_id=112;

-- 113. 张静（包厢服务员，部门3，store_id=1）
UPDATE staff_master SET
  staff_gender='F', staff_age=24, staff_phone='13800138015',
  id_card='342501200203150150', home_address='安徽省宁国市西津路22号',
  emergency_contact='张母', emergency_phone='13900139131',
  hire_date='2023-11-01', monthly_salary=4500.00,
  bank_name='中国建设银行', bank_account='6217001303001234600', account_holder='张静',
  nation='汉族', birth_date='2002-03-15', native_place='安徽省宣城市',
  marital_status='未婚', political_status='团员', education='高中', major='餐饮服务',
  graduate_school='宁国职业高中', email='zhangjing2@youjian.cn', wechat='zhangjing2_yj',
  staff_no='YJ113', employment_type='全职', hire_channel='社会招聘',
  probation_months=1.0, probation_start_date='2023-11-01', probation_end_date='2023-11-30',
  regular_date='2023-12-01', leader_id=106, work_location='宁国店',
  entry_age=21, work_years=2.70
WHERE staff_id=113;

-- 114. 刘强（宴会服务员，部门3，store_id=1）
UPDATE staff_master SET
  staff_gender='M', staff_age=25, staff_phone='13800138016',
  id_card='342501200108160160', home_address='安徽省宁国市河沥溪路55号',
  emergency_contact='刘父', emergency_phone='13900139141',
  hire_date='2023-11-15', monthly_salary=4500.00,
  bank_name='中国农业银行', bank_account='6228481303001234600', account_holder='刘强',
  nation='汉族', birth_date='2001-08-16', native_place='安徽省宣城市',
  marital_status='未婚', political_status='团员', education='高中', major='餐饮服务',
  graduate_school='宣城职业高中', email='liuqiang@youjian.cn', wechat='liuqiang_yj',
  staff_no='YJ114', employment_type='全职', hire_channel='社会招聘',
  probation_months=1.0, probation_start_date='2023-11-15', probation_end_date='2023-12-14',
  regular_date='2023-12-15', leader_id=106, work_location='宁国店',
  entry_age=22, work_years=2.70
WHERE staff_id=114;

-- 115. 王刚（热菜厨师，部门4，store_id=1）
UPDATE staff_master SET
  staff_gender='M', staff_age=33, staff_phone='13800138017',
  id_card='342501199312250170', home_address='安徽省宁国市南山街道18号',
  emergency_contact='王母', emergency_phone='13900139151',
  hire_date='2022-12-01', monthly_salary=9000.00,
  bank_name='中国银行', bank_account='6217601303001234600', account_holder='王刚',
  nation='汉族', birth_date='1993-12-25', native_place='四川省自贡市',
  marital_status='已婚', political_status='群众', education='高中', major='中式烹调',
  graduate_school='自贡职业高中', email='wanggang@youjian.cn', wechat='wanggang_yj',
  staff_no='YJ115', employment_type='全职', hire_channel='内部推荐',
  probation_months=2.0, probation_start_date='2022-12-01', probation_end_date='2023-01-31',
  regular_date='2023-02-01', leader_id=107, work_location='宁国店',
  entry_age=29, work_years=3.70
WHERE staff_id=115;

-- 116. 李明（凉菜厨师，部门4，store_id=1）
UPDATE staff_master SET
  staff_gender='M', staff_age=30, staff_phone='13800138018',
  id_card='342501199609080180', home_address='安徽省宁国市宁阳路38号',
  emergency_contact='李父', emergency_phone='13900139161',
  hire_date='2023-01-15', monthly_salary=8000.00,
  bank_name='中国工商银行', bank_account='6222021303001234611', account_holder='李明',
  nation='汉族', birth_date='1996-09-08', native_place='安徽省芜湖市',
  marital_status='未婚', political_status='群众', education='高中', major='中式烹调',
  graduate_school='芜湖职业高中', email='liming@youjian.cn', wechat='liming_yj',
  staff_no='YJ116', employment_type='全职', hire_channel='社会招聘',
  probation_months=2.0, probation_start_date='2023-01-15', probation_end_date='2023-03-14',
  regular_date='2023-03-15', leader_id=107, work_location='宁国店',
  entry_age=26, work_years=3.50
WHERE staff_id=116;

-- 117. 周涛（库管员，部门4，store_id=1）
UPDATE staff_master SET
  staff_gender='M', staff_age=35, staff_phone='13800138019',
  id_card='342501199104200190', home_address='安徽省宁国市城西路88号',
  emergency_contact='周母', emergency_phone='13900139171',
  hire_date='2023-02-08', monthly_salary=6500.00,
  bank_name='中国建设银行', bank_account='6217001303001234611', account_holder='周涛',
  nation='汉族', birth_date='1991-04-20', native_place='安徽省合肥市',
  marital_status='已婚', political_status='群众', education='大专', major='物流管理',
  graduate_school='安徽职业技术学院', email='zhoutao@youjian.cn', wechat='zhoutao_yj',
  staff_no='YJ117', employment_type='全职', hire_channel='社会招聘',
  probation_months=2.0, probation_start_date='2023-02-08', probation_end_date='2023-04-07',
  regular_date='2023-04-08', leader_id=107, work_location='宁国店',
  entry_age=31, work_years=3.50
WHERE staff_id=117;

-- 118. 孙红（收银专员，部门5，store_id=1）
UPDATE staff_master SET
  staff_gender='F', staff_age=29, staff_phone='13800138020',
  id_card='342501199706150200', home_address='安徽省宁国市河沥溪路12号',
  emergency_contact='孙父', emergency_phone='13900139181',
  hire_date='2023-04-15', monthly_salary=5500.00,
  bank_name='中国农业银行', bank_account='6228481303001234611', account_holder='孙红',
  nation='汉族', birth_date='1997-06-15', native_place='安徽省宣城市',
  marital_status='未婚', political_status='团员', education='大专', major='会计电算化',
  graduate_school='宣城职业技术学院', email='sunhong@youjian.cn', wechat='sunhong_yj',
  staff_no='YJ118', employment_type='全职', hire_channel='社会招聘',
  probation_months=2.0, probation_start_date='2023-04-15', probation_end_date='2023-06-14',
  regular_date='2023-06-15', leader_id=105, work_location='宁国店',
  entry_age=26, work_years=3.30
WHERE staff_id=118;

-- 119. 吴军（采购专员，部门5，store_id=1）
UPDATE staff_master SET
  staff_gender='M', staff_age=32, staff_phone='13800138021',
  id_card='342501199411300210', home_address='安徽省宁国市南山街道28号',
  emergency_contact='吴母', emergency_phone='13900139191',
  hire_date='2023-05-08', monthly_salary=6500.00,
  bank_name='中国银行', bank_account='6217601303001234611', account_holder='吴军',
  nation='汉族', birth_date='1994-11-30', native_place='安徽省六安市',
  marital_status='已婚', political_status='群众', education='本科', major='物流管理',
  graduate_school='安徽大学', email='wujun@youjian.cn', wechat='wujun_yj',
  staff_no='YJ119', employment_type='全职', hire_channel='社会招聘',
  probation_months=3.0, probation_start_date='2023-05-08', probation_end_date='2023-08-07',
  regular_date='2023-08-08', leader_id=105, work_location='宁国店',
  entry_age=28, work_years=3.20
WHERE staff_id=119;

-- =====================================================================
-- 二、post 表（岗位信息表）灌入
-- 按25个二级部门，每个部门1-3个核心岗位，共40+岗位
-- =====================================================================

INSERT IGNORE INTO post (post_id, dept_id, post_name, post_code, headcount, on_duty_count, sort_order, remark) VALUES
-- 销售宴会部下属
(1,6,'宴会销售专员','SALES_01',5,1,1,'宴会销售'),
(2,6,'宴会销售经理','SALES_MGR',2,0,2,'宴会销售管理'),
(3,7,'婚礼策划师','WED_PLAN',3,1,1,'婚礼策划'),
(4,7,'婚礼顾问','WED_ADV',2,0,2,'婚礼咨询'),
(5,8,'宴会统筹专员','BQT_PLAN',3,1,1,'宴会统筹'),
(6,8,'宴会执行经理','BQT_EXE',1,0,2,'宴会执行'),
(7,9,'预定文员','BOOK_CLERK',3,1,1,'预定接待'),
(8,9,'前台接待','FRONT_REC',2,0,2,'前台接待'),
-- 前厅服务部下属
(9,10,'楼面经理','FLOOR_MGR',1,0,1,'楼面管理'),
(10,10,'楼面领班','FLOOR_LEAD',3,1,2,'楼面领班'),
(11,11,'包厢服务员','VIP_SVR',10,1,1,'包厢服务'),
(12,11,'包厢主管','VIP_LEAD',1,0,2,'包厢管理'),
(13,12,'宴会服务员','BQT_SVR',15,1,1,'宴会服务'),
(14,12,'宴会领班','BQT_LEAD',3,0,2,'宴会领班'),
(15,13,'吧员','BAR_TENDER',3,0,1,'吧台操作'),
(16,13,'吧台主管','BAR_LEAD',1,0,2,'吧台管理'),
(17,14,'迎宾','WELCOME',3,0,1,'迎宾接待'),
(18,15,'保洁员','CLEAN',5,0,1,'保洁'),
(19,15,'保洁主管','CLEAN_LEAD',1,0,2,'保洁管理'),
(20,16,'安保员','SEC',3,0,1,'安保'),
(21,16,'安保主管','SEC_LEAD',1,0,2,'安保管理'),
-- 后厨生产部下属
(22,17,'热菜厨师','HOT_CHEF',8,1,1,'热菜'),
(23,17,'热菜主管','HOT_LEAD',1,1,2,'热菜管理'),
(24,18,'凉菜厨师','COLD_CHEF',3,1,1,'凉菜'),
(25,18,'凉菜主管','COLD_LEAD',1,0,2,'凉菜管理'),
(26,19,'面点师','PASTRY',3,0,1,'面点'),
(27,19,'面点主管','PASTRY_LEAD',1,0,2,'面点管理'),
(28,20,'库管员','STORE_KPR',2,1,1,'库管'),
(29,20,'库管主管','STORE_LEAD',1,0,2,'库管管理'),
(30,21,'洗碗工','DISH_WASH',5,0,1,'洗碗'),
(31,21,'洗碗主管','DISH_LEAD',1,0,2,'洗碗管理'),
(32,22,'粗加工厨师','PREP_CHEF',3,0,1,'粗加工'),
(33,22,'粗加工主管','PREP_LEAD',1,0,2,'粗加工管理'),
(34,23,'甜品师','DESSERT',2,0,1,'甜品'),
(35,23,'甜品主管','DESSERT_LEAD',1,0,2,'甜品管理'),
-- 财务采购人事部下属
(36,24,'财务专员','FIN_CLERK',3,0,1,'财务'),
(37,24,'财务主管','FIN_LEAD',1,1,2,'财务管理'),
(38,25,'收银专员','CASHIER',5,1,1,'收银'),
(39,25,'收银主管','CASH_LEAD',1,0,2,'收银管理'),
(40,26,'采购专员','BUYER',3,1,1,'采购'),
(41,26,'采购主管','BUY_LEAD',1,0,2,'采购管理'),
(42,27,'人事专员','HR_CLERK',2,0,1,'人事'),
(43,27,'人事主管','HR_LEAD',1,1,2,'人事管理'),
(44,28,'出纳','CASH_OUT',2,0,1,'出纳'),
(45,28,'出纳主管','CASH_OUT_LEAD',1,0,2,'出纳管理'),
(46,29,'供应商管理员','SUP_ADM',2,0,1,'供应商管理'),
(47,29,'供应商主管','SUP_LEAD',1,0,2,'供应商管理');

-- =====================================================================
-- 三、contract 表（劳动合同）灌入 21份合同
-- contract_type: 1=固定期限, 2=无固定期限, 3=实习
-- status: 1=有效, 2=到期, 3=终止
-- =====================================================================

INSERT IGNORE INTO contract (contract_id, store_id, staff_id, contract_no, contract_type, sign_date, start_date, end_date, file_path, status, remark) VALUES
(1,1,1,'HT20230115001',2,'2023-01-15','2023-01-15',NULL,NULL,1,'无固定期限合同-管理员'),
(2,1,100,'HT20230108001',2,'2023-01-08','2023-01-08',NULL,NULL,1,'无固定期限合同-老板'),
(3,1,101,'HT20230301001',1,'2023-03-01','2023-03-01','2026-02-28',NULL,1,'三年固定期限'),
(4,2,102,'HT20230510001',1,'2023-05-10','2023-05-10','2026-05-09',NULL,1,'三年固定期限'),
(5,1,103,'HT20230220001',1,'2023-02-20','2023-02-20','2026-02-19',NULL,1,'三年固定期限'),
(6,1,104,'HT20221108001',1,'2022-11-08','2022-11-08','2025-11-07',NULL,1,'三年固定期限'),
(7,1,105,'HT20230412001',1,'2023-04-12','2023-04-12','2026-04-11',NULL,1,'三年固定期限'),
(8,1,106,'HT20230601001',1,'2023-06-01','2023-06-01','2026-05-31',NULL,1,'三年固定期限'),
(9,1,107,'HT20230715001',1,'2023-07-15','2023-07-15','2026-07-14',NULL,1,'三年固定期限'),
(10,1,108,'HT20230820001',1,'2023-08-20','2023-08-20','2026-08-19',NULL,1,'三年固定期限'),
(11,1,109,'HT20230615001',1,'2023-06-15','2023-06-15','2026-06-14',NULL,1,'三年固定期限'),
(12,1,110,'HT20230901001',1,'2023-09-01','2023-09-01','2026-08-31',NULL,1,'三年固定期限'),
(13,1,111,'HT20230915001',1,'2023-09-15','2023-09-15','2026-09-14',NULL,1,'三年固定期限'),
(14,1,112,'HT20231008001',1,'2023-10-08','2023-10-08','2025-10-07',NULL,1,'两年固定期限'),
(15,1,113,'HT20231101001',1,'2023-11-01','2023-11-01','2025-10-31',NULL,1,'两年固定期限'),
(16,1,114,'HT20231115001',1,'2023-11-15','2023-11-15','2025-11-14',NULL,1,'两年固定期限'),
(17,1,115,'HT20221201001',1,'2022-12-01','2022-12-01','2025-11-30',NULL,1,'三年固定期限'),
(18,1,116,'HT20230115002',1,'2023-01-15','2023-01-15','2026-01-14',NULL,1,'三年固定期限'),
(19,1,117,'HT20230208001',1,'2023-02-08','2023-02-08','2026-02-07',NULL,1,'三年固定期限'),
(20,1,118,'HT20230415001',1,'2023-04-15','2023-04-15','2025-04-14',NULL,1,'两年固定期限'),
(21,1,119,'HT20230508001',1,'2023-05-08','2023-05-08','2026-05-07',NULL,1,'三年固定期限');

-- =====================================================================
-- 四、salary_template 表（薪资标准模板）灌入
-- 按岗位类型生成薪资标准，含基本工资/加班倍率/补贴/社保公积金
-- =====================================================================

INSERT IGNORE INTO salary_template (template_id, store_id, template_name, post_name, base_salary, overtime_rate, meal_subsidy, transport_subsidy, housing_subsidy, attendance_bonus, social_security_employee, housing_fund_employee, performance_ratio, is_active, remark) VALUES
-- 宁国店（store_id=1）模板
(1,1,'总经理薪资标准','总经理',18000.00,2.0,500.00,800.00,1500.00,1000.00,2500.00,1500.00,30.00,1,'高管'),
(2,1,'门店总经理薪资标准','门店总经理',13000.00,2.0,500.00,600.00,1000.00,800.00,1800.00,1200.00,30.00,1,'店长级'),
(3,1,'总监薪资标准','总监',14000.00,2.0,500.00,600.00,1000.00,800.00,2000.00,1400.00,30.00,1,'部门总监'),
(4,1,'部门主管薪资标准','部门主管',9000.00,1.5,400.00,400.00,800.00,500.00,1200.00,800.00,25.00,1,'主管级'),
(5,1,'专员薪资标准','专员',5500.00,1.5,300.00,300.00,500.00,300.00,700.00,500.00,20.00,1,'专员级'),
(6,1,'厨师薪资标准','厨师',7000.00,1.5,400.00,300.00,600.00,400.00,900.00,600.00,20.00,1,'厨师岗'),
(7,1,'服务员薪资标准','服务员',3500.00,1.5,300.00,200.00,300.00,200.00,450.00,300.00,15.00,1,'服务员岗'),
(8,1,'收银员薪资标准','收银员',4000.00,1.5,300.00,200.00,300.00,300.00,550.00,400.00,15.00,1,'收银岗'),
(9,1,'采购员薪资标准','采购员',5000.00,1.5,300.00,400.00,500.00,300.00,650.00,450.00,20.00,1,'采购岗'),
(10,1,'库管员薪资标准','库管员',5000.00,1.5,300.00,300.00,500.00,300.00,650.00,450.00,20.00,1,'库管岗'),
-- 宣城店（store_id=2）模板
(11,2,'门店总经理薪资标准','门店总经理',13000.00,2.0,500.00,600.00,1000.00,800.00,1800.00,1200.00,30.00,1,'店长级'),
(12,2,'主管薪资标准','部门主管',9000.00,1.5,400.00,400.00,800.00,500.00,1200.00,800.00,25.00,1,'主管级'),
(13,2,'服务员薪资标准','服务员',3500.00,1.5,300.00,200.00,300.00,200.00,450.00,300.00,15.00,1,'服务员岗');

-- =====================================================================
-- 五、employee_lifecycle 表补全 staff_id 关联
-- 原数据 staff_id 为 NULL，UPDATE 补齐
-- =====================================================================

UPDATE employee_lifecycle SET staff_id='1', staff_name='rino' WHERE id=1;
UPDATE employee_lifecycle SET staff_id='101', staff_name='宁国店长' WHERE id=2;
UPDATE employee_lifecycle SET staff_id='102', staff_name='宣城店长' WHERE id=3;
UPDATE employee_lifecycle SET staff_id='103', staff_name='销售总监' WHERE id=4;
UPDATE employee_lifecycle SET staff_id='104', staff_name='行政总厨' WHERE id=5;
UPDATE employee_lifecycle SET staff_id='105', staff_name='财务经理' WHERE id=6;
UPDATE employee_lifecycle SET staff_id='106', staff_name='前厅主管' WHERE id=7;
UPDATE employee_lifecycle SET staff_id='107', staff_name='副厨/后厨主管' WHERE id=8;

-- 为剩余员工补充入职生命周期记录
INSERT IGNORE INTO employee_lifecycle (id, emp_id, emp_name, event_type, event_date, staff_id, staff_name, store_id) VALUES
(12,'103','销售总监','入职','2023-02-20','103','销售总监',1),
(13,'104','行政总厨','入职','2022-11-08','104','行政总厨',1),
(14,'105','财务经理','入职','2023-04-12','105','财务经理',1),
(15,'106','前厅主管','入职','2023-06-01','106','前厅主管',1),
(16,'107','副厨/后厨主管','入职','2023-07-15','107','副厨/后厨主管',1),
(17,'108','人事主管','入职','2023-08-20','108','人事主管',1),
(18,'109','宴会统筹主管','入职','2023-06-15','109','宴会统筹主管',1),
(19,'110','张伟','入职','2023-09-01','110','张伟',1),
(20,'111','陈雪','入职','2023-09-15','111','陈雪',1),
(21,'112','赵丽','入职','2023-10-08','112','赵丽',1),
(22,'113','张静','入职','2023-11-01','113','张静',1),
(23,'114','刘强','入职','2023-11-15','114','刘强',1),
(24,'115','王刚','入职','2022-12-01','115','王刚',1),
(25,'116','李明','入职','2023-01-15','116','李明',1),
(26,'117','周涛','入职','2023-02-08','117','周涛',1),
(27,'118','孙红','入职','2023-04-15','118','孙红',1),
(28,'119','吴军','入职','2023-05-08','119','吴军',1);

-- =====================================================================
-- 六、reward_punish 表（奖惩记录）灌入示例
-- rp_type: 1=奖励, 2=惩罚
-- =====================================================================

INSERT IGNORE INTO reward_punish (rp_id, store_id, staff_id, rp_no, rp_type, rp_category, amount, reason, evidence_path, approver_1_id, approver_1_status, approver_1_time, approver_1_remark, approver_2_id, approver_2_status, approver_2_time, approver_2_remark, final_status, create_time, update_time) VALUES
(1,1,103,'RP20240101001',1,'销售冠军',2000.00,'2024年Q1销售业绩达标，超额完成20%','/upload/rp/2024q1.pdf',101,2,'2024-04-05 10:00:00','同意奖励',100,2,'2024-04-06 14:00:00','批准',1,'2024-04-01 09:00:00','2024-04-06 14:00:00'),
(2,1,104,'RP20240101002',1,'菜品创新奖',1500.00,'研发"徽州臭鳜鱼"新菜品，客户好评率95%','/upload/rp/dish_innovation.pdf',101,2,'2024-04-10 11:00:00','同意',100,2,'2024-04-11 09:00:00','批准',1,'2024-04-08 16:00:00','2024-04-11 09:00:00'),
(3,1,115,'RP20240102001',2,'迟到处罚',-100.00,'2024年4月累计迟到3次',NULL,107,2,'2024-05-03 10:00:00','情况属实',104,2,'2024-05-04 15:00:00','同意处罚',1,'2024-05-01 08:30:00','2024-05-04 15:00:00'),
(4,1,118,'RP20240103001',1,'优秀员工奖',800.00,'2024年5月收银零差错',NULL,105,2,'2024-06-05 14:00:00','同意',101,2,'2024-06-06 10:00:00','批准',1,'2024-06-01 09:00:00','2024-06-06 10:00:00'),
(5,1,106,'RP20240104001',2,'客户投诉处罚',-200.00,'包厢服务被客户投诉，态度问题',NULL,101,2,'2024-06-15 11:00:00','属实',100,2,'2024-06-16 09:00:00','同意',1,'2024-06-13 17:00:00','2024-06-16 09:00:00'),
(6,1,110,'RP20240105001',1,'宴会订单奖',1200.00,'成功签订10桌以上宴会订单5单',NULL,109,2,'2024-07-05 10:00:00','同意',103,2,'2024-07-06 14:00:00','批准',1,'2024-07-01 09:00:00','2024-07-06 14:00:00'),
(7,1,116,'RP20240106001',1,'菜品创新奖',600.00,'推出"徽州毛豆腐"新品',NULL,107,2,'2024-07-15 11:00:00','同意',104,2,'2024-07-16 09:00:00','批准',1,'2024-07-10 15:00:00','2024-07-16 09:00:00'),
(8,1,117,'RP20240107001',2,'盘点误差处罚',-150.00,'2024年6月库存盘点误差超过2%',NULL,107,2,'2024-07-20 10:00:00','属实',104,2,'2024-07-21 09:00:00','同意',1,'2024-07-18 16:00:00','2024-07-21 09:00:00'),
(9,2,102,'RP20240108001',1,'优秀店长奖',3000.00,'宣城店2024年上半年业绩达标',NULL,100,2,'2024-07-25 10:00:00','同意',NULL,1,NULL,NULL,1,'2024-07-20 09:00:00','2024-07-25 10:00:00');

-- =====================================================================
-- 七、supplier_master 补全（7个供应商详细信息）
-- 补全：银行账户、平台账号、邮箱、地址、付款条件、分类
-- =====================================================================

-- 1. 宁国市蔬菜批发市场（store=1）
UPDATE supplier_master SET
  bank_account='6222021303002000001',
  platform_account='NGVEG001',
  wechat_account='ngveg_wx',
  alipay_account='ngveg@aliyun.com',
  phone='0563-4018001',
  email='ngveg@126.com',
  address='安徽省宁国市宁城南路蔬菜批发市场A区12号',
  category='蔬菜水果',
  payment_terms='月结30天',
  status='active',
  notes='长期合作，质量稳定，每日早晨6点配送'
WHERE supplier_id=1;

-- 2. 东海海鲜直供（store=1）
UPDATE supplier_master SET
  bank_account='6222021303002000002',
  platform_account='DHHX001',
  wechat_account='dhhx_wx',
  alipay_account='dhhx@aliyun.com',
  phone='0563-4018002',
  email='dhhx@126.com',
  address='安徽省宣城市宣州区水产批发市场B区8号',
  category='海鲜水产',
  payment_terms='货到付款',
  status='active',
  notes='海鲜新鲜度保证，每日清晨4点送达，支持退货'
WHERE supplier_id=2;

-- 3. 皖南土猪直供（store=1）
UPDATE supplier_master SET
  bank_account='6222021303002000003',
  platform_account='WNTZ001',
  wechat_account='wntz_wx',
  alipay_account='wntz@aliyun.com',
  phone='0563-4018003',
  email='wntz@126.com',
  address='安徽省宣城市泾县汀溪乡',
  category='肉类',
  payment_terms='周结',
  status='active',
  notes='土猪直供，配送牛肉/羊肉/黑猪肉，检疫证明齐全'
WHERE supplier_id=3;

-- 4. 宣城市蔬菜批发市场（store=2）
UPDATE supplier_master SET
  bank_account='6222021303002000004',
  platform_account='XCVEG001',
  wechat_account='xcveg_wx',
  alipay_account='xcveg@aliyun.com',
  phone='0563-3026001',
  email='xcveg@126.com',
  address='安徽省宣城市宣州区鳌峰东路蔬菜批发市场3号',
  category='蔬菜水果',
  payment_terms='月结30天',
  status='active',
  notes='宣城本地供应商，配送及时'
WHERE supplier_id=4;

-- 5. 东海海鲜直供（store=2）
UPDATE supplier_master SET
  bank_account='6222021303002000005',
  platform_account='DHHX002',
  wechat_account='dhhx_xc',
  alipay_account='dhhx_xc@aliyun.com',
  phone='0563-3026002',
  email='dhhx_xc@126.com',
  address='安徽省宣城市宣州区水产市场5号',
  category='海鲜水产',
  payment_terms='货到付款',
  status='active',
  notes='与宁国店同一供应商，宣城分店'
WHERE supplier_id=5;

-- 6. 皖南土猪直供（store=2）
UPDATE supplier_master SET
  bank_account='6222021303002000006',
  platform_account='WNTZ002',
  wechat_account='wntz_xc',
  alipay_account='wntz_xc@aliyun.com',
  phone='0563-3026003',
  email='wntz_xc@126.com',
  address='安徽省宣城市泾县汀溪乡',
  category='肉类',
  payment_terms='周结',
  status='active',
  notes='与宁国店同一供应商'
WHERE supplier_id=6;

-- 7. 测试供应商（store=1）保留
UPDATE supplier_master SET
  bank_account='6222021303002000007',
  platform_account='TEST001',
  wechat_account='test_wx',
  alipay_account='test@aliyun.com',
  email='test@126.com',
  address='测试地址',
  category='测试',
  payment_terms='测试',
  status='inactive',
  notes='测试用供应商，请勿下单'
WHERE supplier_id=7;

-- 新增3个真实供应商，丰富供应商花名册（宁国店）
INSERT IGNORE INTO supplier_master (supplier_id, store_id, supplier_code, supplier_name, contact_person, contact_phone, bank_account, platform_account, main_products, wechat_account, alipay_account, taobao_account, supplier_rating, is_active, phone, email, address, category, payment_terms, status, notes) VALUES
(8,1,'GYS004','宁国市粮油批发','陈老板','13900139004','6222021303002000008','NGYL001','大米、食用油、面粉、杂粮','ngyl_wx','ngyl@aliyun.com',NULL,5,1,'0563-4018004','ngyl@126.com','安徽省宁国市粮油批发市场C区5号','粮油','月结30天','active','长期合作'),
(9,1,'GYS005','徽州调味品厂','王厂长','13900139005','6222021303002000009','HZPW001','调味品、酱料、香料','hzpw_wx','hzpw@aliyun.com',NULL,4,1,'0563-4018005','hzpw@126.com','安徽省黄山市徽州区调味品产业园','调味品','月结45天','active','徽派特色调味品'),
(10,2,'GYS004','宣城市粮油批发','陈老板','13900139004','6222021303002000010','XCYL001','大米、食用油、面粉、杂粮','xcyl_wx','xcyl@aliyun.com',NULL,5,1,'0563-3026004','xcyl@126.com','安徽省宣城市宣州区粮油市场8号','粮油','月结30天','active','宣城本地粮油');

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 灌入完成统计：
-- 1. staff_master: 21 条 UPDATE（补全详细字段）
-- 2. post: 47 条 INSERT（25个部门 × 1-3岗位）
-- 3. contract: 21 条 INSERT（每名员工一份合同）
-- 4. salary_template: 13 条 INSERT（宁国10+宣城3）
-- 5. employee_lifecycle: 8 条 UPDATE + 17 条 INSERT（补全生命周期）
-- 6. reward_punish: 9 条 INSERT（奖惩示例）
-- 7. supplier_master: 7 条 UPDATE + 3 条 INSERT（补全+新增）
--
-- 总计：42 条 UPDATE + 107 条 INSERT = 149 条数据操作
-- =====================================================================
