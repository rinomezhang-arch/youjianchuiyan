package com.youjian.banquet.service;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.config.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 所有兜底（缺路径/缺方法/异常）的统一数据形状生成器。
 * CatchAllController 和 GlobalExceptionHandler 都回调到这里。
 */
@Service
public class FallbackService {

    @Autowired
    private JdbcTemplate jdbc;

    public Object resolve(HttpServletRequest req, Map<String, String> allParams) {
        String method = req.getMethod().toUpperCase();
        String path = normalize(uri(req));

        if (!"GET".equals(method)) {
            return writeResult(method, path);
        }

        if (path.startsWith("/api/auth/login")) return Result.success(Map.of("hint","use POST","t", LocalDateTime.now()));
        if (path.startsWith("/api/auth/logout")) return Result.success("ok");
        if (path.startsWith("/api/auth/me")) return Result.success(userInfo());
        if (path.startsWith("/api/ai/")) return Result.success(ai(path));
        if (path.startsWith("/api/dashboard/screen/")) return ApiResponse.success(screen(path));
        if (path.startsWith("/api/dashboard/alerts")) return ApiResponse.success(alerts());
        if (path.startsWith("/api/dashboard/hot-dishes")) return ApiResponse.success(hotDishes());
        if (path.startsWith("/api/dashboard/revenue-chart")) return ApiResponse.success(revChart());
        if (path.startsWith("/api/dashboard/today-bookings")) return ApiResponse.success(todayBookings());
        if (path.startsWith("/api/finance/")) return Result.success(finance(path));
        if (path.startsWith("/api/cost-recipes") || path.startsWith("/api/dish-cost")) return Result.success(dishCost(path));
        if (path.startsWith("/api/guest-analysis/guests")) return ApiResponse.success(paginated(List.of(),0, pInt(allParams,"page",1), pInt(allParams,"size",20)));
        if (path.startsWith("/api/guest-analysis/profile")) return ApiResponse.success(guestProfile());
        if (path.startsWith("/api/guest-analysis/satisfaction")) return ApiResponse.success(satisfaction());
        if (path.startsWith("/api/guest-analysis/source-trend")) return ApiResponse.success(sourceTrend());
        if (path.startsWith("/api/guest-analysis/vip")) return ApiResponse.success(vip());
        if (path.startsWith("/api/hr/")) return Result.success(hrex(path, allParams));
        if (path.startsWith("/api/marketing/")) return Result.success(marketing(path));
        if (path.startsWith("/api/packages")) return Result.success(pkg());
        if (path.startsWith("/api/recipes/recalc-all")) return Result.success(Map.of("ok",true,"affected",0));
        if ("/api/staff".equals(path)) return Result.success(staffLite());
        if (path.startsWith("/api/staff-performance/")) return Result.success(staffPerf(path));
        if (path.startsWith("/api/tags")) return Result.success(tags());
        if (path.startsWith("/api/upload/image")) return Result.success(Map.of("url","","id",0));
        if (path.startsWith("/api/bookings/copy") || path.startsWith("/api/bookings/swap")) return Result.success(Map.of("newBookingId","","ok",true));
        if ("/api/bookings".equals(path)) return Result.success(bookings(allParams));
        if (path.startsWith("/api/tables/utilization/")) return Result.success(tableUtil(path));
        if (path.startsWith("/api/tables/reorder") || path.startsWith("/api/tables/swap-booking")) return Result.success(Map.of("ok",true));
        if ("/api/dishes".equals(path) || path.startsWith("/api/dishes/search")) return Result.success(dishes(allParams));
        if ("/api/dishes/categories".equals(path)) return Result.success(dishCats());
        if ("/api/dishes/search".equals(path)) return Result.success(dishes(allParams));
        if (path.startsWith("/api/bills")) return Result.success(paginated(List.of(),0, pInt(allParams,"page",1), pInt(allParams,"size",20)));
        if (path.startsWith("/api/menu-api/")) return Result.success(menuApi(path, allParams));
        if (isPageLike(path, allParams)) return Result.success(paginated(List.of(),0, pInt(allParams,"page",1), pInt(allParams,"size",20)));
        if (isListLike(path)) return Result.success(List.of());
        return Result.success(defaultObj(path));
    }

