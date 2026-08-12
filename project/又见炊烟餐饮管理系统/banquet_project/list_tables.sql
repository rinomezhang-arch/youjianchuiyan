-- 输出 110 张表,按业务域分组
SELECT
  CASE
    WHEN table_name LIKE '%booking%' THEN '01预订'
    WHEN table_name LIKE '%customer%' OR table_name LIKE '%guest%' THEN '02客户'
    WHEN table_name LIKE '%staff%' OR table_name LIKE '%hr%' OR table_name LIKE '%employee%' OR table_name LIKE '%payroll%' OR table_name LIKE '%attendance%' OR table_name LIKE '%leave%' OR table_name LIKE '%overtime%' OR table_name LIKE '%shift%' OR table_name LIKE '%schedule%' THEN '03人事'
    WHEN table_name LIKE '%table%' OR table_name LIKE '%floor%' OR table_name LIKE '%hall%' OR table_name LIKE '%package%' THEN '04桌台'
    WHEN table_name LIKE '%dish%' OR table_name LIKE '%menu%' OR table_name LIKE '%recipe%' OR table_name LIKE '%ingredient%' OR table_name LIKE '%category%' OR table_name LIKE '%cuisine%' THEN '05菜单'
    WHEN table_name LIKE '%inventory%' OR table_name LIKE '%stock%' OR table_name LIKE '%purchase%' OR table_name LIKE '%supplier%' OR table_name LIKE '%material%' OR table_name LIKE '%goods%' THEN '06采购'
    WHEN table_name LIKE '%order%' OR table_name LIKE '%bill%' OR table_name LIKE '%payment%' OR table_name LIKE '%invoice%' OR table_name LIKE '%refund%' OR table_name LIKE '%revenue%' OR table_name LIKE '%finance%' OR table_name LIKE '%tax%' THEN '07订单财务'
    WHEN table_name LIKE '%member%' OR table_name LIKE '%point%' OR table_name LIKE '%coupon%' OR table_name LIKE '%marketing%' OR table_name LIKE '%vip%' OR table_name LIKE '%promot%' THEN '08会员营销'
    WHEN table_name LIKE '%kitchen%' OR table_name LIKE '%cook%' OR table_name LIKE '%station%' OR table_name LIKE '%dish_order%' THEN '09厨房'
    WHEN table_name LIKE '%engineer%' OR table_name LIKE '%maintenance%' OR table_name LIKE '%asset%' OR table_name LIKE '%energy%' OR table_name LIKE '%license%' OR table_name LIKE '%safety%' OR table_name LIKE '%hygiene%' OR table_name LIKE '%waste%' OR table_name LIKE '%device%' OR table_name LIKE '%equipment%' OR table_name LIKE '%inspection%' OR table_name LIKE '%repair%' OR table_name LIKE '%decoration%' THEN '10工程资产'
    WHEN table_name LIKE '%approval%' OR table_name LIKE '%audit%' OR table_name LIKE '%log%' OR table_name LIKE '%role%' OR table_name LIKE '%permission%' OR table_name LIKE '%user%' OR table_name LIKE '%store%' OR table_name LIKE '%config%' OR table_name LIKE '%dict%' OR table_name LIKE '%system%' OR table_name LIKE '%param%' OR table_name LIKE '%notice%' OR table_name LIKE '%message%' OR table_name LIKE '%feedback%' OR table_name LIKE '%report%' OR table_name LIKE '%dashboard%' THEN '11系统'
    ELSE '12其他'
  END AS domain,
  table_name, table_rows,
  ROUND(data_length/1024/1024, 2) AS data_mb
FROM information_schema.tables
WHERE table_schema='banquet'
ORDER BY domain, table_name;