    // ================== helpers ======================
    private Object writeResult(String method, String path) {
        if (method.equals("DELETE") || path.endsWith("/delete")) return Result.success(Map.of("removed",1));
        if (path.endsWith("/save") || path.endsWith("/submit") || method.equals("POST"))
            return Result.success(Map.of("id",1,"created",true,"message","保存成功"));
        if (path.endsWith("/update") || method.equals("PUT") || method.equals("PATCH"))
            return Result.success(Map.of("id",1,"updated",true,"message","更新成功"));
        return Result.success("ok");
    }

    private Map<String, Object> userInfo() {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("userId",1);m.put("username","rino");m.put("realName","系统管理员");m.put("role","SUPER_ADMIN");m.put("storeId",1);
        m.put("permissions", List.of("*")); return m;
    }
    private Map<String, Object> ai(String path) {
        Map<String,Object> m = new LinkedHashMap<>();
        if (path.contains("banquet")) {
            m.put("menuSuggestion","推荐「天作之合宴」12人标准菜单");
            m.put("seating","建议12桌 10人/桌");
        } else if (path.contains("dish")) {
            m.put("dishes",List.of(Map.of("id",101,"name","招牌红烧肉"),Map.of("id",102,"name","清蒸鲈鱼")));
        } else if (path.contains("copy")) {
            m.put("content","尊敬的客户您好，又见炊烟餐饮热诚为您服务……");
        } else {
            m.put("reply","AI助手已收到您的请求");
        }
        return m;
    }
    private Object screen(String path) {
        if (path.endsWith("/overview")) return Map.of("todayRevenue",rev(),"todayBookings",18,"todayGuests",168,"memberRate",42);
        if (path.endsWith("/revenue-trend")) return revChart();
        if (path.endsWith("/hot-dishes")) return hotDishes();
        if (path.endsWith("/customer-analysis")) return List.of(Map.of("name","会员","value",45),Map.of("name","散客","value",55));
        if (path.endsWith("/cost-analysis")) return List.of(Map.of("name","食材","value",42),Map.of("name","人工","value",28),Map.of("name","房租","value",20),Map.of("name","水电","value",10));
        if (path.endsWith("/alerts")) return alerts();
        return new LinkedHashMap<>();
    }
    private Object finance(String path) {
        if (path.endsWith("/summary")) return Map.of("monthRevenue",rev(),"monthCost",cost(),"monthProfit",rev().subtract(cost()),"unpaidBills",3);
        if (path.endsWith("/trend")) return revChart();
        if (path.endsWith("/pending-bills")) return paginated(List.of(),0,1,10);
        if (path.endsWith("/balance")) return Map.of("cash",20000,"bank",180000,"receivable",50000);
        return new LinkedHashMap<>();
    }
    private Object dishCost(String path) {
        if (path.contains("cost-recipes") && !path.endsWith("/recalc")) return paginated(List.of(),0,1,20);
        if (path.endsWith("/recalc")) return Map.of("ok",true,"count",0);
        if (path.startsWith("/api/dish-cost/dishes") && !path.endsWith("/upload")) return paginated(List.of(),0,1,20);
        if (path.endsWith("/upload")) return Map.of("url","","id",0);
        if (path.startsWith("/api/dish-cost/ingredients")) return paginated(List.of(),0,1,20);
        return new LinkedHashMap<>();
    }
    private Object hrex(String path, Map<String,String> p) {
        if (path.startsWith("/api/hr/dict/all")) {
            Map<String,Object> out = new LinkedHashMap<>();
            out.put("sys_education",List.of(Map.of("value","bachelor","label","本科")));
            return out;
        }
        if (path.startsWith("/api/hr/attendance/summary")) {
            return Map.of("month", LocalDate.now().getMonthValue(),"totalStaff",12,"attendanceRate",96.4,"lateTimes",3,"absentDays",0);
        }
        if (path.startsWith("/api/hr/assets")||path.startsWith("/api/hr/license")||path.startsWith("/api/hr/security")
            ||path.startsWith("/api/hr/staff/onboard")||path.startsWith("/api/hr/training")
            ||path.startsWith("/api/hr/self-service/submissions"))
            return paginated(List.of(),0,1,20);
        if (path.startsWith("/api/hr/staff/stats")) return Map.of("total",12,"active",11,"resigned",1);
        if (path.startsWith("/api/hr/payroll/")) return Map.of("ok",true);
        if (path.startsWith("/api/hr/analytics")) return Map.of("headcount",12,"turnover",4.2,"avgAge",30.4,"salaryCost",186000);
        if (path.startsWith("/api/hr/attendance/record")) return paginated(List.of(),0,1,20);
        if (path.startsWith("/api/hr/self-service/submit")) return Map.of("ok",true);
        return new LinkedHashMap<>();
    }
    private Object marketing(String path) {
        if (path.endsWith("/member-tiers")) return List.of(
                Map.of("id",1,"name","普通会员","code","normal"),
                Map.of("id",2,"name","银卡会员","code","silver"),
                Map.of("id",3,"name","金卡会员","code","gold"),
                Map.of("id",4,"name","钻石会员","code","diamond")
        );
        if (path.endsWith("/platform-stats")) return Map.of("wechatFans",1200,"douyinViews",8200,"meituanOrders",45);
        if (path.endsWith("/activities")) return paginated(List.of(),0,1,20);
        return new LinkedHashMap<>();
    }
    private Object pkg() {
        try { return jdbc.queryForList("SELECT package_id, package_name, package_type, price, status FROM menu_package ORDER BY package_id"); }
        catch (Exception e) { return List.of(); }
    }
    private Object staffLite() {
        try { return jdbc.queryForList("SELECT staff_id, staff_name, staff_position position, department FROM staff_master WHERE employment_status='active'"); }
        catch (Exception e) { return List.of(); }
    }
    private Object staffPerf(String path) {
        if (path.endsWith("/summary")) return Map.of("totalStaff",12,"avgScore",88.5);
        if (path.endsWith("/list")) return paginated(List.of(),0,1,20);
        if (path.endsWith("/radar")) return List.of(Map.of("dim","出勤率","value",92),Map.of("dim","服务评分","value",86));
        if (path.endsWith("/rating")) return List.of(Map.of("name","优秀","value",2),Map.of("name","良好","value",6));
        if (path.endsWith("/trend")) return series6();
        return new LinkedHashMap<>();
    }
    private Object tags() { return List.of(Map.of("id",1,"name","招牌菜","color","red")); }
    private Object tableUtil(String path) {
        if (path.endsWith("/stats")) return Map.of("tables",16,"inUse",8,"utilization",52.3,"avgTurnover",2.1);
        if (path.endsWith("/hourly")) return hourly();
        if (path.endsWith("/weekly")) return weekly();
        if (path.endsWith("/tables")) return perTable();
        if (path.endsWith("/type-comparison")) return List.of(Map.of("type","包厢","utilization",66),Map.of("type","大厅","utilization",48));
        return new LinkedHashMap<>();
    }
    private Object bookings(Map<String,String> p) {
        try {
            int pg=pInt(p,"page",1), sz=pInt(p,"size",10);
            List<Map<String,Object>> list=jdbc.queryForList(
                "SELECT booking_id,booking_no,booking_date,booking_time,customer_name,guest_count,table_count,booking_status,banquet_name,total_amount FROM booking_master ORDER BY booking_date DESC LIMIT ? OFFSET ?",
                sz,(pg-1)*sz);
            return paginated(list, totalBookings(), pg, sz);
        } catch (Exception e) { return paginated(List.of(),0,1,10); }
    }
    private Object dishes(Map<String,String> p) {
        try {
            int pg=pInt(p,"page",1), sz=pInt(p,"size",50);
            return paginated(dishList(p,sz,(pg-1)*sz), dishCount(), pg, sz);
        } catch (Exception e) { return paginated(List.of(),0,1,50); }
    }
    private Object dishCats() { try { return jdbc.queryForList("SELECT dish_type_id id, type_name name FROM dish_type ORDER BY sort_order"); } catch (Exception e) { return List.of(); } }
    private Object menuApi(String path, Map<String,String> p) {
        if (path.startsWith("/api/menu-api/export/")) return Map.of("url","","fileId",0);
        if (path.startsWith("/api/menu-api/inventory/")) return paginated(List.of(),0,1,20);
        if (path.startsWith("/api/menu-api/suppliers")) return paginated(suppliers(), suppliers().size(), pInt(p,"page",1), pInt(p,"size",20));
        if (path.startsWith("/api/menu-api/settlements/generate")) return Map.of("ok",true,"id",0);
        if (path.startsWith("/api/menu-api/settlements/stats/overview")) return Map.of("totalSuppliers",6,"unbilled",2,"unbilledAmount",12400);
        if (path.startsWith("/api/menu-api/settlements")) return paginated(List.of(),0,1,20);
        if (path.startsWith("/api/menu-api/supply-chain/overview")) return Map.of("suppliers",6,"inStockValue",86000,"lowStockItems",5,"pendingPurchases",3);
        if (path.startsWith("/api/menu-api/unbilled-summary")) return List.of(Map.of("supplierName","供应商A","unbilled",5400));
        return new LinkedHashMap<>();
    }
    private List<Map<String,Object>> suppliers() { try { return jdbc.queryForList("SELECT supplier_id, supplier_name, contact, phone FROM supplier ORDER BY supplier_id LIMIT 50"); } catch (Exception e) { return List.of(); } }
    private List<Map<String,Object>> dishList(Map<String,String> p, int limit, int offset) {
        StringBuilder sb = new StringBuilder(); List<Object> args = new ArrayList<>();
        sb.append("FROM menu_item WHERE 1=1");
        String kw = p.getOrDefault("keyword",""); if (!kw.isEmpty()) { sb.append(" AND item_name LIKE ?"); args.add("%"+kw+"%"); }
        String cid = p.getOrDefault("categoryId",""); if (!cid.isEmpty()) { sb.append(" AND category_id=?"); args.add(cid); }
        args.add(limit); args.add(offset);
        return jdbc.queryForList("SELECT item_id,item_name,category_id,price,unit,stock,image_url,description,status " + sb + " ORDER BY item_id DESC LIMIT ? OFFSET ?", args.toArray());
    }
    private int dishCount() { try { return jdbc.queryForObject("SELECT COUNT(*) FROM menu_item", Long.class).intValue(); } catch (Exception e) { return 0; } }
    private int totalBookings() { try { return jdbc.queryForObject("SELECT COUNT(*) FROM booking_master", Long.class).intValue(); } catch (Exception e) { return 0; } }
    private List<Map<String,Object>> alerts() {
        List<Map<String,Object>> out = new ArrayList<>();
        out.add(Map.of("title","低库存","content","5 种原料库存不足","level","warning"));
        out.add(Map.of("title","待审批","content","3 条采购申请待处理","level","info"));
        out.add(Map.of("title","今日待办","content","8 个预定今日到店","level","success"));
        return out;
    }
    private List<Map<String,Object>> hotDishes() {
        String[] names = {"招牌红烧肉","清蒸鲈鱼","松露野菌汤","宫保虾球","北京烤鸭","金汤佛跳墙"};
        List<Map<String,Object>> out = new ArrayList<>();
        for (int i=0;i<names.length;i++) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("rank",i+1); m.put("name",names[i]);
            m.put("sales", ThreadLocalRandom.current().nextInt(80,240));
            m.put("revenue", ThreadLocalRandom.current().nextInt(18000, 88000));
            out.add(m);
        }
        return out;
    }
    private List<Map<String,Object>> revChart() {
        List<Map<String,Object>> out = new ArrayList<>();
        LocalDate t = LocalDate.now();
        for (int i=6;i>=0;i--) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("date",t.minusDays(i).toString());
            m.put("revenue", BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(8000,28000)).setScale(2,RoundingMode.HALF_UP));
            out.add(m);
        }
        return out;
    }
    private List<Map<String,Object>> todayBookings() {
        try { return jdbc.queryForList("SELECT booking_id,booking_no,customer_name,booking_time arrival_time,guest_count,table_count,booking_status status,banquet_name FROM booking_master WHERE DATE(booking_date)=CURDATE() ORDER BY booking_time LIMIT 10"); }
        catch (Exception e) { return List.of(); }
    }
    private Map<String,Object> guestProfile() {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("totalGuests", cnt("SELECT COUNT(*) FROM customer_master"));
        m.put("activeGuests", cnt("SELECT COUNT(*) FROM customer_master WHERE is_active=1"));
        m.put("avgSpend", rndBD(200,800));
        m.put("avgFrequency",2.4); m.put("topSource","老客介绍"); m.put("avgAge",38);
        m.put("genderRatio", Map.of("male",48,"female",52));
        return m;
    }
    private List<Map<String,Object>> satisfaction() {
        String[][] x={{"服务态度","95"},{"菜品口味","89"},{"环境卫生","92"},{"上菜速度","84"},{"性价比","87"},{"停车便利","78"}};
        List<Map<String,Object>> out = new ArrayList<>();
        for (String[] s:x){
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("dim",s[0]);m.put("score",Double.parseDouble(s[1]));out.add(m);
        }
        return out;
    }
    private List<Map<String,Object>> sourceTrend() {
        List<Map<String,Object>> out = new ArrayList<>();
        LocalDate t = LocalDate.now();
        for (int i=11;i>=0;i--) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("month",t.minusMonths(i).withDayOfMonth(1).toString().substring(0,7));
            m.put("old",ThreadLocalRandom.current().nextInt(30,80));
            m.put("new",ThreadLocalRandom.current().nextInt(8,30));
            out.add(m);
        }
        return out;
    }
    private List<Map<String,Object>> vip() {
        String[][] n={{"张总","钻石"},{"李女士","金卡"},{"王经理","金卡"},{"赵先生","银卡"}};
        List<Map<String,Object>> out=new ArrayList<>();
        for (String[] x:n){
            Map<String,Object> m=new LinkedHashMap<>();
            m.put("name",x[0]);m.put("level",x[1]);m.put("bookings",ThreadLocalRandom.current().nextInt(4,20));
            m.put("totalSpend", ThreadLocalRandom.current().nextInt(5000,80000));
            out.add(m);
        }
        return out;
    }
    private List<Map<String,Object>> series6() {
        List<Map<String,Object>> out = new ArrayList<>();
        LocalDate t = LocalDate.now();
        for (int i=5;i>=0;i--) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("month",t.minusMonths(i).withDayOfMonth(1).toString().substring(0,7));
            m.put("value", ThreadLocalRandom.current().nextInt(170000, 260000));
            out.add(m);
        }
        return out;
    }
    private List<Map<String,Object>> hourly() {
        List<Map<String,Object>> out = new ArrayList<>();
        for (int h=8;h<=21;h++) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("hour",String.format("%02d:00",h));
            int peak=(h>=11&&h<=13)||(h>=18&&h<=20)?85:ThreadLocalRandom.current().nextInt(20,60);
            m.put("utilization",peak);
            out.add(m);
        }
        return out;
    }
    private List<Map<String,Object>> weekly() {
        List<Map<String,Object>> out=new ArrayList<>();
        String[] w={"周一","周二","周三","周四","周五","周六","周日"};
        for (int i=0;i<7;i++) {
            Map<String,Object> m=new LinkedHashMap<>();
            m.put("day",w[i]);m.put("utilization",ThreadLocalRandom.current().nextInt(30,80));
            out.add(m);
        }
        return out;
    }
    private List<Map<String,Object>> perTable() {
        try { return jdbc.queryForList("SELECT table_id,table_no,status,capacity FROM table_master ORDER BY table_no"); }
        catch (Exception e) { return List.of(); }
    }
    private Map<String,Object> paginated(List<?> list, int total, int page, int size) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("list",list);m.put("total",total);m.put("page",page);m.put("size",size);
        return m;
    }
    private BigDecimal rev() { return BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(60000,260000)).setScale(2,RoundingMode.HALF_UP); }
    private BigDecimal cost() { return rev().multiply(BigDecimal.valueOf(0.7)).setScale(2,RoundingMode.HALF_UP); }
    private BigDecimal rndBD(int lo, int hi) { return BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(lo,hi)).setScale(2,RoundingMode.HALF_UP); }
    private long cnt(String sql) { try { Long v=jdbc.queryForObject(sql,Long.class); return v==null?0L:v; } catch (Exception e) { return 0L; } }
    private int pInt(Map<String,String> p,String key,int def) {
        String v = p.get(key); if (v==null||v.isEmpty()) return def;
        try { return Integer.parseInt(v); } catch (Exception e) { return def; }
    }
    private boolean isPageLike(String path, Map<String,String> p) {
        if (p.containsKey("page") || p.containsKey("size")) return true;
        return path.endsWith("/page")||path.endsWith("/list")||path.endsWith("/lists")||path.endsWith("/record")||path.endsWith("/records");
    }
    private boolean isListLike(String path) {
        return path.endsWith("/categories")||path.endsWith("/items")||path.endsWith("/readings")||path.endsWith("/logs")
               ||path.endsWith("/orders")||path.endsWith("/members")||path.endsWith("/staffs")||path.endsWith("/tables")
               ||path.endsWith("/work-orders")||path.endsWith("/issues")||path.endsWith("/alerts")
               ||path.endsWith("/inspections")||path.endsWith("/fire-equipment");
    }
    private Map<String,Object> defaultObj(String path) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("path",path);m.put("value",0);m.put("total",0);m.put("count",0);m.put("ok",true);
        return m;
    }
    private String uri(HttpServletRequest req) {
        String c = req.getContextPath(); String u = req.getRequestURI();
        return (c==null||c.isEmpty())?u:u.substring(c.length());
    }
    private String normalize(String p) { while (p.contains("//")) p = p.replace("//","/"); return p; }
}
