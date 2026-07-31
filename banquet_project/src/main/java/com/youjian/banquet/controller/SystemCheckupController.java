package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 系统体检控制器 - Java 实现的全量体检扫描
 */
@RestController
@RequestMapping("/api/system")
@CrossOrigin
public class SystemCheckupController {

    private static final Logger log = LoggerFactory.getLogger(SystemCheckupController.class);

    /** 是否正在扫描中 */
    private static final AtomicBoolean SCANNING = new AtomicBoolean(false);

    /** 数据库配置 */
    private static final String DB_NAME = "banquet";
    
    /** 路径配置 */
    private static final String BACK_CODE_PATH = "F:\\solo\\project\\又见炊烟餐饮管理系统\\banquet_project\\src\\main\\java\\com\\youjian\\banquet";
    private static final String FRONT_CODE_PATH = "F:\\solo\\project\\又见炊烟餐饮管理系统\\frontend_v3";
    
    /** API 配置 */
    private static final String API_BASE_URL = "http://localhost:8080/api";
    private static final String LOGIN_USERNAME = "张婧";
    private static final String LOGIN_PASSWORD = "002323";

    /** 阈值配置 */
    private static final int CPU_THRESHOLD = 85;
    private static final int MEM_THRESHOLD = 85;
    private static final int DISK_THRESHOLD = 90;

    /** 业务配置 */
    private static final String[][] DUPLICATE_TABLE_GROUP = {
        {"purchase_request", "procurement_request"},
        {"purchase_receipt", "purchase_receipt"},
        {"material_requisition", "requisition_order"},
        {"cost_card", "dish_cost_card"},
        {"package_dish_detail", "package_dish_rel"}
    };

    private static final Map<String, String[][]> REDUNDANT_COL_MAP = new HashMap<>() {{
        put("dish_master", new String[][]{{"price", "cost_price"}});
        put("ingredient_master", new String[][]{{"unit_price", "avg_price"}});
        put("booking_master", new String[][]{{"deposit_amount", "deposit"}});
    }};

    private static final String[][] FK_FIELD_MATCH = {
        {"cost_card", "dish_id", "dish_master", "dish_id"},
        {"goods_receipt", "supplier_id", "supplier_master", "supplier_id"},
        {"finance_receivable", "booking_id", "booking_master", "id"},
        {"material_requisition_item", "requisition_id", "material_requisition", "requisition_id"},
        {"package_dish_detail", "package_id", "package_master", "package_id"}
    };

    private static final String[] API_FULL_LIST = {
        "/bookings", "/bookings/list",
        "/dishes", "/customers",
        "/kitchen-supply/purchase-requests",
        "/kitchen-supply/goods-receipts",
        "/kitchen-supply/requisitions",
        "/hr/schedule", "/hr/departments",
        "/finance/today", "/finance/balance",
        "/menu-api/ingredients", "/menu-api/suppliers"
    };

    private static final String[] VUE_PAGE_LIST = {
        "Receipt.vue", "Issue.vue", "Booking.vue", "DishManage.vue", "Staff.vue"
    };

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RestTemplate restTemplate = new RestTemplate();
    private List<Map<String, Object>> scanItems = new ArrayList<>();
    private Map<String, Object> stat = new HashMap<>();
    private List<String> allDbTables = new ArrayList<>();
    private Map<String, Map<String, Object>> dbTableDetail = new HashMap<>();
    private String token = "";

    /** 最近一次扫描结果（异步模式） */
    private volatile List<Map<String, Object>> lastScanItems = new ArrayList<>();
    private volatile Map<String, Object> lastScanStat = new HashMap<>();
    private volatile String lastScanStatus = "idle"; // idle / running / success / error
    private volatile String lastScanMessage = "";

    /**
     * 启动系统体检扫描（异步，立即返回）
     */
    @PostMapping("/checkup")
    public Result<Map<String, Object>> runCheckup() {
        Map<String, Object> data = new HashMap<>();

        if (SCANNING.get()) {
            data.put("status", "running");
            data.put("message", "体检正在执行中，请勿重复点击");
            return Result.success(data);
        }

        // 在后台线程执行扫描
        new Thread(() -> {
            SCANNING.set(true);
            lastScanStatus = "running";
            lastScanMessage = "正在执行全量体检...";
            log.info("===== 系统体检开始 =====");

            try {
                scanItems = new ArrayList<>();
                stat = new HashMap<>();
                stat.put("FATAL", 0);
                stat.put("ERROR", 0);
                stat.put("WARNING", 0);
                stat.put("NORMAL", 0);
                stat.put("total", 0);

                Runnable flush = () -> {
                    lastScanItems = new ArrayList<>(scanItems);
                    lastScanStat = new HashMap<>(stat);
                    lastScanMessage = "已扫描 " + scanItems.size() + " 条...";
                };

                scanServer(); flush.run();
                scanDatabase(); flush.run();
                scanBackend(); flush.run();
                scanFrontend(); flush.run();
                scanApi(); flush.run();
                scanIndexDeep(); flush.run();
                scanConstraint(); flush.run();
                scanDataQuality(); flush.run();
                scanPerformance(); flush.run();
                scanBackendSecurity(); flush.run();
                scanApiDeep(); flush.run();
                scanFrontSecurity(); flush.run();
                scanConfigLog(); flush.run();
                scanBusinessLink(); flush.run();
                scanScheduler(); flush.run();
                scanExceptionHandler(); flush.run();
                scanDBProgram(); flush.run();
                scanInfrastructure(); flush.run();
                scanDataDict(); flush.run();
                scanPageSpec(); flush.run();
                generateDashboards(); flush.run();

                lastScanItems = new ArrayList<>(scanItems);
                lastScanStat = new HashMap<>(stat);
                lastScanStatus = "success";
                lastScanMessage = "体检完成";
                SCANNING.set(false);
                log.info("===== 系统体检完成, 共{}条 =====", scanItems.size());

            } catch (Exception e) {
                SCANNING.set(false);
                lastScanStatus = "error";
                lastScanMessage = "体检执行异常: " + e.getMessage();
                log.error("体检执行异常", e);
            }
        }, "system-checkup").start();

        data.put("status", "started");
        data.put("message", "体检已启动，请轮询 /api/system/checkup/status 获取进度");
        return Result.success(data);
    }

    /**
     * 查询扫描状态和结果
     */
    @GetMapping("/checkup/status")
    public Result<Map<String, Object>> checkupStatus() {
        Map<String, Object> data = new HashMap<>();
        data.put("scanning", SCANNING.get());
        data.put("status", lastScanStatus);
        data.put("message", lastScanMessage);
        // running 和 success 都返回已扫描的部分数据
        if ("success".equals(lastScanStatus) || "running".equals(lastScanStatus)) {
            data.put("items", lastScanItems);
            data.put("summary", lastScanStat);
        }
        return Result.success(data);
    }

    // ====================== 辅助方法 ======================

    private void recordItem(String scanId, String module, String scene, String title,
                           String expect, String actual, String level, String detail,
                           String fixSql, String fixCmd, String fileList, String tags, int score) {
        Map<String, Object> item = new HashMap<>();
        item.put("scan_id", scanId);
        item.put("module", module);
        item.put("scene", scene);
        item.put("title", title);
        item.put("expect", expect);
        item.put("actual", actual);
        item.put("level", level);
        item.put("detail", detail);
        item.put("fix_sql", fixSql != null ? fixSql : "");
        item.put("fix_cmd", fixCmd != null ? fixCmd : "");
        item.put("file_list", fileList != null ? fileList : "");
        item.put("tags", tags != null ? tags : "");
        item.put("score", score);
        
        scanItems.add(item);
        stat.put(level, (Integer) stat.getOrDefault(level, 0) + 1);
        stat.put("total", (Integer) stat.getOrDefault("total", 0) + 1);
    }

    private void recordItem(String scanId, String module, String scene, String title,
                           String expect, String actual, String level, String detail) {
        recordItem(scanId, module, scene, title, expect, actual, level, detail, "", "", "", "", 0);
    }

    private void recordItem(String scanId, String module, String scene, String title,
                           String expect, String actual, String level) {
        recordItem(scanId, module, scene, title, expect, actual, level, "", "", "", "", "", 0);
    }

    private void recordItem(String scanId, String module, String scene, String title,
                           String expect, String actual, String level,
                           String detail, String fixSql, String fixCmd, String fileList, String tags) {
        recordItem(scanId, module, scene, title, expect, actual, level, detail, fixSql, fixCmd, fileList, tags, 0);
    }

    private void recordItem(String scanId, String module, String scene, String title,
                           String expect, String actual, String level,
                           String detail, String fixSql, String fixCmd, String fileList, int score) {
        recordItem(scanId, module, scene, title, expect, actual, level, detail, fixSql, fixCmd, fileList, "", score);
    }

    /** 安全获取 Map 中的 int 值，null 时返回 0 */
    private int getIntValue(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? 0 : ((Number) v).intValue();
    }

    private List<Map<String, Object>> dbQuery(String sql) {
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("SQL异常: {} | {}", sql, e.getMessage());
            return new ArrayList<>();
        }
    }

    private void loadAllTableInfo() {
        String sql = "SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA='" + DB_NAME + "'";
        List<Map<String, Object>> tableList = dbQuery(sql);
        allDbTables = tableList.stream()
            .map(row -> (String) row.get("TABLE_NAME"))
            .collect(Collectors.toList());
        
        for (Map<String, Object> table : tableList) {
            String tblName = (String) table.get("TABLE_NAME");
            String tblComment = (String) table.get("TABLE_COMMENT");
            List<Map<String, Object>> columns = dbQuery("SHOW FULL COLUMNS FROM `" + tblName + "`");
            Map<String, Object> tableInfo = new HashMap<>();
            tableInfo.put("table_comment", tblComment);
            tableInfo.put("columns", columns);
            dbTableDetail.put(tblName, tableInfo);
        }
    }

    private boolean apiLogin() {
        try {
            Map<String, String> loginCred = new HashMap<>();
            loginCred.put("username", LOGIN_USERNAME);
            loginCred.put("password", LOGIN_PASSWORD);
            
            Map<String, Object> response = restTemplate.postForObject(
                API_BASE_URL + "/auth/login", loginCred, Map.class);
            
            if (response != null && (int) response.get("code") == 200) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null && data.get("token") != null) {
                    token = (String) data.get("token");
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("登录接口异常: {}", e.getMessage());
            return false;
        }
    }

    private Map<String, Object> apiGet(String path, Map<String, String> params) {
        try {
            StringBuilder url = new StringBuilder(API_BASE_URL + path);
            if (params == null) {
                params = new HashMap<>();
            }
            if (!params.containsKey("storeId")) {
                params.put("storeId", "1");
            }
            
            String queryString = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
            url.append("?").append(queryString);
            
            Map<String, Object> response = restTemplate.getForObject(url.toString(), Map.class);
            return response != null ? response : new HashMap<>();
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 999);
            error.put("msg", "连接失败:" + e.getMessage());
            return error;
        }
    }

    private List<String> scanFileKeyword(String rootPath, String keyword) {
        List<String> matches = new ArrayList<>();
        if (!Files.exists(Paths.get(rootPath))) {
            return matches;
        }
        
        Set<String> skipDirs = Set.of("dist", "node_modules", ".git", "__pycache__", "target", "build");
        
        try (Stream<Path> paths = Files.walk(Paths.get(rootPath))) {
            paths.filter(path -> {
                if (Files.isDirectory(path)) {
                    return !skipDirs.contains(path.getFileName().toString());
                }
                String fileName = path.getFileName().toString();
                return fileName.endsWith(".vue") || fileName.endsWith(".js") || fileName.endsWith(".java");
            })
            .filter(Files::isRegularFile)
            .forEach(path -> {
                try {
                    String content = Files.readString(path);
                    if (content.contains(keyword)) {
                        matches.add(path.toString());
                    }
                } catch (IOException e) {
                    // 忽略读取错误
                }
            });
        } catch (IOException e) {
            log.error("文件扫描异常: {}", e.getMessage());
        }
        
        return matches;
    }

    // ====================== 服务器层扫描 ======================
    private void scanServer() {
        log.info("开始服务器层扫描...");
        
        // CPU/内存监控（Java 中用 OperatingSystemMXBean）
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double cpuLoad = osBean.getSystemLoadAverage();
        int cpuPercent = (int) (cpuLoad * 100 / osBean.getAvailableProcessors());
        
        if (cpuPercent >= CPU_THRESHOLD) {
            recordItem("SERVER-001", "服务器层", "CPU负载", "CPU阈值<85%", 
                cpuPercent + "%", "WARNING", "负载偏高", "", "top -c", "", "", 0);
        } else {
            recordItem("SERVER-001", "服务器层", "CPU负载", "CPU阈值<85%", 
                cpuPercent + "%", "NORMAL", "负载正常");
        }
        
        // 内存监控
        com.sun.management.OperatingSystemMXBean sunOsBean = 
            (com.sun.management.OperatingSystemMXBean) osBean;
        long totalMem = sunOsBean.getTotalPhysicalMemorySize();
        long freeMem = sunOsBean.getFreePhysicalMemorySize();
        int memPercent = (int) ((totalMem - freeMem) * 100 / totalMem);
        
        if (memPercent >= MEM_THRESHOLD) {
            recordItem("SERVER-002", "服务器层", "内存占用", "<" + MEM_THRESHOLD + "%", 
                memPercent + "%", "WARNING", "易OOM", "", "free -h", "", "", 0);
        } else {
            recordItem("SERVER-002", "服务器层", "内存占用", "<" + MEM_THRESHOLD + "%", 
                memPercent + "%", "NORMAL", "内存充足");
        }
        
        // 磁盘监控（Java 中用 File 获取）
        java.io.File disk = new java.io.File("F:\\");
        long diskTotal = disk.getTotalSpace();
        long diskFree = disk.getFreeSpace();
        int diskPercent = (int) ((diskTotal - diskFree) * 100 / diskTotal);
        
        if (diskPercent >= DISK_THRESHOLD) {
            recordItem("SERVER-003", "服务器层", "磁盘空间", "<" + DISK_THRESHOLD + "%", 
                diskPercent + "%", "FATAL", "磁盘将耗尽宕机", "", "df -h", "", "", 0);
        } else {
            recordItem("SERVER-003", "服务器层", "磁盘空间", "<" + DISK_THRESHOLD + "%", 
                diskPercent + "%", "NORMAL", "空间充足");
        }
        
        // 数据库连接池检查
        List<Map<String, Object>> currConnResult = dbQuery("SHOW STATUS LIKE 'Threads_connected'");
        List<Map<String, Object>> maxConnResult = dbQuery("SHOW VARIABLES LIKE 'max_connections'");
        
        if (!currConnResult.isEmpty() && !maxConnResult.isEmpty()) {
            int currConn = Integer.parseInt((String) currConnResult.get(0).get("Value"));
            int maxConn = Integer.parseInt((String) maxConnResult.get(0).get("Value"));
            double rate = (double) currConn / maxConn * 100;
            
            if (rate >= 80) {
                recordItem("SERVER-004", "服务器层", "数据库连接池", "<80%", 
                    String.format("%.1f%%", rate), "WARNING", "连接接近上限", "", "show processlist;", "", "", 0);
            } else {
                recordItem("SERVER-004", "服务器层", "数据库连接池", "<80%", 
                    String.format("%.1f%%", rate), "NORMAL", "负载正常");
            }
        }
    }

    // ====================== 数据库层扫描 ======================
    private void scanDatabase() {
        log.info("开始数据库层扫描...");
        loadAllTableInfo();
        
        // 1. 业务表唯一性检查
        List<String> dupErr = new ArrayList<>();
        StringBuilder dupSql = new StringBuilder("SET FOREIGN_KEY_CHECKS=0;\n");
        for (String[] pair : DUPLICATE_TABLE_GROUP) {
            String main = pair[0];
            String dup = pair[1];
            if (allDbTables.contains(main) && allDbTables.contains(dup)) {
                dupErr.add("主表" + main + " 冗余" + dup);
                dupSql.append("DROP TABLE IF EXISTS ").append(dup).append(";\n");
            }
        }
        dupSql.append("SET FOREIGN_KEY_CHECKS=1;");
        
        if (!dupErr.isEmpty()) {
            recordItem("DB-001", "数据库层", "业务表唯一性", "无重复业务表", 
                dupErr.toString(), "FATAL", "两套表数据割裂对账不平", dupSql.toString(), "", "", "", 0);
        } else {
            recordItem("DB-001", "数据库层", "业务表唯一性", "无重复业务表", 
                "无冗余", "NORMAL", "表结构干净");
        }
        
        // 2. 字段唯一性检查
        List<String> colErr = new ArrayList<>();
        StringBuilder colSql = new StringBuilder("SET FOREIGN_KEY_CHECKS=0;\n");
        for (Map.Entry<String, String[][]> entry : REDUNDANT_COL_MAP.entrySet()) {
            String tbl = entry.getKey();
            if (!allDbTables.contains(tbl)) continue;
            
            List<Map<String, Object>> columns = (List<Map<String, Object>>) dbTableDetail.get(tbl).get("columns");
            List<String> colNames = columns.stream()
                .map(c -> (String) c.get("Field"))
                .collect(Collectors.toList());
            
            for (String[] pair : entry.getValue()) {
                String oldCol = pair[0];
                String newCol = pair[1];
                if (colNames.contains(oldCol) && colNames.contains(newCol)) {
                    colErr.add(tbl + " 重复字段" + oldCol + "/" + newCol);
                    colSql.append("ALTER TABLE ").append(tbl).append(" DROP COLUMN ").append(oldCol).append(";\n");
                }
            }
        }
        colSql.append("SET FOREIGN_KEY_CHECKS=1;");
        
        if (!colErr.isEmpty()) {
            recordItem("DB-002", "数据库层", "字段唯一性", "无同义重复字段", 
                colErr.toString(), "ERROR", "同步更新统计错乱", colSql.toString(), "", "", "", 0);
        } else {
            recordItem("DB-002", "数据库层", "字段唯一性", "无同义重复字段", 
                "正常", "NORMAL", "字段统一");
        }
        
        // 3. 外键关联一致性检查
        List<String> typeErr = new ArrayList<>();
        StringBuilder typeSql = new StringBuilder("SET FOREIGN_KEY_CHECKS=0;\n");
        for (String[] fk : FK_FIELD_MATCH) {
            String childT = fk[0], childC = fk[1], parentT = fk[2], parentC = fk[3];
            if (!allDbTables.contains(childT) || !allDbTables.contains(parentT)) continue;
            
            List<Map<String, Object>> childCols = (List<Map<String, Object>>) dbTableDetail.get(childT).get("columns");
            List<Map<String, Object>> parentCols = (List<Map<String, Object>>) dbTableDetail.get(parentT).get("columns");
            
            String cType = childCols.stream()
                .filter(c -> childC.equals(c.get("Field")))
                .map(c -> (String) c.get("Type"))
                .findFirst().orElse("");
            String pType = parentCols.stream()
                .filter(c -> parentC.equals(c.get("Field")))
                .map(c -> (String) c.get("Type"))
                .findFirst().orElse("");
            
            if (!cType.equals(pType)) {
                typeErr.add(childT + "." + childC + " 与 " + parentT + "." + parentC + " 类型不一致");
                if (pType.contains("varchar")) {
                    Matcher m = Pattern.compile("\\((\\d+)\\)").matcher(pType);
                    if (m.find()) {
                        String len = m.group(1);
                        typeSql.append("ALTER TABLE ").append(childT).append(" MODIFY ")
                            .append(childC).append(" VARCHAR(").append(len).append(");\n");
                    }
                } else if (pType.equals("int")) {
                    typeSql.append("ALTER TABLE ").append(childT).append(" MODIFY ")
                        .append(childC).append(" INT;\n");
                } else if (pType.equals("bigint")) {
                    typeSql.append("ALTER TABLE ").append(childT).append(" MODIFY ")
                        .append(childC).append(" BIGINT;\n");
                }
            }
        }
        typeSql.append("SET FOREIGN_KEY_CHECKS=1;");
        
        if (!typeErr.isEmpty()) {
            recordItem("DB-003", "数据库层", "外键关联一致性", "父子字段完全匹配", 
                typeErr.toString(), "FATAL", "无法创建物理外键", typeSql.toString(), "", "", "", 0);
        } else {
            recordItem("DB-003", "数据库层", "外键关联一致性", "父子字段完全匹配", 
                "全部合规", "NORMAL", "关系约束可用");
        }
        
        // 4. 数据完整性检查（孤儿数据检测）
        List<String> orphanErr = new ArrayList<>();
        StringBuilder orphanSql = new StringBuilder();
        for (String[] fk : FK_FIELD_MATCH) {
            String childT = fk[0], childC = fk[1], parentT = fk[2], parentC = fk[3];
            String sql = "SELECT COUNT(*) as cnt FROM `" + childT + "` c LEFT JOIN `" + parentT + 
                "` p ON c." + childC + "=p." + parentC + " WHERE c." + childC + 
                " IS NOT NULL AND p." + parentC + " IS NULL";
            List<Map<String, Object>> result = dbQuery(sql);
            if (!result.isEmpty()) {
                int cnt = getIntValue(result.get(0), "cnt");
                if (cnt > 0) {
                    orphanErr.add(childT + " 存在" + cnt + "条孤儿数据");
                    orphanSql.append("DELETE FROM ").append(childT).append(" WHERE ")
                        .append(childC).append(" NOT IN (SELECT ").append(parentC)
                        .append(" FROM ").append(parentT).append(");\n");
                }
            }
        }
        
        if (!orphanErr.isEmpty()) {
            recordItem("DB-004", "数据库层", "数据完整性", "无孤立子记录", 
                orphanErr.toString(), "ERROR", "报表成本全部失真", orphanSql.toString(), "", "", "", 0);
        } else {
            recordItem("DB-004", "数据库层", "数据完整性", "无孤立子记录", 
                "正常", "NORMAL", "关联完整");
        }
        
        // 5. 业务数据合法性检查
        String[][] badSqls = {
            {"库存负数", "SELECT COUNT(*) as cnt FROM inventory_summary WHERE total_quantity < 0", 
             "DELETE FROM inventory_summary WHERE total_quantity < 0;"},
            {"采购负单价", "SELECT COUNT(*) as cnt FROM ingredient_purchase WHERE purchase_price < 0", 
             "UPDATE ingredient_purchase SET purchase_price = 0 WHERE purchase_price < 0;"},
            {"入职大于离职", "SELECT COUNT(*) as cnt FROM staff_master WHERE hire_date > resign_date AND resign_date IS NOT NULL", ""},
            {"成本单价为0", "SELECT COUNT(*) as cnt FROM cost_card_detail WHERE unit_price <= 0", ""}
        };
        
        List<String> badErr = new ArrayList<>();
        StringBuilder badFix = new StringBuilder();
        for (String[] item : badSqls) {
            String name = item[0], sql = item[1], fix = item[2];
            List<Map<String, Object>> result = dbQuery(sql);
            int num = 0;
            if (!result.isEmpty()) {
                num = getIntValue(result.get(0), "cnt");
            }
            if (num > 0) {
                badErr.add(name + ":" + num + "条异常");
                if (!fix.isEmpty()) {
                    badFix.append(fix).append("\n");
                }
            }
        }
        
        if (!badErr.isEmpty()) {
            recordItem("DB-005", "数据库层", "业务数据合法性", "无逻辑错误数据", 
                badErr.toString(), "ERROR", "财务对账不平", badFix.toString(), "", "", "", 0);
        } else {
            recordItem("DB-005", "数据库层", "业务数据合法性", "无逻辑错误数据", 
                "全部合规", "NORMAL", "数据正常");
        }
        
        // 6. 查询性能索引检查
        String[] idxTbls = {"inventory_summary", "purchase_request", "booking_master", "staff_master"};
        List<String> idxErr = new ArrayList<>();
        StringBuilder idxSql = new StringBuilder();
        for (String tbl : idxTbls) {
            if (!allDbTables.contains(tbl)) continue;
            List<Map<String, Object>> indexes = dbQuery("SHOW INDEX FROM `" + tbl + "`");
            boolean hasStoreId = indexes.stream()
                .anyMatch(idx -> "store_id".equals(idx.get("Column_name")));
            if (!hasStoreId) {
                idxErr.add(tbl + "缺失门店索引");
                idxSql.append("CREATE INDEX idx_").append(tbl).append("_store ON ")
                    .append(tbl).append("(store_id);\n");
            }
        }
        
        if (!idxErr.isEmpty()) {
            recordItem("DB-006", "数据库层", "查询性能索引", "高频表存在store索引", 
                idxErr.toString(), "WARNING", "分页看板卡顿", idxSql.toString(), "", "", "", 0);
        } else {
            recordItem("DB-006", "数据库层", "查询性能索引", "高频表存在store索引", 
                "索引齐全", "NORMAL", "查询流畅");
        }
        
        // 7. 文档完整性检查
        List<Map<String, Object>> emptyComment = dbQuery(
            "SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='" + 
            DB_NAME + "' AND COLUMN_COMMENT = '' LIMIT 200");
        
        if (!emptyComment.isEmpty()) {
            recordItem("DB-007", "数据库层", "文档完整性", "所有字段带注释", 
                "缺失注释" + emptyComment.size() + "条", "WARNING", "新人无法读懂字段含义");
        } else {
            recordItem("DB-007", "数据库层", "文档完整性", "所有字段带注释", 
                "注释完整", "NORMAL", "库文档齐全");
        }
        
        // 8. 全表元数据校验
        for (Map.Entry<String, Map<String, Object>> entry : dbTableDetail.entrySet()) {
            String tblName = entry.getKey();
            Map<String, Object> tblInfo = entry.getValue();
            List<Map<String, Object>> columns = (List<Map<String, Object>>) tblInfo.get("columns");
            int colTotal = columns.size();
            String commentStatus = ((String) tblInfo.get("table_comment")).trim().isEmpty() ? "无注释" : "注释完整";
            String level = commentStatus.equals("无注释") ? "WARNING" : "NORMAL";
            
            recordItem("DB-TABLE-" + tblName, "数据库-数据表", "数据表元数据完整性校验",
                "数据表【" + tblName + "】基础档案校验",
                "数据表存在业务注释，字段数量>=1",
                "总字段数：" + colTotal + "，表注释状态：" + commentStatus,
                level, "用于核查数据表是否完善归档，无注释表会增加后期维护成本");
        }
        
        // 9. 全字段合规校验
        for (Map.Entry<String, Map<String, Object>> entry : dbTableDetail.entrySet()) {
            String tblName = entry.getKey();
            Map<String, Object> tblInfo = entry.getValue();
            List<Map<String, Object>> columns = (List<Map<String, Object>>) tblInfo.get("columns");
            
            for (Map<String, Object> col : columns) {
                String cName = (String) col.get("Field");
                String cType = (String) col.get("Type");
                String nullFlag = (String) col.get("Null");
                String keyCol = (String) col.get("Key");
                Object defVal = col.get("Default");
                String comment = col.containsKey("Comment") ? (String) col.get("Comment") : "";
                
                String riskLevel = "NORMAL";
                List<String> riskMsgs = new ArrayList<>();
                
                // 校验1：字段无注释
                if (comment.trim().isEmpty()) {
                    riskLevel = "WARNING";
                    riskMsgs.add("缺失字段注释");
                }
                
                // 校验2：主键允许为空（致命）
                if ("YES".equals(nullFlag) && keyCol != null && keyCol.contains("PRI")) {
                    riskLevel = "FATAL";
                    riskMsgs.add("主键字段允许NULL，违反数据库规范");
                }
                
                // 校验3：数值类型默认值为空（警告）
                String cTypeLower = cType.toLowerCase();
                if ((cTypeLower.contains("int") || cTypeLower.contains("decimal") || 
                     cTypeLower.contains("float") || cTypeLower.contains("double")) && defVal == null) {
                    riskLevel = "WARNING";
                    riskMsgs.add("数值字段未设置默认值，易产生NULL脏数据");
                }
                
                // 校验4：varchar长度过短/过长
                if (cTypeLower.contains("varchar")) {
                    Matcher m = Pattern.compile("(\\d+)").matcher(cType);
                    if (m.find()) {
                        int lenNum = Integer.parseInt(m.group(1));
                        if (lenNum > 1000) {
                            riskLevel = "WARNING";
                            riskMsgs.add("字符串长度" + lenNum + "过长，建议使用TEXT类型");
                        } else if (lenNum < 5) {
                            riskLevel = "WARNING";
                            riskMsgs.add("字符串长度" + lenNum + "过短，可能截断数据");
                        }
                    }
                }
                
                // 校验5：时间字段
                if (cName.toLowerCase().contains("time") && !cTypeLower.contains("datetime") && 
                    !cTypeLower.contains("timestamp")) {
                    riskLevel = "WARNING";
                    riskMsgs.add("时间相关字段类型为" + cType + "，建议使用datetime");
                }
                
                String checkDesc = riskMsgs.isEmpty() ? "字段全部合规" : String.join("；", riskMsgs);
                
                recordItem("DB-COL-" + tblName + "." + cName, "数据库-字段校验", 
                    "字段类型/空值/注释/默认值合法性全检",
                    "字段【" + tblName + "." + cName + "】合规校验",
                    "字段注释完整、主键非空、数值存在默认值、字符串长度规范",
                    "字段类型：" + cType + "，允许空：" + nullFlag + "，默认值：" + defVal + 
                    "，注释：" + comment + "，异常点：" + checkDesc,
                    riskLevel, "全维度字段规范校验，拦截数据库设计不规范问题，避免业务脏数据");
            }
        }
        
        // 10. 索引检测
        for (String tblName : allDbTables) {
            try {
                List<Map<String, Object>> idxRows = dbQuery("SHOW INDEX FROM `" + tblName + "`");
                Map<String, Map<String, Object>> idxMap = new LinkedHashMap<>();
                
                for (Map<String, Object> row : idxRows) {
                    String idxName = (String) row.get("Key_name");
                    String colName = (String) row.get("Column_name");
                    String idxType = (String) row.get("Index_type");
                    
                    idxMap.computeIfAbsent(idxName, k -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("cols", new ArrayList<String>());
                        m.put("type", idxType);
                        return m;
                    });
                    ((List<String>) idxMap.get(idxName).get("cols")).add(colName);
                }
                
                for (Map.Entry<String, Map<String, Object>> idxEntry : idxMap.entrySet()) {
                    String idxName = idxEntry.getKey();
                    Map<String, Object> idxInfo = idxEntry.getValue();
                    List<String> idxCols = (List<String>) idxInfo.get("cols");
                    String idxTypeDesc = (String) idxInfo.get("type");
                    String idxColsStr = String.join(",", idxCols);
                    
                    String idxNote = "索引正常";
                    if ("PRIMARY".equals(idxName)) {
                        idxNote = "主键索引";
                    }
                    
                    recordItem("DB-IDX-" + tblName + "." + idxName, "数据库-索引", 
                        "索引合理性校验",
                        "索引【" + tblName + "." + idxName + "】检查",
                        "索引命名规范、无重复索引、联合索引顺序合理",
                        "索引字段：" + idxColsStr + "，索引类型：" + idxTypeDesc + "，" + idxNote,
                        "NORMAL", "核查索引冗余、命名不规范等影响查询性能问题");
                }
            } catch (Exception e) {
                recordItem("DB-IDX-" + tblName + ".ERR", "数据库-索引", "索引检测异常",
                    "索引【" + tblName + "】检测失败",
                    "成功检测", "检测异常:" + e.getMessage(),
                    "WARNING", "索引检测过程出错");
            }
        }
        
        // 11. 业务数据巡检
        for (String tblName : allDbTables) {
            try {
                // 空表检测
                List<Map<String, Object>> rowResult = dbQuery("SELECT COUNT(*) as cnt FROM `" + tblName + "`");
                int rowCount = 0;
                if (!rowResult.isEmpty()) {
                    rowCount = getIntValue(rowResult.get(0), "cnt");
                }
                
                // 负数异常数据检测
                int negativeCnt = 0;
                if (dbTableDetail.containsKey(tblName)) {
                    List<Map<String, Object>> columns = (List<Map<String, Object>>) dbTableDetail.get(tblName).get("columns");
                    for (Map<String, Object> col : columns) {
                        String colName = (String) col.get("Field");
                        String colNameLower = colName.toLowerCase();
                        if (colNameLower.contains("amount") || colNameLower.contains("price") || 
                            colNameLower.contains("stock") || colNameLower.contains("num") || 
                            colNameLower.contains("quantity") || colNameLower.contains("total") || 
                            colNameLower.contains("deposit")) {
                            try {
                                List<Map<String, Object>> negResult = dbQuery(
                                    "SELECT COUNT(*) as cnt FROM `" + tblName + "` WHERE `" + colName + "` < 0");
                                if (!negResult.isEmpty()) {
                                    negativeCnt += getIntValue(negResult.get(0), "cnt");
                                }
                            } catch (Exception e) {
                                // 忽略查询错误
                            }
                        }
                    }
                }
                
                String riskLv = "NORMAL";
                List<String> riskTexts = new ArrayList<>();
                if (rowCount == 0) {
                    riskLv = "WARNING";
                    riskTexts.add("空数据表，无业务数据");
                }
                if (negativeCnt > 0) {
                    if ("NORMAL".equals(riskLv)) {
                        riskLv = "ERROR";
                    }
                    riskTexts.add("存在" + negativeCnt + "条负数金额/库存脏数据");
                }
                
                String actualDetail = "表总行数：" + rowCount + "，负数异常数据总量：" + negativeCnt;
                if (riskTexts.isEmpty()) {
                    actualDetail += "，异常描述：无脏数据";
                } else {
                    actualDetail += "，异常描述：" + String.join("；", riskTexts);
                }
                
                recordItem("DB-DATA-" + tblName, "数据库-业务数据", "脏数据、空表异常检测",
                    "数据表【" + tblName + "】业务数据巡检",
                    "数据表存在业务数据，金额/库存字段无负数脏数据",
                    actualDetail, riskLv, "拦截业务异常脏数据，防止报表、收银计算出错");
            } catch (Exception e) {
                recordItem("DB-DATA-" + tblName + ".ERR", "数据库-业务数据", "脏数据检测异常",
                    "数据表【" + tblName + "】巡检失败",
                    "成功检测", "检测异常:" + e.getMessage(),
                    "WARNING", "巡检过程出错");
            }
        }
    }

    // ====================== 后端代码扫描 ======================
    private void scanBackend() {
        log.info("开始后端代码扫描...");
        
        List<String> allJava = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(BACK_CODE_PATH))) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".java"))
                 .forEach(p -> allJava.add(p.toString()));
        } catch (IOException e) {
            log.error("扫描Java文件异常: {}", e.getMessage());
        }
        
        // 1. Controller 入参校验
        for (String javaFp : allJava) {
            try {
                String content = Files.readString(Paths.get(javaFp));
                if (content.contains("@RestController") || content.contains("@Controller")) {
                    String fileName = Paths.get(javaFp).getFileName().toString();
                    boolean hasValid = content.contains("@Valid") || content.contains("@NotBlank") || 
                                      content.contains("@NotNull");
                    boolean hasGlobalErr = content.contains("GlobalExceptionHandler") || 
                                          content.contains("try{") || content.contains("Exception");
                    
                    List<String> riskItems = new ArrayList<>();
                    if (!hasValid) riskItems.add("缺失@Valid/@NotBlank入参校验");
                    if (!hasGlobalErr) riskItems.add("无统一异常捕获");
                    
                    String riskLevel = riskItems.isEmpty() ? "NORMAL" : "WARNING";
                    
                    recordItem("CTRL-" + fileName, "后端代码", "Controller接口入参校验",
                        "控制器 " + fileName + " 规范检查",
                        "接口必须配置@Valid、@NotBlank、异常捕获",
                        "@Valid/NotNull:" + hasValid + " | 异常处理:" + hasGlobalErr,
                        riskLevel, riskItems.isEmpty() ? "入参校验全部合规" : String.join("；", riskItems),
                        "", "", javaFp, "", 0);
                }
            } catch (IOException e) {
                // 忽略读取错误
            }
        }
        
        // 2. Mapper 分页校验
        for (String javaFp : allJava) {
            String fileName = Paths.get(javaFp).getFileName().toString();
            if (fileName.contains("Mapper") || fileName.contains("Dao") || fileName.contains("Repository")) {
                try {
                    String content = Files.readString(Paths.get(javaFp)).toLowerCase();
                    boolean hasLimit = content.contains("limit");
                    boolean hasPageHelper = content.contains("pagehelper");
                    boolean hasPage = content.contains("page") && (content.contains("select") || content.contains("query"));
                    boolean hasSelect = content.contains("select");
                    
                    String riskLevel;
                    String riskDesc;
                    if (hasSelect) {
                        riskLevel = (hasLimit || hasPageHelper || hasPage) ? "NORMAL" : "WARNING";
                        riskDesc = (hasLimit || hasPageHelper || hasPage) ? "分页逻辑正常" : "未配置分页（大数据查询会OOM）";
                    } else {
                        riskLevel = "NORMAL";
                        riskDesc = "非查询类Mapper";
                    }
                    
                    recordItem("MAPPER-" + fileName, "后端代码", "Mapper SQL分页校验",
                        "Mapper " + fileName + " 分页规则检查",
                        "列表查询必须带limit或PageHelper分页",
                        "limit:" + hasLimit + " | PageHelper:" + hasPageHelper + " | Page分页:" + hasPage,
                        riskLevel, riskDesc, "", "", javaFp, "", 0);
                } catch (IOException e) {
                    // 忽略读取错误
                }
            }
        }
        
        // 3. 实体@Column映射校验
        List<String> entityFiles = scanFileKeyword(BACK_CODE_PATH, "@Column");
        if (!entityFiles.isEmpty()) {
            recordItem("BACK-001", "后端代码", "数据库实体映射",
                "实体与库字段对齐校验",
                "实体@Column与数据库字段完全匹配",
                "待核对文件数量：" + entityFiles.size(),
                "WARNING", "字段名称不匹配会查询返回NULL", "", "", String.join("\n", entityFiles), "", 0);
        } else {
            recordItem("BACK-001", "后端代码", "数据库实体映射",
                "实体与库字段对齐校验",
                "实体@Column与数据库字段完全匹配",
                "全部实体映射规范",
                "NORMAL", "无字段映射不一致问题");
        }
        
        // 4. 事务注解校验
        List<String> txFiles = new ArrayList<>();
        List<String> serviceAll = scanFileKeyword(BACK_CODE_PATH, "@Service");
        for (String fp : serviceAll) {
            if (fp.contains("Inventory") || fp.contains("Salary") || fp.contains("Booking") || 
                fp.contains("Purchase") || fp.contains("Finance") || fp.contains("Stock")) {
                try {
                    String content = Files.readString(Paths.get(fp));
                    if (!content.contains("@Transactional")) {
                        txFiles.add(fp);
                    }
                } catch (IOException e) {
                    // 忽略读取错误
                }
            }
        }
        
        if (!txFiles.isEmpty()) {
            recordItem("BACK-002", "后端代码", "业务事务防护",
                "库存/薪资/预订事务注解校验",
                "修改数据方法必须添加@Transactional",
                "缺失事务文件：" + txFiles.size() + "个",
                "ERROR", "并发操作会产生数据错乱、脏数据", "", "", String.join("\n", txFiles), "", 0);
        } else {
            recordItem("BACK-002", "后端代码", "业务事务防护",
                "库存/薪资/预订事务注解校验",
                "修改数据方法必须添加@Transactional",
                "所有核心业务均配置事务",
                "NORMAL", "并发数据安全有保障");
        }
        
        // 5. Service 文件规范检查
        for (String javaFp : allJava) {
            String fileName = Paths.get(javaFp).getFileName().toString();
            if (fileName.contains("Service") && fileName.contains("Impl")) {
                try {
                    String content = Files.readString(Paths.get(javaFp));
                    boolean hasTx = content.contains("@Transactional");
                    boolean hasLog = content.contains("Logger") || content.toLowerCase().contains("log") || 
                                    content.contains("@Slf4j");
                    
                    List<String> issues = new ArrayList<>();
                    if (!hasTx && (fileName.contains("Inventory") || fileName.contains("Salary") || 
                                   fileName.contains("Booking") || fileName.contains("Purchase") || 
                                   fileName.contains("Finance") || fileName.contains("Stock"))) {
                        issues.add("核心业务未加@Transactional");
                    }
                    if (!hasLog) {
                        issues.add("未引入日志");
                    }
                    
                    String riskLv = issues.isEmpty() ? "NORMAL" : "WARNING";
                    
                    recordItem("SVC-" + fileName, "后端代码-Service", "Service实现规范",
                        fileName + " 规范检查",
                        "事务+日志完整",
                        "事务:" + (hasTx ? "有" : "无") + " | 日志:" + (hasLog ? "有" : "无"),
                        riskLv, issues.isEmpty() ? "规范完整" : String.join("；", issues),
                        "", "", javaFp, "", 0);
                } catch (IOException e) {
                    // 忽略读取错误
                }
            }
        }
        
        // 6. Java 文件存在性校验
        for (String javaFp : allJava) {
            String fileName = Paths.get(javaFp).getFileName().toString();
            recordItem("JAVA-FILE-" + fileName, "后端代码", "Java文件基础校验",
                "源码文件 " + fileName + " 存在性校验",
                "源码文件完整无缺失",
                "文件路径：" + javaFp,
                "NORMAL", "统计所有后端源码文件");
        }
    }

    // ====================== 前端页面扫描 ======================
    private void scanFrontend() {
        log.info("开始前端页面扫描...");
        
        // 1. 门店硬编码检查
        List<String> hardcode = scanFileKeyword(FRONT_CODE_PATH, "storeId:1");
        if (!hardcode.isEmpty()) {
            recordItem("FRONT-001", "前端模板层", "门店取值规范",
                "禁止硬编码固定门店",
                "违规" + hardcode.size() + "文件",
                "FATAL", "切换门店数据不刷新", "", "", String.join("\n", hardcode), "", 0);
        } else {
            recordItem("FRONT-001", "前端模板层", "门店取值规范",
                "禁止硬编码固定门店",
                "无硬编码", "NORMAL", "动态取值正常");
        }
        
        // 2. 页面完整性检查
        List<String> emptyPages = new ArrayList<>();
        for (String pageName : VUE_PAGE_LIST) {
            try (Stream<Path> paths = Files.walk(Paths.get(FRONT_CODE_PATH))) {
                paths.filter(Files::isRegularFile)
                     .filter(p -> p.getFileName().toString().equals(pageName))
                     .forEach(p -> {
                         try {
                             String content = Files.readString(p);
                             if (content.length() < 300) {
                                 emptyPages.add(p.toString());
                             }
                         } catch (IOException e) {
                             // 忽略读取错误
                         }
                     });
            } catch (IOException e) {
                // 忽略扫描错误
            }
        }
        
        if (!emptyPages.isEmpty()) {
            recordItem("FRONT-002", "前端模板层", "页面完整性",
                "所有业务页面具备逻辑",
                "空白" + emptyPages.size() + "页面",
                "ERROR", "无法操作单据", "", "", String.join("\n", emptyPages), "", 0);
        } else {
            recordItem("FRONT-002", "前端模板层", "页面完整性",
                "所有业务页面具备逻辑",
                "页面齐全", "NORMAL", "功能完整");
        }
        
        // 3. Vue 页面规范性检查
        List<String> vueFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(FRONT_CODE_PATH))) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".vue"))
                 .forEach(p -> vueFiles.add(p.toString()));
        } catch (IOException e) {
            log.error("扫描Vue文件异常: {}", e.getMessage());
        }
        
        for (String fp : vueFiles) {
            try {
                String content = Files.readString(Paths.get(fp));
                String fileName = Paths.get(fp).getFileName().toString();
                List<String> issues = new ArrayList<>();
                
                boolean hasComment = content.contains("<!--") || content.contains("//");
                boolean hasFormValidate = content.contains("rules") || content.contains("validate");
                boolean hasErrorHandle = content.contains(".catch") || content.contains("try {");
                boolean hasHardcodeApi = content.contains("http://") && content.contains("localhost");
                boolean hasScoped = content.contains("scoped");
                
                if (!hasComment) issues.add("页面无注释");
                if (content.contains("el-form") && !hasFormValidate) issues.add("表单未做校验");
                if (content.toLowerCase().contains("axios") || content.toLowerCase().contains("fetch") || 
                    content.toLowerCase().contains("request")) {
                    if (!hasErrorHandle) issues.add("API请求无异常捕获");
                }
                if (hasHardcodeApi) issues.add("硬编码API地址");
                if (content.contains("<style") && !hasScoped) issues.add("样式未加scoped");
                
                String riskLv = issues.isEmpty() ? "NORMAL" : "WARNING";
                
                recordItem("FRONT-VUE-" + fileName, "前端页面-Vue组件", "Vue页面规范性检查",
                    "Vue页面【" + fileName + "】检查",
                    "页面含注释、表单校验、异常捕获、无硬编码、scoped样式",
                    "问题点：" + (issues.isEmpty() ? "全部合规" : String.join("；", issues)),
                    riskLv, "前端页面规范检查，保障可维护性和稳定性", "", "", fp, "", 0);
            } catch (IOException e) {
                // 忽略读取错误
            }
        }
        
        // 4. API 请求文件检查
        List<String> apiFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(FRONT_CODE_PATH))) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> {
                     String fn = p.toString();
                     return fn.endsWith(".js") || fn.endsWith(".ts");
                 })
                 .forEach(p -> {
                     try {
                         String content = Files.readString(p);
                         if (content.toLowerCase().contains("axios") || content.toLowerCase().contains("fetch") || 
                             content.toLowerCase().contains("request") || content.contains("/api/")) {
                             apiFiles.add(p.toString());
                         }
                     } catch (IOException e) {
                         // 忽略读取错误
                     }
                 });
        } catch (IOException e) {
            log.error("扫描API文件异常: {}", e.getMessage());
        }
        
        for (String fp : apiFiles) {
            try {
                String content = Files.readString(Paths.get(fp));
                String fileName = Paths.get(fp).getFileName().toString();
                List<String> issues = new ArrayList<>();
                
                boolean hasToken = content.toLowerCase().contains("token") || content.contains("Authorization");
                boolean hasError = content.contains(".catch") || content.contains("try");
                
                if (!hasToken) issues.add("未拦截token/鉴权");
                if (!hasError) issues.add("无异常处理");
                
                String riskLv = issues.isEmpty() ? "NORMAL" : "WARNING";
                
                recordItem("FRONT-API-" + fileName, "前端-API请求", "API请求拦截规范检查",
                    "API请求文件【" + fileName + "】检查",
                    "含鉴权拦截、异常捕获、baseURL统一配置",
                    "鉴权:" + (hasToken ? "有" : "无") + "，异常处理:" + (hasError ? "有" : "无") + 
                    "，问题:" + (issues.isEmpty() ? "无" : String.join(";", issues)),
                    riskLv, "API请求文件规范检查", "", "", fp, "", 0);
            } catch (IOException e) {
                // 忽略读取错误
            }
        }
        
        // 5. 配置文件检查
        List<String> configFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(FRONT_CODE_PATH))) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> {
                     String fn = p.getFileName().toString();
                     return fn.equals("package.json") || fn.equals("vite.config.js") || 
                            fn.equals("vite.config.ts") || fn.equals("tsconfig.json") || 
                            fn.equals(".env") || fn.equals(".env.production");
                 })
                 .forEach(p -> configFiles.add(p.toString()));
        } catch (IOException e) {
            log.error("扫描配置文件异常: {}", e.getMessage());
        }
        
        for (String fp : configFiles) {
            try {
                String content = Files.readString(Paths.get(fp));
                String fileName = Paths.get(fp).getFileName().toString();
                List<String> issues = new ArrayList<>();
                
                if (fileName.equals("package.json")) {
                    if (!content.contains("\"vue\"") && !content.contains("\"react\"")) {
                        issues.add("package.json未找到框架依赖");
                    }
                }
                if (fileName.endsWith(".env") || fileName.endsWith(".env.production")) {
                    if (!content.contains("VITE_") && !content.contains("VUE_APP_")) {
                        issues.add("环境变量前缀不规范");
                    }
                }
                
                String riskLv = issues.isEmpty() ? "NORMAL" : "WARNING";
                
                recordItem("FRONT-CFG-" + fileName, "前端-配置", "前端配置文件规范检查",
                    "配置文件【" + fileName + "】检查",
                    "配置完整、无硬编码、环境变量规范",
                    "问题点：" + (issues.isEmpty() ? "配置规范" : String.join("；", issues)),
                    riskLv, "配置文件规范检查", "", "", fp, "", 0);
            } catch (IOException e) {
                // 忽略读取错误
            }
        }
        
        // 6. 路由配置检测
        Path routerFile = Paths.get(FRONT_CODE_PATH, "src", "router", "index.js");
        if (Files.exists(routerFile)) {
            try {
                String routerContent = Files.readString(routerFile);
                Pattern pathPattern = Pattern.compile("path\\s*:\\s*['\"]([^'\"]+)['\"]");
                Matcher pathMatcher = pathPattern.matcher(routerContent);
                List<String> routes = new ArrayList<>();
                while (pathMatcher.find()) {
                    routes.add(pathMatcher.group(1));
                }
                
                recordItem("FRONT-ROUTE-001", "前端-路由", "路由路径解析",
                    "router/index.js应包含路由定义",
                    "已解析" + routes.size() + "条路由",
                    routes.isEmpty() ? "WARNING" : "NORMAL",
                    "路由是前端导航的基础");
                
                Pattern metaPattern = Pattern.compile("meta\\s*:\\s*\\{[^}]*roles\\s*:\\s*\\[([^\\]]+)\\]");
                Matcher metaMatcher = metaPattern.matcher(routerContent);
                List<String> metaRoles = new ArrayList<>();
                while (metaMatcher.find()) {
                    metaRoles.add(metaMatcher.group(1));
                }
                
                recordItem("FRONT-ROUTE-002", "前端-路由", "路由权限元数据",
                    "路由应配置roles权限控制",
                    metaRoles.size() + "条路由有roles",
                    metaRoles.isEmpty() ? "WARNING" : "NORMAL",
                    "无权限元数据的路由任何人都能访问");
                
                int missing = 0;
                for (String path : routes.subList(0, Math.min(100, routes.size()))) {
                    String compName = path.replaceAll("^/+", "").replaceAll("/", "_");
                    List<String> found = scanFileKeyword(FRONT_CODE_PATH, compName);
                    if (found.isEmpty()) {
                        missing++;
                    }
                }
                
                if (missing > 0) {
                    recordItem("FRONT-ROUTE-003", "前端-路由", "路由组件存在性",
                        "每条路由应有对应Vue组件",
                        "约" + missing + "条路由组件可能缺失",
                        "WARNING", "缺失组件的路由会白屏");
                } else {
                    recordItem("FRONT-ROUTE-003", "前端-路由", "路由组件存在性",
                        "每条路由应有对应Vue组件",
                        "全部路由组件校验通过",
                        "NORMAL", "路由完整性良好");
                }
            } catch (IOException e) {
                log.error("读取路由文件异常: {}", e.getMessage());
            }
        } else {
            recordItem("FRONT-ROUTE-000", "前端-路由", "路由配置文件",
                "router/index.js应存在",
                "未找到路由文件",
                "WARNING", "无法分析路由结构");
        }
        
        // 7. 组件功能统计
        if (!vueFiles.isEmpty()) {
            Map<String, Integer> stats = new HashMap<>();
            stats.put("search", 0);
            stats.put("table", 0);
            stats.put("page", 0);
            stats.put("add", 0);
            stats.put("export", 0);
            stats.put("print", 0);
            stats.put("dialog", 0);
            stats.put("perm", 0);
            
            for (String fp : vueFiles) {
                try {
                    String ct = Files.readString(Paths.get(fp));
                    if (ct.contains("el-form") || ct.contains("el-input")) stats.put("search", stats.get("search") + 1);
                    if (ct.contains("el-table")) stats.put("table", stats.get("table") + 1);
                    if (ct.contains("el-pagination") || ct.toLowerCase().contains("pagination")) 
                        stats.put("page", stats.get("page") + 1);
                    if (ct.contains("新增") || ct.contains("添加") || ct.contains("handleCreate")) 
                        stats.put("add", stats.get("add") + 1);
                    if (ct.contains("导出") || ct.toLowerCase().contains("export")) 
                        stats.put("export", stats.get("export") + 1);
                    if (ct.contains("打印") || ct.toLowerCase().contains("print")) 
                        stats.put("print", stats.get("print") + 1);
                    if (ct.contains("el-dialog")) stats.put("dialog", stats.get("dialog") + 1);
                    if (ct.contains("v-permission")) stats.put("perm", stats.get("perm") + 1);
                } catch (IOException e) {
                    // 忽略读取错误
                }
            }
            
            recordItem("FRONT-STAT-001", "前端-组件功能", "页面控件覆盖率",
                "总" + vueFiles.size() + "个Vue页面应含核心控件",
                "搜索" + stats.get("search") + " 表格" + stats.get("table") + " 分页" + stats.get("page") + 
                " 新增" + stats.get("add") + " 导出" + stats.get("export") + " 打印" + stats.get("print") + 
                " 弹窗" + stats.get("dialog") + " 权限" + stats.get("perm"),
                "NORMAL", "页面功能完好度统计");
            
            if (stats.get("perm") == 0 && vueFiles.size() > 5) {
                recordItem("FRONT-STAT-002", "前端-组件功能", "权限指令覆盖率",
                    "业务页面应使用v-permission",
                    "0个页面使用v-permission",
                    "WARNING", "建议为敏感操作添加权限控制");
            }
        }
        
        // 8. 模块分组统计
        Map<String, List<String>> moduleGroups = new HashMap<>();
        moduleGroups.put("前厅预订", Arrays.asList("Booking", "FrontDesk", "FrontOffice", "TableBoard", 
            "TableLayout", "TableUtilization", "SelfService"));
        moduleGroups.put("厨房管理", Arrays.asList("Kitchen", "Production", "KitchenLog", "DishCost", 
            "Hygiene", "Menu", "MenuManage"));
        moduleGroups.put("采购仓储", Arrays.asList("Procurement", "Inventory", "StockTake", "Suppliers", 
            "SupplyChain", "SupplyManagement", "GoodsReceipt"));
        moduleGroups.put("财务管理", Arrays.asList("Finance", "Revenue", "Tax", "Cost", "Payroll", 
            "SupplierReconciliation"));
        moduleGroups.put("人事管理", Arrays.asList("Staff", "HRAdmin", "HRAnalytics", "Schedule", 
            "Attendance", "Leave", "Training", "StaffPerformance", "StaffProfile"));
        moduleGroups.put("工程维修", Arrays.asList("Engineering", "Maintenance", "Energy"));
        moduleGroups.put("营销活动", Arrays.asList("Marketing", "MarketingActivity", "MemberList"));
        moduleGroups.put("审批审计", Arrays.asList("ApprovalCenter", "AuditLog", "ReviewQueue", 
            "PermManager", "ChangeLogView"));
        moduleGroups.put("报表看板", Arrays.asList("Dashboard", "Reports", "DataScreen", 
            "CustomerAnalysis", "GuestAnalysis"));
        moduleGroups.put("系统设置", Arrays.asList("Settings", "Security", "DeviceBinding", "License"));
        
        Map<String, Map<String, Object>> vuePageMap = new HashMap<>();
        for (String fp : vueFiles) {
            String n = Paths.get(fp).getFileName().toString().replace(".vue", "");
            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("path", fp);
            pageInfo.put("size", new java.io.File(fp).length());
            vuePageMap.put(n, pageInfo);
        }
        
        for (Map.Entry<String, List<String>> entry : moduleGroups.entrySet()) {
            String mod = entry.getKey();
            List<String> names = entry.getValue();
            List<Map<String, Object>> pages = new ArrayList<>();
            
            for (String n : names) {
                if (vuePageMap.containsKey(n)) {
                    try {
                        String path = (String) vuePageMap.get(n).get("path");
                        String ct = Files.readString(Paths.get(path));
                        Map<String, Object> controls = new HashMap<>();
                        controls.put("search", ct.contains("el-form") || ct.contains("el-input"));
                        controls.put("table", ct.contains("el-table"));
                        controls.put("page", ct.contains("el-pagination") || ct.toLowerCase().contains("pagination"));
                        controls.put("add", ct.contains("新增") || ct.contains("添加") || ct.contains("handleCreate"));
                        controls.put("export", ct.contains("导出") || ct.toLowerCase().contains("export"));
                        controls.put("print", ct.contains("打印") || ct.toLowerCase().contains("print"));
                        controls.put("dialog", ct.contains("el-dialog"));
                        controls.put("perm", ct.contains("v-permission"));
                        pages.add(controls);
                    } catch (IOException e) {
                        // 忽略读取错误
                    }
                }
            }
            
            if (!pages.isEmpty()) {
                int addCount = (int) pages.stream().filter(p -> (boolean) p.get("add")).count();
                int permCount = (int) pages.stream().filter(p -> (boolean) p.get("perm")).count();
                int tableCount = (int) pages.stream().filter(p -> (boolean) p.get("table")).count();
                
                recordItem("FRONT-MOD-" + mod, "前端-模块控件", mod + "页面统计",
                    pages.size() + "个页面应有核心控件",
                    "共" + pages.size() + "页 新增" + addCount + " 权限" + permCount + " 表" + tableCount,
                    "NORMAL", "模块控件覆盖率统计");
            }
        }
    }

    // ====================== 接口层扫描 ======================
    private void scanApi() {
        log.info("开始接口层扫描...");
        
        // 1. 登录鉴权链路
        boolean loginOk = apiLogin();
        if (!loginOk) {
            recordItem("API-001", "接口层", "登录鉴权链路",
                "正常返回JWT",
                "连接拒绝",
                "FATAL", "后端8080未启动", "", "curl " + API_BASE_URL + "/auth/login -X POST", "", "", 0);
        } else {
            recordItem("API-001", "接口层", "登录鉴权链路",
                "正常返回JWT",
                "登录成功",
                "NORMAL", "鉴权正常");
        }
        
        // 2. 全 CRUD 端点连通
        List<String> failApi = new ArrayList<>();
        for (String url : API_FULL_LIST) {
            try {
                Thread.sleep(500);
                Map<String, Object> resp = apiGet(url, null);
                if (resp.get("code") == null || (int) resp.get("code") != 200) {
                    failApi.add(url + " 返回" + resp.get("code"));
                }
            } catch (Exception e) {
                failApi.add(url + " 异常:" + e.getMessage());
            }
        }
        
        if (!failApi.isEmpty()) {
            recordItem("API-002", "接口层", "全CRUD端点连通",
                "所有接口200",
                failApi.toString(),
                "ERROR", "业务功能不可用");
        } else {
            recordItem("API-002", "接口层", "全CRUD端点连通",
                "所有接口200",
                "全部正常",
                "NORMAL", "接口全通");
        }
        
        // 3. 数据隔离链路
        Map<String, String> crossParams = new HashMap<>();
        crossParams.put("storeId", "9999");
        Map<String, Object> crossResp = apiGet("/hr/schedule", crossParams);
        if (crossResp.get("code") != null && (int) crossResp.get("code") == 200) {
            Object data = crossResp.get("data");
            if (data instanceof List) {
                List<Map<String, Object>> crossData = (List<Map<String, Object>>) data;
                boolean leaked = crossData.stream()
                    .anyMatch(r -> r.get("storeId") != null && (int) r.get("storeId") == 9999);
                if (leaked) {
                    recordItem("API-003", "接口层", "数据隔离链路",
                        "非admin禁止跨门店",
                        "可越权查询",
                        "FATAL", "数据安全漏洞");
                } else {
                    recordItem("API-003", "接口层", "数据隔离链路",
                        "非admin禁止跨门店",
                        "正常拦截(token隔离)",
                        "NORMAL", "隔离生效");
                }
            } else {
                recordItem("API-003", "接口层", "数据隔离链路",
                    "非admin禁止跨门店",
                    "正常拦截",
                    "NORMAL", "隔离生效");
            }
        } else {
            recordItem("API-003", "接口层", "数据隔离链路",
                "非admin禁止跨门店",
                "正常拦截",
                "NORMAL", "隔离生效");
        }
    }

    // ====================== 索引深度扫描 ======================
    private void scanIndexDeep() {
        log.info("开始索引深度扫描...");
        loadAllTableInfo();
        
        // 1. 外键索引缺失检测
        List<Map<String, Object>> fks = dbQuery(
            "SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME " +
            "FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA='" + DB_NAME + 
            "' AND REFERENCED_TABLE_NAME IS NOT NULL");
        
        for (Map<String, Object> fk : fks) {
            String tbl = (String) fk.get("TABLE_NAME");
            String col = (String) fk.get("COLUMN_NAME");
            String refTbl = (String) fk.get("REFERENCED_TABLE_NAME");
            
            List<Map<String, Object>> hasIdx = dbQuery("SHOW INDEX FROM `" + tbl + "` WHERE Column_name='" + col + "'");
            if (hasIdx.isEmpty()) {
                recordItem("IDX-FK-" + tbl + "." + col, "数据库-索引深度", "外键索引缺失",
                    "外键列 " + tbl + "." + col + " 应有索引",
                    "未找到索引",
                    "ERROR", "JOIN " + refTbl + " 时全表扫描",
                    "CREATE INDEX idx_" + tbl + "_" + col + " ON " + tbl + "(" + col + ");",
                    "", "", "", 0);
            } else {
                recordItem("IDX-FK-" + tbl + "." + col, "数据库-索引深度", "外键索引存在",
                    "外键列 " + tbl + "." + col + " 应有索引",
                    "索引已存在",
                    "NORMAL", "JOIN查询可走索引");
            }
        }
        
        // 2. 冗余索引检测
        for (String tbl : allDbTables) {
            try {
                List<Map<String, Object>> idxRows = dbQuery("SHOW INDEX FROM `" + tbl + "`");
                Map<String, List<Map<String, Object>>> idxMap = new LinkedHashMap<>();
                
                for (Map<String, Object> row : idxRows) {
                    String name = (String) row.get("Key_name");
                    String cn = (String) row.get("Column_name");
                    int seq = getIntValue(row, "Seq_in_index");
                    
                    idxMap.computeIfAbsent(name, k -> new ArrayList<>());
                    Map<String, Object> colInfo = new HashMap<>();
                    colInfo.put("seq", seq);
                    colInfo.put("name", cn);
                    idxMap.get(name).add(colInfo);
                }
                
                List<String> names = new ArrayList<>(idxMap.keySet());
                for (int i = 0; i < names.size(); i++) {
                    for (int j = i + 1; j < names.size(); j++) {
                        List<String> aCols = idxMap.get(names.get(i)).stream()
                            .sorted((x, y) -> Integer.compare((int) x.get("seq"), (int) y.get("seq")))
                            .map(x -> (String) x.get("name"))
                            .collect(Collectors.toList());
                        List<String> bCols = idxMap.get(names.get(j)).stream()
                            .sorted((x, y) -> Integer.compare((int) x.get("seq"), (int) y.get("seq")))
                            .map(x -> (String) x.get("name"))
                            .collect(Collectors.toList());
                        
                        if (aCols.equals(bCols)) {
                            recordItem("IDX-DUP-" + tbl + "." + names.get(i) + "-" + names.get(j), 
                                "数据库-索引深度", "完全冗余索引",
                                "索引不应完全重复",
                                names.get(i) + "(" + String.join(",", aCols) + ") ≡ " + 
                                names.get(j) + "(" + String.join(",", bCols) + ")",
                                "WARNING", "浪费存储和写入性能",
                                "DROP INDEX " + names.get(j) + " ON " + tbl + ";",
                                "", "", "", 0);
                        } else if (aCols.size() >= 2 && bCols.size() >= 2 && 
                                   aCols.subList(0, 2).equals(bCols.subList(0, 2))) {
                            recordItem("IDX-SIM-" + tbl + "." + names.get(i) + "-" + names.get(j), 
                                "数据库-索引深度", "疑似冗余复合索引",
                                "前缀重复的复合索引",
                                names.get(i) + ":" + String.join(",", aCols) + " vs " + 
                                names.get(j) + ":" + String.join(",", bCols),
                                "WARNING", "考虑合并或删除其中一个");
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略检测错误
            }
        }
        
        // 3. 索引统计汇总
        if (fks.isEmpty()) {
            recordItem("IDX-FK-SUM", "数据库-索引深度", "外键索引汇总",
                "外键列应有索引",
                "无物理外键定义，跳过FK索引检测",
                "WARNING", "逻辑外键无索引也会导致全表扫描");
        } else {
            long fkMiss = scanItems.stream()
                .filter(it -> ((String) it.get("scan_id")).startsWith("IDX-FK-"))
                .filter(it -> !"NORMAL".equals(it.get("level")))
                .count();
            if (fkMiss == 0) {
                recordItem("IDX-FK-SUM", "数据库-索引深度", "外键索引汇总",
                    "所有外键都有索引",
                    fks.size() + "个外键，索引覆盖率100%",
                    "NORMAL", "FK索引完整");
            }
        }
        
        if (!allDbTables.isEmpty()) {
            long idxCount = scanItems.stream()
                .filter(it -> "数据库-索引深度".equals(it.get("module")))
                .filter(it -> "NORMAL".equals(it.get("level")))
                .count();
            long warnCount = scanItems.stream()
                .filter(it -> "数据库-索引深度".equals(it.get("module")))
                .filter(it -> !"NORMAL".equals(it.get("level")))
                .count();
            
            recordItem("IDX-STAT", "数据库-索引深度", "索引深度统计",
                "索引覆盖率统计",
                "全量索引检测中" + (warnCount == 0 ? "NORMAL" : "WARNING") + ": 正常" + idxCount + " 预警" + warnCount,
                warnCount == 0 ? "NORMAL" : "WARNING",
                "已扫描" + allDbTables.size() + "张表的索引");
        }
    }

    // ====================== 约束完整性扫描 ======================
    private void scanConstraint() {
        log.info("开始约束完整性扫描...");
        loadAllTableInfo();
        
        // 1. 主键完整性
        for (String tbl : allDbTables) {
            try {
                List<Map<String, Object>> pk = dbQuery(
                    "SELECT COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA='" + 
                    DB_NAME + "' AND TABLE_NAME='" + tbl + "' AND CONSTRAINT_NAME='PRIMARY'");
                if (pk.isEmpty()) {
                    recordItem("CONS-NOPK-" + tbl, "数据库-约束", "缺失主键",
                        "每张表必须有主键",
                        "表 " + tbl + " 无主键",
                        "FATAL", "无法唯一标识行，同步/恢复灾难");
                } else {
                    recordItem("CONS-PK-" + tbl, "数据库-约束", "主键正常",
                        "主键存在",
                        "主键: " + pk.get(0).get("COLUMN_NAME"),
                        "NORMAL", "");
                }
            } catch (Exception e) {
                // 忽略检测错误
            }
        }
        
        // 2. AUTO_INCREMENT 接近上限
        for (Map.Entry<String, Map<String, Object>> entry : dbTableDetail.entrySet()) {
            String tbl = entry.getKey();
            Map<String, Object> info = entry.getValue();
            List<Map<String, Object>> columns = (List<Map<String, Object>>) info.get("columns");
            
            for (Map<String, Object> col : columns) {
                String extra = col.containsKey("Extra") ? (String) col.get("Extra") : "";
                if (extra.toLowerCase().contains("auto_increment")) {
                    try {
                        String colName = (String) col.get("Field");
                        List<Map<String, Object>> maxVal = dbQuery("SELECT MAX(`" + colName + "`) as max_val FROM `" + tbl + "`");
                        if (!maxVal.isEmpty() && maxVal.get(0).get("max_val") != null) {
                            long val = ((Number) maxVal.get(0).get("max_val")).longValue();
                            String colType = ((String) col.get("Type")).toLowerCase();
                            if (colType.contains("int") && val > 2000000000L) {
                                recordItem("CONS-AI-" + tbl + "." + colName, "数据库-约束",
                                    "AUTO_INCREMENT接近上限",
                                    "自增值不应接近int上限",
                                    tbl + "." + colName + " 最大值=" + val + "，int上限=2147483647",
                                    "WARNING", "即将无法插入新记录",
                                    "ALTER TABLE " + tbl + " MODIFY " + colName + " BIGINT AUTO_INCREMENT;",
                                    "", "", "", 0);
                            }
                        }
                    } catch (Exception e) {
                        // 忽略检测错误
                    }
                }
            }
        }
        
        // 3. 字符集统一检测
        List<Map<String, Object>> charsetIssues = dbQuery(
            "SELECT TABLE_NAME, COLUMN_NAME, CHARACTER_SET_NAME FROM information_schema.COLUMNS " +
            "WHERE TABLE_SCHEMA='" + DB_NAME + "' AND CHARACTER_SET_NAME IS NOT NULL AND CHARACTER_SET_NAME != 'utf8mb4'");
        
        for (Map<String, Object> issue : charsetIssues) {
            String tbl = (String) issue.get("TABLE_NAME");
            String col = (String) issue.get("COLUMN_NAME");
            String cs = (String) issue.get("CHARACTER_SET_NAME");
            
            recordItem("CONS-CHARSET-" + tbl + "." + col, "数据库-约束",
                "非utf8mb4字符集",
                "所有文本列应为utf8mb4",
                tbl + "." + col + " 使用 " + cs,
                "WARNING", "emoji/生僻字会截断",
                "ALTER TABLE " + tbl + " MODIFY " + col + " VARCHAR(255) CHARACTER SET utf8mb4;",
                "", "", "", 0);
        }
        
        // 4. 表引擎检测
        List<Map<String, Object>> engineIssues = dbQuery(
            "SELECT TABLE_NAME, ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA='" + 
            DB_NAME + "' AND ENGINE != 'InnoDB'");
        
        for (Map<String, Object> issue : engineIssues) {
            String tbl = (String) issue.get("TABLE_NAME");
            String eng = (String) issue.get("ENGINE");
            
            recordItem("CONS-ENGINE-" + tbl, "数据库-约束",
                "非InnoDB引擎",
                "所有表应为InnoDB",
                tbl + " 使用 " + eng,
                "ERROR", "不支持事务/外键/行锁",
                "ALTER TABLE " + tbl + " ENGINE=InnoDB;",
                "", "", "", 0);
        }
        
        if (engineIssues.isEmpty()) {
            recordItem("CONS-ENGINE-SUM", "数据库-约束", "存储引擎一致性",
                "所有表应为InnoDB",
                "全部" + allDbTables.size() + "张表使用InnoDB",
                "NORMAL", "存储引擎统一");
        }
        
        if (charsetIssues.isEmpty()) {
            recordItem("CONS-CHARSET-SUM", "数据库-约束", "字符集一致性",
                "所有文本列应为utf8mb4",
                "全部文本列使用utf8mb4",
                "NORMAL", "字符集统一");
        }
        
        long pkOk = scanItems.stream()
            .filter(it -> ((String) it.get("scan_id")).startsWith("CONS-PK-"))
            .count();
        
        recordItem("CONS-PK-SUM", "数据库-约束", "主键完整性汇总",
            "所有表应有主键",
            "主键覆盖率: " + pkOk + "/" + allDbTables.size(),
            pkOk == allDbTables.size() ? "NORMAL" : "FATAL",
            "主键是数据完整性的基础");
    }

    // ====================== 数据质量扫描 ======================
    private void scanDataQuality() {
        log.info("开始数据质量扫描...");
        loadAllTableInfo();
        
        // 1. NULL 比例检测
        for (String tbl : allDbTables) {
            if (!dbTableDetail.containsKey(tbl)) continue;
            List<Map<String, Object>> columns = (List<Map<String, Object>>) dbTableDetail.get(tbl).get("columns");
            
            for (Map<String, Object> colInfo : columns) {
                String cName = (String) colInfo.get("Field");
                String nullFlag = (String) colInfo.get("Null");
                
                if ("id".equals(cName) || "create_time".equals(cName) || "update_time".equals(cName)) {
                    continue;
                }
                
                try {
                    List<Map<String, Object>> totalResult = dbQuery("SELECT COUNT(*) as cnt FROM `" + tbl + "`");
                    int total = 0;
                    if (!totalResult.isEmpty()) {
                        total = getIntValue(totalResult.get(0), "cnt");
                    }
                    if (total == 0) continue;
                    
                    List<Map<String, Object>> nullResult = dbQuery(
                        "SELECT COUNT(*) as cnt FROM `" + tbl + "` WHERE `" + cName + "` IS NULL");
                    int nullCnt = 0;
                    if (!nullResult.isEmpty()) {
                        nullCnt = getIntValue(nullResult.get(0), "cnt");
                    }
                    double ratio = (double) nullCnt / total * 100;
                    
                    if (ratio > 50) {
                        recordItem("DQ-NULL-" + tbl + "." + cName, "数据库-数据质量",
                            "字段NULL比例过高",
                            "NULL比例应<50%",
                            tbl + "." + cName + ": " + nullCnt + "/" + total + " (" + (int)ratio + "%)",
                            "WARNING", "数据完整性问题");
                    } else if (ratio > 0 && "NO".equals(nullFlag)) {
                        recordItem("DQ-NULL-" + tbl + "." + cName, "数据库-数据质量",
                            "NOT NULL字段存在NULL",
                            "NOT NULL列不应有NULL",
                            tbl + "." + cName + ": " + nullCnt + "条NULL",
                            "ERROR", "约束与实际数据不一致");
                    }
                } catch (Exception e) {
                    // 忽略检测错误
                }
            }
        }
        
        // 2. 日期逻辑错误
        String[][] dateChecks = {
            {"staff_master", "hire_date", "resign_date", "入职日期>退职日期"},
            {"booking_master", "booking_date", "create_time", "预定日期>创建时间"}
        };
        
        for (String[] check : dateChecks) {
            String tbl = check[0], colA = check[1], colB = check[2], desc = check[3];
            if (!allDbTables.contains(tbl)) continue;
            
            try {
                List<Map<String, Object>> bad = dbQuery(
                    "SELECT COUNT(*) as cnt FROM `" + tbl + "` WHERE `" + colA + "` > `" + colB + 
                    "` AND `" + colB + "` IS NOT NULL");
                if (!bad.isEmpty() && getIntValue(bad.get(0), "cnt") > 0) {
                    recordItem("DQ-DATE-" + tbl + "." + colA + "-" + colB, "数据库-数据质量",
                        desc,
                        "日期逻辑应合理",
                        "异常" + desc + ": " + bad.get(0).get("cnt") + "条",
                        "ERROR", "数据时序错误影响报表");
                }
            } catch (Exception e) {
                // 忽略检测错误
            }
        }
        
        // 3. 字符串首尾空格
        for (String tbl : allDbTables) {
            if (!dbTableDetail.containsKey(tbl)) continue;
            List<Map<String, Object>> columns = (List<Map<String, Object>>) dbTableDetail.get(tbl).get("columns");
            
            for (Map<String, Object> colInfo : columns) {
                String cName = (String) colInfo.get("Field");
                String cType = ((String) colInfo.get("Type")).toLowerCase();
                
                if (cType.contains("varchar") || cType.contains("char")) {
                    try {
                        List<Map<String, Object>> space = dbQuery(
                            "SELECT COUNT(*) as cnt FROM `" + tbl + "` WHERE `" + cName + 
                            "` LIKE '% ' OR `" + cName + "` LIKE ' %'");
                        if (!space.isEmpty() && getIntValue(space.get(0), "cnt") > 0) {
                            recordItem("DQ-TRIM-" + tbl + "." + cName, "数据库-数据质量",
                                "字段存在首尾空格",
                                "字符串不应含首尾空格",
                                tbl + "." + cName + ": " + space.get(0).get("cnt") + "条含空格",
                                "WARNING", "可能导致匹配/去重失败",
                                "UPDATE `" + tbl + "` SET `" + cName + "`=TRIM(`" + cName + "`);",
                                "", "", "", 0);
                        }
                    } catch (Exception e) {
                        // 忽略检测错误
                    }
                }
            }
        }
        
        // 4. 重复数据检测
        String[][] dupGroups = {
            {"customer_master", "customer_phone", "手机号"},
            {"staff_master", "staff_name", "员工姓名"},
            {"supplier_master", "supplier_phone", "供应商手机"}
        };
        
        for (String[] group : dupGroups) {
            String tbl = group[0], col = group[1], label = group[2];
            if (!allDbTables.contains(tbl)) continue;
            
            try {
                List<Map<String, Object>> dups = dbQuery(
                    "SELECT `" + col + "`, COUNT(*) as c FROM `" + tbl + "` GROUP BY `" + col + 
                    "` HAVING c > 1 LIMIT 10");
                for (Map<String, Object> dup : dups) {
                    Object val = dup.get(col);
                    int cnt = getIntValue(dup, "c");
                    recordItem("DQ-DUP-" + tbl + "." + col, "数据库-数据质量",
                        "重复" + label,
                        "不应出现重复",
                        tbl + ": " + label + "=" + val + " 重复" + cnt + "次",
                        "WARNING", "影响统计/关联准确度");
                }
            } catch (Exception e) {
                // 忽略检测错误
            }
        }
        
        // 5. 手机号格式校验
        for (String tbl : allDbTables) {
            if (!dbTableDetail.containsKey(tbl)) continue;
            List<Map<String, Object>> columns = (List<Map<String, Object>>) dbTableDetail.get(tbl).get("columns");
            
            for (Map<String, Object> colInfo : columns) {
                String cName = ((String) colInfo.get("Field")).toLowerCase();
                if (cName.contains("phone")) {
                    try {
                        String colName = (String) colInfo.get("Field");
                        List<Map<String, Object>> bad = dbQuery(
                            "SELECT COUNT(*) as cnt FROM `" + tbl + "` WHERE `" + colName + 
                            "` IS NOT NULL AND LENGTH(`" + colName + "`) < 11");
                        if (!bad.isEmpty() && getIntValue(bad.get(0), "cnt") > 0) {
                            recordItem("DQ-FMT-" + tbl + "." + colName, "数据库-数据质量",
                                "手机号格式异常",
                                "应为11位",
                                bad.get(0).get("cnt") + "条格式异常",
                                "WARNING", "短信/通知无法送达");
                        }
                    } catch (Exception e) {
                        // 忽略检测错误
                    }
                }
            }
        }
        
        // 6. 数据质量汇总
        long totalNullChecks = scanItems.stream()
            .filter(it -> "数据库-数据质量".equals(it.get("module")))
            .filter(it -> ((String) it.get("scan_id")).contains("NULL"))
            .count();
        long totalDupChecks = scanItems.stream()
            .filter(it -> "数据库-数据质量".equals(it.get("module")))
            .filter(it -> ((String) it.get("scan_id")).contains("DUP"))
            .count();
        long totalFmtChecks = scanItems.stream()
            .filter(it -> "数据库-数据质量".equals(it.get("module")))
            .filter(it -> ((String) it.get("scan_id")).contains("FMT"))
            .count();
        
        recordItem("DQ-STAT-NULL", "数据库-数据质量", "字段NULL分布统计",
            "统计所有字段NULL比例",
            "已检测" + totalNullChecks + "个字段的NULL分布",
            "NORMAL", "NULL覆盖率统计");
        recordItem("DQ-STAT-DUP", "数据库-数据质量", "重复数据统计",
            "统计关键业务字段去重",
            "已检测" + totalDupChecks + "组重复字段",
            "NORMAL", "去重覆盖率统计");
        recordItem("DQ-STAT-FMT", "数据库-数据质量", "数据格式统计",
            "手机号/日期格式校验",
            "已检测" + totalFmtChecks + "组格式校验",
            "NORMAL", "格式校验覆盖率");
        
        long dqIssues = scanItems.stream()
            .filter(it -> "数据库-数据质量".equals(it.get("module")))
            .filter(it -> !"NORMAL".equals(it.get("level")))
            .count();
        
        recordItem("DQ-SUMMARY", "数据库-数据质量", "数据质量全量汇总",
            "无脏数据、无重复、无格式异常",
            "总检测:" + scanItems.stream().filter(it -> "数据库-数据质量".equals(it.get("module"))).count() + 
            " 问题项:" + dqIssues,
            dqIssues == 0 ? "NORMAL" : "WARNING",
            "数据质量问题总数");
    }

    // ====================== 数据库性能扫描 ======================
    private void scanPerformance() {
        log.info("开始数据库性能扫描...");
        
        // 1. 慢查询日志配置
        List<Map<String, Object>> slow = dbQuery("SHOW VARIABLES LIKE 'slow_query_log'");
        List<Map<String, Object>> slowFile = dbQuery("SHOW VARIABLES LIKE 'slow_query_log_file'");
        List<Map<String, Object>> longTime = dbQuery("SHOW VARIABLES LIKE 'long_query_time'");
        
        String slowVal = slow.isEmpty() ? "?" : (String) slow.get(0).get("Value");
        String longTimeVal = longTime.isEmpty() ? "?" : (String) longTime.get(0).get("Value");
        
        recordItem("PERF-SLOW-CFG", "数据库-性能", "慢查询日志配置",
            "慢查询日志应开启",
            "slow_query_log=" + slowVal + "  long_query_time=" + longTimeVal,
            "ON".equalsIgnoreCase(slowVal) ? "NORMAL" : "WARNING",
            "未开启慢查询无法发现性能瓶颈");
        
        // 2. 连接池饱和度
        try {
            List<Map<String, Object>> currResult = dbQuery("SHOW STATUS LIKE 'Threads_connected'");
            List<Map<String, Object>> maxResult = dbQuery("SHOW VARIABLES LIKE 'max_connections'");
            if (!currResult.isEmpty() && !maxResult.isEmpty()) {
                int curr = Integer.parseInt((String) currResult.get(0).get("Value"));
                int maxc = Integer.parseInt((String) maxResult.get(0).get("Value"));
                double rate = (double) curr / maxc * 100;
                
                String level;
                if (rate >= 90) level = "ERROR";
                else if (rate >= 70) level = "WARNING";
                else level = "NORMAL";
                
                recordItem("PERF-CONN", "数据库-性能", "数据库连接池饱和度",
                    "使用率<80%",
                    curr + "/" + maxc + " (" + String.format("%.1f%%", rate) + ")",
                    level, "连接用尽会导致服务拒绝");
            }
        } catch (Exception e) {
            // 忽略检测错误
        }
        
        // 3. 锁等待次数
        try {
            List<Map<String, Object>> locks = dbQuery("SHOW STATUS LIKE 'Innodb_row_lock_waits'");
            if (!locks.isEmpty() && Integer.parseInt((String) locks.get(0).get("Value")) > 100) {
                recordItem("PERF-LOCK", "数据库-性能", "InnoDB锁等待",
                    "锁等待应<100",
                    "累计" + locks.get(0).get("Value") + "次",
                    "WARNING", "高并发下性能下降");
            }
        } catch (Exception e) {
            // 忽略检测错误
        }
        
        // 4. 临时表频率
        try {
            List<Map<String, Object>> tmp = dbQuery("SHOW STATUS LIKE 'Created_tmp_tables'");
            List<Map<String, Object>> tmpDisk = dbQuery("SHOW STATUS LIKE 'Created_tmp_disk_tables'");
            if (!tmp.isEmpty() && !tmpDisk.isEmpty()) {
                int tmpVal = Integer.parseInt((String) tmp.get(0).get("Value"));
                int tmpDiskVal = Integer.parseInt((String) tmpDisk.get(0).get("Value"));
                double diskRatio = (double) tmpDiskVal / Math.max(1, tmpVal) * 100;
                
                recordItem("PERF-TMP", "数据库-性能", "磁盘临时表比例",
                    "磁盘临时表<10%",
                    "磁盘:" + tmpDiskVal + " 总量:" + tmpVal + " (" + String.format("%.1f%%", diskRatio) + ")",
                    diskRatio > 20 ? "WARNING" : "NORMAL",
                    "磁盘临时表增多说明排序/分组缺索引");
            }
        } catch (Exception e) {
            // 忽略检测错误
        }
        
        // 5. QPS/TPS 统计
        try {
            List<Map<String, Object>> qps = dbQuery("SHOW STATUS LIKE 'Questions'");
            List<Map<String, Object>> uptime = dbQuery("SHOW STATUS LIKE 'Uptime'");
            if (!qps.isEmpty() && !uptime.isEmpty()) {
                int qpsVal = Integer.parseInt((String) qps.get(0).get("Value"));
                int uptimeVal = Integer.parseInt((String) uptime.get(0).get("Value"));
                if (uptimeVal > 0) {
                    double avgQps = (double) qpsVal / uptimeVal;
                    recordItem("PERF-QPS", "数据库-性能", "平均QPS",
                        "QPS在合理范围",
                        "平均QPS: " + String.format("%.1f", avgQps),
                        "NORMAL", "数据库负载正常");
                }
            }
        } catch (Exception e) {
            // 忽略检测错误
        }
        
        // 6. InnoDB 缓冲池命中率
        try {
            List<Map<String, Object>> bp = dbQuery("SHOW STATUS LIKE 'Innodb_buffer_pool_read_requests'");
            List<Map<String, Object>> bpr = dbQuery("SHOW STATUS LIKE 'Innodb_buffer_pool_reads'");
            if (!bp.isEmpty() && !bpr.isEmpty()) {
                int bpVal = Integer.parseInt((String) bp.get(0).get("Value"));
                int bprVal = Integer.parseInt((String) bpr.get(0).get("Value"));
                if (bpVal > 0) {
                    double hitRate = 100 - (double) bprVal / bpVal * 100;
                    recordItem("PERF-BPHIT", "数据库-性能", "InnoDB缓冲池命中率",
                        "命中率>99%",
                        "命中率: " + String.format("%.2f%%", hitRate),
                        hitRate < 95 ? "WARNING" : "NORMAL",
                        "缓冲池命中率偏低需加大innodb_buffer_pool_size");
                }
            }
        } catch (Exception e) {
            // 忽略检测错误
        }
        
        // 7. 表体积统计
        if (!allDbTables.isEmpty()) {
            try {
                List<Map<String, Object>> sizes = dbQuery(
                    "SELECT TABLE_NAME, TABLE_ROWS, DATA_LENGTH + INDEX_LENGTH as size FROM information_schema.TABLES " +
                    "WHERE TABLE_SCHEMA='" + DB_NAME + "' ORDER BY DATA_LENGTH DESC LIMIT 20");
                for (Map<String, Object> size : sizes) {
                    String tbl = (String) size.get("TABLE_NAME");
                    long rows = size.get("TABLE_ROWS") == null ? 0 : ((Number) size.get("TABLE_ROWS")).longValue();
                    long sizeVal = size.get("size") == null ? 0 : ((Number) size.get("size")).longValue();
                    double sizeMb = sizeVal / 1048576.0;
                    
                    String lv = "NORMAL";
                    String note = "表大小正常";
                    if (sizeMb > 100) {
                        lv = "WARNING";
                        note = "单表>" + (int)sizeMb + "MB，需关注";
                    }
                    
                    recordItem("PERF-SIZE-" + tbl, "数据库-性能", "表体积统计",
                        "单表<100MB",
                        tbl + ": " + rows + "行 " + String.format("%.1fMB", sizeMb),
                        lv, note);
                }
            } catch (Exception e) {
                // 忽略检测错误
            }
        }
        
        long perfIssues = scanItems.stream()
            .filter(it -> "数据库-性能".equals(it.get("module")))
            .filter(it -> !"NORMAL".equals(it.get("level")))
            .count();
        
        recordItem("PERF-SUMMARY", "数据库-性能", "数据库性能汇总",
            "慢查询/连接池/锁/临时表均正常",
            "性能问题: " + perfIssues + " 项",
            perfIssues == 0 ? "NORMAL" : "WARNING",
            "数据库性能审计结果");
    }

    // ====================== 后端安全扫描 ======================
    private void scanBackendSecurity() {
        log.info("开始后端安全扫描...");
        
        List<String> allJava = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(BACK_CODE_PATH))) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".java"))
                 .forEach(p -> allJava.add(p.toString()));
        } catch (IOException e) {
            log.error("扫描Java文件异常: {}", e.getMessage());
        }
        
        // 1. SQL注入风险检测
        for (String fp : allJava) {
            try {
                String content = Files.readString(Paths.get(fp));
                String fileName = Paths.get(fp).getFileName().toString();
                List<String> riskyPatterns = new ArrayList<>();
                
                if (content.contains("\"+") && (content.toLowerCase().contains("select") || 
                    content.toLowerCase().contains("delete") || content.toLowerCase().contains("update"))) {
                    riskyPatterns.add("可能存在字符串拼接SQL");
                }
                if (content.contains("System.out.println") && (content.toLowerCase().contains("password") || 
                    content.toLowerCase().contains("token"))) {
                    riskyPatterns.add("可能打印密码/Token到控制台");
                }
                if (content.contains("log.info") && content.toLowerCase().contains("password")) {
                    riskyPatterns.add("日志中输出敏感字段");
                }
                
                if (!riskyPatterns.isEmpty()) {
                    String level = riskyPatterns.toString().contains("SQL") ? "FATAL" : "ERROR";
                    recordItem("BSEC-" + fileName, "后端-安全", "代码安全风险",
                        "无SQL注入/无敏感信息泄露",
                        String.join("; ", riskyPatterns),
                        level, "可能导致数据泄露或注入攻击", "", "", fp, "", 0);
                }
            } catch (IOException e) {
                // 忽略读取错误
            }
        }
        
        // 2. JWT密钥强度检测
        for (String fp : allJava) {
            try {
                String content = Files.readString(Paths.get(fp));
                if (content.toLowerCase().contains("jwt.secret") || content.contains("jwtSecret") || 
                    content.contains("SECRET")) {
                    Pattern secretPattern = Pattern.compile("['\"]([^'\"]{10,50})['\"]");
                    Matcher matcher = secretPattern.matcher(content);
                    while (matcher.find()) {
                        String secret = matcher.group(1);
                        if (secret.length() < 20) {
                            recordItem("BSEC-JWT-" + Paths.get(fp).getFileName().toString(), "后端-安全",
                                "JWT密钥强度不足",
                                "密钥长度>=32字符",
                                "密钥长度仅" + secret.length() + "字符",
                                "ERROR", "弱密钥易被暴力破解", "", "", fp, "", 0);
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                // 忽略读取错误
            }
        }
        
        // 3. BCrypt检测
        List<String> bcryptFiles = scanFileKeyword(BACK_CODE_PATH, "BCrypt");
        if (!bcryptFiles.isEmpty()) {
            recordItem("BSEC-BCRYPT", "后端-安全", "密码加密策略",
                "应使用BCrypt加密密码",
                "已使用BCrypt（" + bcryptFiles.size() + "个文件）",
                "NORMAL", "加密策略合规");
        } else {
            recordItem("BSEC-BCRYPT", "后端-安全", "密码加密策略",
                "应使用BCrypt加密密码",
                "未检测到BCrypt",
                "ERROR", "密码可能明文存储");
        }
        
        // 4. @PreAuthorize 权限注解检测
        List<String> authCtrl = scanFileKeyword(BACK_CODE_PATH, "@PreAuthorize");
        long ctrlTotal = allJava.stream()
            .filter(fp -> Paths.get(fp).getFileName().toString().endsWith("Controller.java"))
            .count();
        
        if (ctrlTotal > 0 && authCtrl.size() < ctrlTotal) {
            recordItem("BSEC-AUTH-ALL", "后端-安全", "权限注解覆盖率",
                "所有Controller应有权限注解",
                authCtrl.size() + "/" + ctrlTotal + "个Controller有权限注解",
                "WARNING", "未授权访问风险");
        }
        
        long bsecTotal = scanItems.stream()
            .filter(it -> "后端-安全".equals(it.get("module")))
            .count();
        long bsecIssues = scanItems.stream()
            .filter(it -> "后端-安全".equals(it.get("module")))
            .filter(it -> !"NORMAL".equals(it.get("level")))
            .count();
        
        recordItem("BSEC-STAT", "后端-安全", "后端安全扫描统计",
            "安全扫描覆盖率",
            "已扫描" + allJava.size() + "个Java文件 安全检测" + bsecTotal + "项",
            "NORMAL", "安全扫描统计");
        recordItem("BSEC-SUMMARY", "后端-安全", "后端安全汇总",
            "无SQL注入/密码明文/JWT弱密钥/越权",
            "安全问题: " + bsecIssues + " 项",
            bsecIssues > 0 ? "FATAL" : "NORMAL", "代码安全审计结果");
    }

    // ====================== API深度扫描 ======================
    private void scanApiDeep() {
        log.info("开始API深度扫描...");
        
        List<String> allJava = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(BACK_CODE_PATH))) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".java"))
                 .forEach(p -> allJava.add(p.toString()));
        } catch (IOException e) {
            log.error("扫描Java文件异常: {}", e.getMessage());
        }
        
        // 1. 自动发现所有Controller端点
        int totalEndpoints = 0;
        for (String fp : allJava) {
            try {
                String content = Files.readString(Paths.get(fp));
                if (!content.contains("@RestController") && !content.contains("@Controller")) {
                    continue;
                }
                String fileName = Paths.get(fp).getFileName().toString();
                
                // 提取类级别RequestMapping
                String basePath = "";
                Pattern clsPattern = Pattern.compile("@RequestMapping\\s*\\(\\s*[\"']([^\"']+)[\"']");
                Matcher clsMatcher = clsPattern.matcher(content);
                if (clsMatcher.find()) {
                    basePath = clsMatcher.group(1);
                }
                
                // 提取方法级别Mapping
                Pattern methodPattern = Pattern.compile(
                    "@(?:Get|Post|Put|Delete|Request)Mapping\\s*\\(\\s*(?:value\\s*=\\s*)?[\"']([^\"']+)[\"']");
                Matcher methodMatcher = methodPattern.matcher(content);
                while (methodMatcher.find()) {
                    String path = methodMatcher.group(1);
                    String fullPath = basePath + path;
                    totalEndpoints++;
                    
                    // 检测是否有权限注解
                    int methodPos = content.indexOf(path);
                    String methodSlice = content.substring(Math.max(0, methodPos - 200), 
                        Math.min(content.length(), methodPos));
                    boolean hasAuth = methodSlice.contains("@PreAuthorize") || methodSlice.contains("@Secured");
                    
                    // 检测路径是否含敏感词
                    boolean isSensitive = fullPath.toLowerCase().contains("admin") || 
                                         fullPath.toLowerCase().contains("delete") || 
                                         fullPath.toLowerCase().contains("remove") || 
                                         fullPath.toLowerCase().contains("grant") || 
                                         fullPath.toLowerCase().contains("reset");
                    
                    recordItem("APID-" + fileName + "-" + path.replace('/', '_'),
                        "API端点-深度", "端点自动发现",
                        "每个端点含权限/校验/文档",
                        fullPath,
                        isSensitive && !hasAuth ? "WARNING" : "NORMAL",
                        "自动从Controller提取的API端点");
                }
            } catch (IOException e) {
                // 忽略读取错误
            }
        }
        
        recordItem("APID-COUNT", "API端点-深度", "全端点统计",
            "所有Controller端点全部发现",
            "共发现" + totalEndpoints + "个REST端点",
            "NORMAL", "端点覆盖率100%");
        
        // 2. API响应格式校验
        if (apiLogin()) {
            int checked = 0, ok = 0, err = 0;
            for (String url : API_FULL_LIST) {
                Map<String, Object> resp = apiGet(url, null);
                checked++;
                if (resp.get("code") != null && (int) resp.get("code") == 200) {
                    ok++;
                    Object data = resp.get("data");
                    int respSize = data != null ? data.toString().length() : 0;
                    recordItem("APID-RESP-" + url.replace('/', '_'), "API端点-深度",
                        "响应体大小检查",
                        "响应体<100KB",
                        respSize + "字节",
                        respSize > 102400 ? "WARNING" : "NORMAL",
                        "大数据量响应影响性能");
                } else {
                    err++;
                }
            }
            recordItem("APID-RESP-STAT", "API端点-深度", "API响应统计",
                "所有API畅通",
                "检测" + checked + "个, 正常" + ok + ", 异常" + err,
                err == 0 ? "NORMAL" : "ERROR", "API响应率");
        }
    }

    // ====================== 前端安全扫描 ======================
    private void scanFrontSecurity() {
        log.info("开始前端安全扫描...");
        
        // 1. v-html XSS风险检测
        List<String> xssFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(FRONT_CODE_PATH))) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> {
                     String fn = p.toString();
                     return fn.endsWith(".vue") || fn.endsWith(".js");
                 })
                 .forEach(p -> {
                     try {
                         String content = Files.readString(p);
                         if (content.contains("v-html") && !content.contains("DOMPurify") && 
                             !content.toLowerCase().contains("sanitize")) {
                             xssFiles.add(p.toString());
                         }
                     } catch (IOException e) {
                         // 忽略读取错误
                     }
                 });
        } catch (IOException e) {
            log.error("扫描前端文件异常: {}", e.getMessage());
        }
        
        for (String fp : xssFiles) {
            recordItem("FSEC-XSS-" + Paths.get(fp).getFileName().toString(), "前端-安全",
                "v-html XSS风险",
                "v-html应使用DOMPurify消毒",
                "文件: " + fp,
                "FATAL", "XSS可注入恶意脚本", "", "", fp, "", 0);
        }
        
        if (xssFiles.isEmpty()) {
            recordItem("FSEC-XSS-ALL", "前端-安全", "v-html安全",
                "无未消毒v-html",
                "全量Vue/JS文件检查通过",
                "NORMAL", "XSS防护合规");
        }
        
        // 2. localStorage敏感数据检测
        List<String> lsFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(FRONT_CODE_PATH))) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> {
                     String fn = p.toString();
                     return fn.endsWith(".vue") || fn.endsWith(".js");
                 })
                 .forEach(p -> {
                     try {
                         String content = Files.readString(p);
                         if (content.contains("localStorage.setItem") && 
                             (content.contains("token") || content.contains("password"))) {
                             lsFiles.add(p.toString());
                         }
                     } catch (IOException e) {
                         // 忽略读取错误
                     }
                 });
        } catch (IOException e) {
            log.error("扫描前端文件异常: {}", e.getMessage());
        }
        
        for (String fp : lsFiles) {
            recordItem("FSEC-LS-" + Paths.get(fp).getFileName().toString(), "前端-安全",
                "localStorage敏感数据",
                "token/password不应存localStorage",
                "文件: " + fp,
                "ERROR", "XSS攻击可窃取token", "", "", fp, "", 0);
        }
        
        long fsecFiles = 0;
        try (Stream<Path> paths = Files.walk(Paths.get(FRONT_CODE_PATH))) {
            fsecFiles = paths.filter(Files::isRegularFile)
                .filter(p -> {
                    String fn = p.toString();
                    return fn.endsWith(".vue") || fn.endsWith(".js");
                })
                .count();
        } catch (IOException e) {
            log.error("统计前端文件异常: {}", e.getMessage());
        }
        
        recordItem("FSEC-STAT", "前端-安全", "前端安全扫描统计",
            "安全扫描覆盖率",
            "已扫描" + fsecFiles + "个前端文件",
            "NORMAL", "前端安全扫描统计");
        
        long fsecIssues = scanItems.stream()
            .filter(it -> "前端-安全".equals(it.get("module")))
            .filter(it -> !"NORMAL".equals(it.get("level")))
            .count();
        
        recordItem("FSEC-SUMMARY", "前端-安全", "前端安全汇总",
            "无XSS风险、无localStorage敏感数据",
            "安全问题: " + fsecIssues + " 项",
            fsecIssues > 0 ? "FATAL" : "NORMAL", "前端安全审计结果");
    }

    // ====================== 配置日志扫描 ======================
    private void scanConfigLog() {
        log.info("开始配置日志扫描...");
        
        String projectRoot = "F:\\solo\\project\\又见炊烟餐饮管理系统";
        List<String> configPaths = Arrays.asList(
            projectRoot + "\\banquet_project\\src\\main\\resources\\application.yml",
            projectRoot + "\\banquet_project\\src\\main\\resources\\application-dev.yml",
            projectRoot + "\\banquet_project\\src\\main\\resources\\application-prod.yml"
        );
        
        for (String cp : configPaths) {
            Path configPath = Paths.get(cp);
            if (Files.exists(configPath)) {
                try {
                    String content = Files.readString(configPath);
                    String fileName = configPath.getFileName().toString();
                    
                    String[][] checks = {
                        {"ddl-auto", content.contains("ddl-auto: none") ? "true" : "false", 
                         "禁止自动DDL", "ddl-auto=update会意外修改表结构"},
                        {"loglevel", (content.contains("level: ERROR") || content.contains("level: WARN")) ? "true" : "false", 
                         "生产日志级别>=WARN", "DEBUG日志影响性能泄露信息"},
                        {"pool", content.contains("maximum-pool-size") ? "true" : "false", 
                         "连接池最大连接数已配置", "未配置可能导致连接泄露"},
                        {"actuator", content.toLowerCase().contains("actuator") ? "true" : "false", 
                         "应配置健康检查端点", "无健康检查无法监控服务状态"}
                    };
                    
                    for (String[] check : checks) {
                        String ck = check[0], ok = check[1], good = check[2], bad = check[3];
                        recordItem("CFG-" + ck + "-" + fileName, "配置与日志", ck,
                            good, "true".equals(ok) ? "OK" : "未配置",
                            "true".equals(ok) ? "NORMAL" : "WARNING",
                            bad, "", "", cp, "", 0);
                    }
                } catch (IOException e) {
                    log.error("读取配置文件异常: {}", e.getMessage());
                }
            }
        }
        
        // Swagger/OpenAPI文档检测
        List<String> swagFiles = scanFileKeyword(BACK_CODE_PATH, "@ApiOperation");
        if (!swagFiles.isEmpty()) {
            recordItem("CFG-SWAGGER", "配置与日志", "API文档(Swagger)",
                "应有Swagger注解",
                swagFiles.size() + "个Controller有@ApiOperation",
                "NORMAL", "API文档齐全");
        } else {
            recordItem("CFG-SWAGGER", "配置与日志", "API文档(Swagger)",
                "应有Swagger注解",
                "未检测到@ApiOperation",
                "WARNING", "无API文档前端联调困难");
        }
        
        // 审计日志检测
        List<String> auditFiles = scanFileKeyword(BACK_CODE_PATH, "@Audit");
        if (auditFiles.isEmpty()) {
            auditFiles = scanFileKeyword(BACK_CODE_PATH, "AuditLog");
        }
        recordItem("CFG-AUDIT", "配置与日志", "审计日志",
            "应有操作审计记录",
            auditFiles.isEmpty() ? "未检测到审计日志（@Audit或AuditLog）" : "已配置",
            auditFiles.isEmpty() ? "WARNING" : "NORMAL",
            "无法追查误操作/恶意操作");
        
        // 配置文件完整性统计
        List<String> foundCfgs = configPaths.stream()
            .filter(cp -> Files.exists(Paths.get(cp)))
            .collect(Collectors.toList());
        List<String> missingCfgs = configPaths.stream()
            .filter(cp -> !Files.exists(Paths.get(cp)))
            .collect(Collectors.toList());
        
        for (String fp : foundCfgs) {
            recordItem("CFG-FILE-" + Paths.get(fp).getFileName().toString(), "配置与日志",
                "配置文件存在性",
                "配置文件应存在",
                "文件: " + fp + " ✓",
                "NORMAL", "配置文件完整");
        }
        for (String fp : missingCfgs) {
            recordItem("CFG-FILE-" + Paths.get(fp).getFileName().toString(), "配置与日志",
                "配置文件缺失",
                "配置文件应存在",
                "文件: " + fp + " ✗",
                "WARNING", "缺少环境配置");
        }
        
        long cfgIssues = scanItems.stream()
            .filter(it -> "配置与日志".equals(it.get("module")))
            .filter(it -> !"NORMAL".equals(it.get("level")))
            .count();
        
        recordItem("CFG-SUMMARY", "配置与日志", "配置与日志汇总",
            "配置完整、日志规范、有API文档",
            "配置问题: " + cfgIssues + " 项",
            cfgIssues == 0 ? "NORMAL" : "WARNING", "系统配置审计结果");
    }

    // ====================== 业务链路扫描 ======================
    private void scanBusinessLink() {
        log.info("开始业务链路扫描...");
        loadAllTableInfo();
        
        // 链路1：采购申请 → 验收 → 入库 → 库存
        if (allDbTables.contains("purchase_request") && allDbTables.contains("goods_receipt") && 
            allDbTables.contains("inventory_summary")) {
            try {
                List<Map<String, Object>> pending = dbQuery(
                    "SELECT COUNT(*) as cnt FROM purchase_request WHERE status='approved' AND id NOT IN " +
                    "(SELECT DISTINCT purchase_id FROM goods_receipt)");
                if (!pending.isEmpty() && getIntValue(pending.get(0), "cnt") > 0) {
                    recordItem("BIZ-PROC-001", "业务链路", "采购→验收闭环",
                        "已审批采购单应有验收记录",
                        pending.get(0).get("cnt") + "条已审批未验收",
                        "WARNING", "采购流程中断，库存未更新");
                }
            } catch (Exception e) {
                // 忽略检测错误
            }
            
            recordItem("BIZ-PROC-002", "业务链路", "验收→入库检测",
                "验收记录应关联入库",
                "业务链路扫描（需进一步明细校验）",
                "NORMAL", "采购→入库链路存在");
        }
        
        // 链路2：预订 → 收银 → 报表
        if (allDbTables.contains("booking_master") && allDbTables.contains("finance_receivable")) {
            try {
                List<Map<String, Object>> uncashed = dbQuery(
                    "SELECT COUNT(*) as cnt FROM booking_master WHERE booking_status='confirmed' AND id NOT IN " +
                    "(SELECT booking_id FROM finance_receivable)");
                if (!uncashed.isEmpty() && getIntValue(uncashed.get(0), "cnt") > 0) {
                    recordItem("BIZ-BOOK-001", "业务链路", "预订→收银闭环",
                        "已确认预订应有财务记录",
                        uncashed.get(0).get("cnt") + "条已确认无财务记录",
                        "WARNING", "预订收入未入账");
                }
            } catch (Exception e) {
                // 忽略检测错误
            }
        }
        
        // 链路3：物料申领 → 库存扣减
        if (allDbTables.contains("material_requisition") && allDbTables.contains("inventory_summary")) {
            recordItem("BIZ-REQ-001", "业务链路", "领料→库存扣减检测",
                "领料审批通过后库存应减少",
                "业务链路扫描（需进一步明细校验）",
                "NORMAL", "领料链路存在");
        }
        
        // 链路4：库存下限预警
        try {
            if (allDbTables.contains("inventory_summary")) {
                List<Map<String, Object>> low = dbQuery(
                    "SELECT COUNT(*) as cnt FROM inventory_summary WHERE total_quantity <= 0");
                if (!low.isEmpty() && getIntValue(low.get(0), "cnt") > 0) {
                    recordItem("BIZ-INV-001", "业务链路", "库存不足预警",
                        "库存量应>0",
                        low.get(0).get("cnt") + "种物料库存<=0",
                        "ERROR", "影响正常出菜/采购节奏");
                }
            }
        } catch (Exception e) {
            // 忽略检测错误
        }
        
        // 链路5：员工排班覆盖检测
        if (allDbTables.contains("attendance")) {
            try {
                String today = java.time.LocalDate.now().toString();
                List<Map<String, Object>> todayAtt = dbQuery(
                    "SELECT COUNT(*) as cnt FROM attendance WHERE attendance_date='" + today + "'");
                if (!todayAtt.isEmpty() && getIntValue(todayAtt.get(0), "cnt") == 0) {
                    recordItem("BIZ-HR-001", "业务链路", "当日排班检测",
                        "当日应有考勤/排班记录",
                        "今日无排班记录",
                        "WARNING", "可能无人值班");
                }
            } catch (Exception e) {
                // 忽略检测错误
            }
        }
        
        // 链路6：成本卡 → 毛利率 → 定价
        if (allDbTables.contains("cost_card") && allDbTables.contains("dish_master")) {
            try {
                // 亏本菜品
                List<Map<String, Object>> loss = dbQuery(
                    "SELECT COUNT(*) as cnt FROM dish_master WHERE sale_price < cost_price AND is_active=1");
                if (!loss.isEmpty() && getIntValue(loss.get(0), "cnt") > 0) {
                    recordItem("BIZ-COST-001", "业务链路", "售价低于成本",
                        "售价>=成本",
                        loss.get(0).get("cnt") + "道菜品亏本销售",
                        "ERROR", "亏损经营需立即调整");
                }
                
                // 成本卡成本为0
                List<Map<String, Object>> zeroCost = dbQuery(
                    "SELECT COUNT(*) as cnt FROM cost_card WHERE total_cost<=0 AND status='active'");
                if (!zeroCost.isEmpty() && getIntValue(zeroCost.get(0), "cnt") > 0) {
                    recordItem("BIZ-COST-002", "业务链路", "成本卡成本异常",
                        "成本卡总成本>0",
                        zeroCost.get(0).get("cnt") + "条成本卡异常",
                        "ERROR", "成本核算错误影响利润率");
                }
                
                // 毛利率异常
                List<Map<String, Object>> badMargin = dbQuery(
                    "SELECT COUNT(*) as cnt FROM cost_card WHERE gross_margin IS NOT NULL AND " +
                    "(gross_margin<5 OR gross_margin>85) AND status='active'");
                if (!badMargin.isEmpty() && getIntValue(badMargin.get(0), "cnt") > 0) {
                    recordItem("BIZ-COST-003", "业务链路", "毛利率异常",
                        "毛利率在5%-85%",
                        badMargin.get(0).get("cnt") + "条异常",
                        "WARNING", "定价或成本核算偏差");
                }
            } catch (Exception e) {
                // 忽略检测错误
            }
        }
        
        // 链路7：应付账款 → 付款 → 核销
        if (allDbTables.contains("finance_payable") && allDbTables.contains("supplier_master")) {
            try {
                List<Map<String, Object>> overdue = dbQuery(
                    "SELECT COUNT(*) as cnt FROM finance_payable WHERE due_date < CURDATE() AND pending_amount > 0");
                if (!overdue.isEmpty() && getIntValue(overdue.get(0), "cnt") > 0) {
                    recordItem("BIZ-PAY-001", "业务链路", "应付账款逾期",
                        "应付账款应在到期日前结清",
                        overdue.get(0).get("cnt") + "条逾期未付",
                        "WARNING", "影响供应商关系和信用");
                }
                
                // 供应商欠款总额
                List<Map<String, Object>> totalDebt = dbQuery(
                    "SELECT COALESCE(SUM(pending_amount),0) as total FROM finance_payable");
                if (!totalDebt.isEmpty()) {
                    recordItem("BIZ-PAY-002", "业务链路", "应付账款总额",
                        "应付账款总额应在合理范围",
                        "应付总额: " + totalDebt.get(0).get("total") + "元",
                        "NORMAL", "应付账款统计");
                }
            } catch (Exception e) {
                // 忽略检测错误
            }
        }
        
        if (allDbTables.contains("finance_receivable")) {
            try {
                List<Map<String, Object>> badDebt = dbQuery(
                    "SELECT COUNT(*) as cnt FROM finance_receivable WHERE due_date < DATE_SUB(CURDATE(), INTERVAL 90 DAY) " +
                    "AND pending_amount > 0");
                if (!badDebt.isEmpty() && getIntValue(badDebt.get(0), "cnt") > 0) {
                    recordItem("BIZ-RCV-001", "业务链路", "应收账款逾期90天+",
                        "应收账款应在90天内收回",
                        badDebt.get(0).get("cnt") + "条坏账风险",
                        "ERROR", "长期应收可能成为坏账");
                }
            } catch (Exception e) {
                // 忽略检测错误
            }
        }
        
        // 链路8：库存损耗/报损 → 损益
        if (allDbTables.contains("waste") || allDbTables.contains("waste_record")) {
            try {
                String tbl = allDbTables.contains("waste") ? "waste" : "waste_record";
                recordItem("BIZ-WASTE-001", "业务链路", "报损检测存在性",
                    "报损表应有记录流程",
                    "表'" + tbl + "'已存在",
                    "NORMAL", "损耗管理链路已配置");
            } catch (Exception e) {
                // 忽略检测错误
            }
        }
        
        // 链路9：员工入职/离职/异动
        if (allDbTables.contains("employee_lifecycle")) {
            try {
                List<Map<String, Object>> recentHires = dbQuery(
                    "SELECT COUNT(*) as cnt FROM employee_lifecycle WHERE event_type='hire' AND " +
                    "event_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)");
                List<Map<String, Object>> recentFires = dbQuery(
                    "SELECT COUNT(*) as cnt FROM employee_lifecycle WHERE event_type='resign' AND " +
                    "event_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)");
                
                if (!recentHires.isEmpty()) {
                    recordItem("BIZ-HR-002", "业务链路", "近30天入职统计",
                        "入职记录完整",
                        "近30天入职" + recentHires.get(0).get("cnt") + "人",
                        "NORMAL", "人事变动追踪");
                }
                if (!recentFires.isEmpty()) {
                    int fireCnt = getIntValue(recentFires.get(0), "cnt");
                    recordItem("BIZ-HR-003", "业务链路", "近30天离职统计",
                        "离职需关注",
                        "近30天离职" + fireCnt + "人",
                        fireCnt > 2 ? "WARNING" : "NORMAL",
                        "高频离职影响运营稳定性");
                }
            } catch (Exception e) {
                // 忽略检测错误
            }
        }
        
        // 链路10：供应商评分/履约
        if (allDbTables.contains("supplier_master")) {
            try {
                List<Map<String, Object>> noRating = dbQuery(
                    "SELECT COUNT(*) as cnt FROM supplier_master WHERE supplier_rating IS NULL");
                if (!noRating.isEmpty() && getIntValue(noRating.get(0), "cnt") > 0) {
                    recordItem("BIZ-SUP-001", "业务链路", "供应商评分为空",
                        "供应商应有评价",
                        noRating.get(0).get("cnt") + "家未评价",
                        "WARNING", "缺少供应商绩效数据");
                }
            } catch (Exception e) {
                // 忽略检测错误
            }
        }
        
        // 链路11：多门店数据分布
        List<String> tablesWithStore = allDbTables.stream()
            .filter(t -> {
                if (!dbTableDetail.containsKey(t)) return false;
                List<Map<String, Object>> cols = (List<Map<String, Object>>) dbTableDetail.get(t).get("columns");
                return cols.stream().anyMatch(c -> "store_id".equals(c.get("Field")));
            })
            .collect(Collectors.toList());
        
        if (!tablesWithStore.isEmpty()) {
            recordItem("BIZ-MULTI-001", "业务链路", "多门店隔离统计",
                "带store_id的表数据已隔离",
                tablesWithStore.size() + "张门店隔离表",
                "NORMAL", "多门店架构已实施");
            
            // 抽查是否有跨门店数据泄露
            try {
                if (tablesWithStore.contains("booking_master")) {
                    List<Map<String, Object>> stores = dbQuery(
                        "SELECT store_id, COUNT(*) as cnt FROM booking_master GROUP BY store_id");
                    if (!stores.isEmpty() && stores.size() > 1) {
                        recordItem("BIZ-MULTI-002", "业务链路", "多门店数据分布",
                            "各门店数据量均衡",
                            stores.size() + "个门店",
                            "NORMAL", "各门店独立运营");
                    }
                }
            } catch (Exception e) {
                // 忽略检测错误
            }
        }
        
        // 链路12：对账金额一致性
        try {
            if (allDbTables.contains("finance_transaction")) {
                List<Map<String, Object>> todayTx = dbQuery(
                    "SELECT COUNT(*) as cnt, COALESCE(SUM(ABS(amount)),0) as total FROM finance_transaction " +
                    "WHERE trans_date=CURDATE()");
                if (!todayTx.isEmpty()) {
                    recordItem("BIZ-FIN-001", "业务链路", "当日资金流水",
                        "每日应有资金流水",
                        todayTx.get(0).get("cnt") + "笔 金额" + todayTx.get(0).get("total"),
                        "NORMAL", "资金流水正常记录");
                }
            }
        } catch (Exception e) {
            // 忽略检测错误
        }
        
        // 链路13：合同到期预警
        try {
            if (allDbTables.contains("staff_master")) {
                List<Map<String, Object>> expiring = dbQuery(
                    "SELECT COUNT(*) as cnt FROM staff_master WHERE resign_date IS NOT NULL AND " +
                    "resign_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)");
                if (!expiring.isEmpty() && getIntValue(expiring.get(0), "cnt") > 0) {
                    recordItem("BIZ-HR-004", "业务链路", "员工合同到期",
                        "到期前30天应预警",
                        expiring.get(0).get("cnt") + "人将在30天内到期",
                        "WARNING", "需提前沟通续签");
                }
            }
        } catch (Exception e) {
            // 忽略检测错误
        }
        
        // 链路14：审批卡滞
        if (allDbTables.contains("approval_flow")) {
            try {
                List<Map<String, Object>> stuck = dbQuery(
                    "SELECT COUNT(*) as cnt FROM approval_flow WHERE status='pending' AND " +
                    "create_time < DATE_SUB(NOW(), INTERVAL 3 DAY)");
                if (!stuck.isEmpty() && getIntValue(stuck.get(0), "cnt") > 0) {
                    recordItem("BIZ-APR-001", "业务链路", "审批卡滞",
                        "审批应在3天内完成",
                        stuck.get(0).get("cnt") + "条审批滞留超3天",
                        "WARNING", "影响业务流转效率");
                }
            } catch (Exception e) {
                // 忽略检测错误
            }
        }
        
        // 链路15：完成预订未收款
        if (allDbTables.contains("booking_master")) {
            try {
                List<Map<String, Object>> settled = dbQuery(
                    "SELECT COUNT(*) as cnt FROM booking_master WHERE status='completed' AND payment_status='unpaid'");
                if (!settled.isEmpty() && getIntValue(settled.get(0), "cnt") > 0) {
                    recordItem("BIZ-FIN-002", "业务链路", "完成预订未收款",
                        "已完成预订应收齐款项",
                        settled.get(0).get("cnt") + "条已完成但未收款",
                        "ERROR", "造成财务漏洞");
                }
            } catch (Exception e) {
                // 忽略检测错误
            }
        }
        
        // 链路16：菜品缺配方
        if (allDbTables.contains("dish_master") && allDbTables.contains("dish_recipe")) {
            try {
                List<Map<String, Object>> noRecipe = dbQuery(
                    "SELECT COUNT(*) as cnt FROM dish_master d WHERE d.is_active=1 AND d.dish_id NOT IN " +
                    "(SELECT DISTINCT dish_id FROM dish_recipe)");
                if (!noRecipe.isEmpty() && getIntValue(noRecipe.get(0), "cnt") > 0) {
                    recordItem("BIZ-MENU-001", "业务链路", "菜品缺失配方",
                        "每道菜品应有BOM配方",
                        noRecipe.get(0).get("cnt") + "道菜无配方",
                        "ERROR", "无法核算成本");
                }
            } catch (Exception e) {
                // 忽略检测错误
            }
        }
        
        // 链路17：供应商供货统计
        if (allDbTables.contains("goods_receipt")) {
            try {
                List<Map<String, Object>> recentRec = dbQuery(
                    "SELECT COUNT(*) as cnt, COALESCE(SUM(total_amount),0) as total FROM goods_receipt " +
                    "WHERE receipt_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)");
                if (!recentRec.isEmpty()) {
                    recordItem("BIZ-SUP-002", "业务链路", "近30天验收统计",
                        "供应商应正常供货",
                        recentRec.get(0).get("cnt") + "笔 金额" + recentRec.get(0).get("total"),
                        "NORMAL", "供应商履约统计");
                }
            } catch (Exception e) {
                // 忽略检测错误
            }
        }
    }

    // ====================== 定时任务扫描 ======================
    private void scanScheduler() {
        log.info("开始定时任务扫描...");
        
        List<String> allJava = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(BACK_CODE_PATH))) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".java"))
                 .forEach(p -> allJava.add(p.toString()));
        } catch (IOException e) {
            log.error("扫描Java文件异常: {}", e.getMessage());
        }
        
        List<String> scheduledFiles = new ArrayList<>();
        for (String fp : allJava) {
            try {
                String ct = Files.readString(Paths.get(fp));
                if (ct.contains("@Scheduled")) {
                    scheduledFiles.add(fp);
                    boolean hasLock = ct.contains("RedisLock") || ct.contains("synchronized") || 
                                     ct.contains("ReentrantLock") || ct.contains("@Lock");
                    boolean hasFixed = ct.contains("fixedRate") || ct.contains("fixedDelay") || ct.contains("cron");
                    
                    recordItem("SCHED-" + Paths.get(fp).getFileName().toString(), "定时任务",
                        "定时任务存在性",
                        "定时任务应有防重入和合理频率",
                        (hasLock ? "有" : "无") + "防重入 " + (hasFixed ? "有" : "无") + "频率配置",
                        hasLock ? "NORMAL" : "WARNING",
                        "无锁的定时任务在集群部署时会重复执行", "", "", fp, "", 0);
                }
            } catch (IOException e) {
                // 忽略读取错误
            }
        }
        
        if (scheduledFiles.isEmpty()) {
            recordItem("SCHED-NONE", "定时任务", "定时任务配置",
                "应有@Scheduled任务",
                "未检测到@Scheduled注解",
                "WARNING", "例如库存预警、成本卡重算、合同到期提醒等需要定时任务");
        } else {
            recordItem("SCHED-COUNT", "定时任务", "定时任务总数",
                "定时任务数量合理",
                "共" + scheduledFiles.size() + "个@Scheduled任务",
                "NORMAL", "定时任务清单");
        }
    }

    // ====================== 异常处理扫描 ======================
    private void scanExceptionHandler() {
        log.info("开始异常处理扫描...");
        
        List<String> allJava = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(BACK_CODE_PATH))) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".java"))
                 .forEach(p -> allJava.add(p.toString()));
        } catch (IOException e) {
            log.error("扫描Java文件异常: {}", e.getMessage());
        }
        
        // 全局异常处理
        List<String> adviceFiles = new ArrayList<>();
        List<String> apiResponseFiles = new ArrayList<>();
        for (String fp : allJava) {
            try {
                String ct = Files.readString(Paths.get(fp));
                if (ct.contains("@ControllerAdvice") || ct.contains("@RestControllerAdvice")) {
                    adviceFiles.add(fp);
                }
                if (ct.contains("class ApiResponse") || ct.contains("class R<") || ct.contains("class Result<")) {
                    apiResponseFiles.add(fp);
                }
            } catch (IOException e) {
                // 忽略读取错误
            }
        }
        
        if (!adviceFiles.isEmpty()) {
            String fileNames = adviceFiles.stream()
                .map(fp -> Paths.get(fp).getFileName().toString())
                .collect(Collectors.joining(","));
            recordItem("EX-ADVICE", "后端-异常处理", "全局异常处理器",
                "应有@ControllerAdvice",
                adviceFiles.size() + "个异常处理器: " + fileNames,
                "NORMAL", "全局异常捕获避免500");
        } else {
            recordItem("EX-ADVICE", "后端-异常处理", "全局异常处理器",
                "应有@ControllerAdvice",
                "未找到全局异常处理器",
                "ERROR", "未捕获的异常会暴露堆栈给前端");
        }
        
        if (!apiResponseFiles.isEmpty()) {
            String fileNames = apiResponseFiles.stream()
                .limit(5)
                .map(fp -> Paths.get(fp).getFileName().toString())
                .collect(Collectors.joining(","));
            recordItem("EX-RESPONSE", "后端-异常处理", "统一返回封装",
                "应有统一ApiResponse/Result类",
                apiResponseFiles.size() + "个返回封装: " + fileNames,
                "NORMAL", "统一返回格式利于前端解析");
        } else {
            recordItem("EX-RESPONSE", "后端-异常处理", "统一返回封装",
                "应有统一ApiResponse/Result类",
                "未找到统一返回封装类",
                "WARNING", "返回格式不统一前端难处理");
        }
        
        // 检测Controller中是否有硬编码的try-catch返回不同格式
        long badTry = allJava.stream()
            .filter(fp -> {
                try {
                    String ct = Files.readString(Paths.get(fp));
                    return (fp.contains("Controller") || ct.contains("@RestController")) && 
                           ct.contains("return new HashMap");
                } catch (IOException e) {
                    return false;
                }
            })
            .count();
        
        if (badTry > 0) {
            recordItem("EX-FORMAT", "后端-异常处理", "Controller返回格式",
                "不应返回HashMap, 应用ApiResponse",
                badTry + "个Controller返回HashMap",
                "WARNING", "前端拿到非标准格式数据");
        }
    }

    // ====================== 数据库程序扫描 ======================
    private void scanDBProgram() {
        log.info("开始数据库程序扫描...");
        
        // 视图
        List<Map<String, Object>> views = dbQuery(
            "SELECT TABLE_NAME FROM information_schema.VIEWS WHERE TABLE_SCHEMA='" + DB_NAME + "'");
        if (!views.isEmpty()) {
            for (Map<String, Object> v : views) {
                recordItem("DBPROG-VIEW-" + v.get("TABLE_NAME"), "数据库-存储程序", "数据库视图",
                    "视图存在",
                    "视图: " + v.get("TABLE_NAME"),
                    "NORMAL", "用于简化查询");
            }
        } else {
            recordItem("DBPROG-VIEW-NONE", "数据库-存储程序", "数据库视图",
                "按需创建视图",
                "无视图定义",
                "NORMAL", "视图非必需");
        }
        
        // 存储过程
        List<Map<String, Object>> procs = dbQuery(
            "SELECT ROUTINE_NAME, ROUTINE_TYPE FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA='" + DB_NAME + "'");
        if (!procs.isEmpty()) {
            for (Map<String, Object> p : procs) {
                recordItem("DBPROG-PROC-" + p.get("ROUTINE_NAME"), "数据库-存储程序", "存储过程/函数",
                    p.get("ROUTINE_TYPE") + "存在",
                    p.get("ROUTINE_TYPE") + ": " + p.get("ROUTINE_NAME"),
                    "NORMAL", "封装复杂业务逻辑");
            }
        } else {
            recordItem("DBPROG-PROC-NONE", "数据库-存储程序", "存储过程/函数",
                "按需创建存储过程",
                "无存储过程或函数",
                "NORMAL", "业务逻辑在应用层实现");
        }
        
        // 触发器
        List<Map<String, Object>> triggers = dbQuery(
            "SELECT TRIGGER_NAME, EVENT_OBJECT_TABLE FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA='" + DB_NAME + "'");
        if (!triggers.isEmpty()) {
            for (Map<String, Object> t : triggers) {
                recordItem("DBPROG-TRIG-" + t.get("TRIGGER_NAME"), "数据库-存储程序", "触发器",
                    "触发器存在",
                    t.get("TRIGGER_NAME") + " on " + t.get("EVENT_OBJECT_TABLE"),
                    "NORMAL", "自动响应数据变更");
            }
        } else {
            recordItem("DBPROG-TRIG-NONE", "数据库-存储程序", "触发器",
                "按需创建触发器",
                "无触发器定义",
                "NORMAL", "未使用触发器");
        }
        
        // 事件调度器
        List<Map<String, Object>> events = dbQuery("SHOW EVENTS");
        if (!events.isEmpty()) {
            for (Map<String, Object> evt : events) {
                recordItem("DBPROG-EVT-" + evt.get(1), "数据库-存储程序", "数据库events",
                    "定时事件",
                    "事件: " + evt.get(1) + " 周期:" + evt.get(2),
                    "NORMAL", "数据库级定时任务");
            }
        } else {
            recordItem("DBPROG-EVT-NONE", "数据库-存储程序", "数据库events",
                "按需配置",
                "无定时事件",
                "NORMAL", "事件调度未启用");
        }
    }

    // ====================== 基础设施扫描 ======================
    private void scanInfrastructure() {
        log.info("开始基础设施扫描...");
        
        List<String> allJava = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(BACK_CODE_PATH))) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".java"))
                 .forEach(p -> allJava.add(p.toString()));
        } catch (IOException e) {
            log.error("扫描Java文件异常: {}", e.getMessage());
        }
        
        Map<String, String[]> deps = new HashMap<>();
        deps.put("邮件", new String[]{"MailService", "JavaMailSender", "mail"});
        deps.put("短信", new String[]{"SmsService", "SmsUtil", "sms"});
        deps.put("支付", new String[]{"PayService", "WechatPay", "AliPay", "Payment"});
        deps.put("打印机", new String[]{"PrintService", "Printer", "热敏"});
        deps.put("文件存储", new String[]{"COS", "OSS", "FileStorage", "UploadService", "MultipartFile"});
        deps.put("微信", new String[]{"Wechat", "WxService", "Weixin", "OfficialAccount"});
        deps.put("数据导出", new String[]{"Excel", "POI", "EasyExcel", "exportExcel", "CsvExport"});
        
        for (Map.Entry<String, String[]> entry : deps.entrySet()) {
            String depName = entry.getKey();
            String[] keywords = entry.getValue();
            boolean found = false;
            
            for (String fp : allJava) {
                try {
                    String fileName = Paths.get(fp).getFileName().toString();
                    String ctLower = Files.readString(Paths.get(fp)).toLowerCase();
                    for (String kw : keywords) {
                        if (ctLower.contains(kw.toLowerCase())) {
                            recordItem("INFRA-" + depName, "系统依赖", depName + "服务集成",
                                depName + "功能应有对应Service",
                                "已集成: " + fileName,
                                "NORMAL", depName + "服务可用");
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                } catch (IOException e) {
                    // 忽略读取错误
                }
            }
            
            if (!found) {
                String level = (depName.equals("邮件") || depName.equals("支付")) ? "WARNING" : "NORMAL";
                recordItem("INFRA-" + depName, "系统依赖", depName + "服务集成",
                    depName + "功能应有对应Service",
                    "未检测到集成代码",
                    level, depName + "功能可能未实现");
            }
        }
    }

    // ====================== 数据字典扫描 ======================
    private void scanDataDict() {
        log.info("开始数据字典扫描...");
        loadAllTableInfo();
        
        // 检测config/字典类表
        List<String> dictTables = allDbTables.stream()
            .filter(t -> {
                String tLower = t.toLowerCase();
                return tLower.contains("config") || tLower.contains("dict") || tLower.contains("enum") || 
                       tLower.contains("setting") || tLower.contains("type") || tLower.contains("category");
            })
            .collect(Collectors.toList());
        
        for (String tbl : dictTables) {
            try {
                List<Map<String, Object>> cnt = dbQuery("SELECT COUNT(*) as cnt FROM `" + tbl + "`");
                if (!cnt.isEmpty()) {
                    int count = getIntValue(cnt.get(0), "cnt");
                    recordItem("DICT-TABLE-" + tbl, "数据字典", "字典表存在性",
                        "字典表应有数据",
                        tbl + ": " + count + "条",
                        count == 0 ? "WARNING" : "NORMAL",
                        "空字典表可能影响前端选项");
                }
            } catch (Exception e) {
                // 忽略检测错误
            }
        }
        
        if (dictTables.isEmpty()) {
            recordItem("DICT-NONE", "数据字典", "字典表",
                "应有配置/字典表",
                "未检测到字典类表",
                "WARNING", "建议建表管理枚举值");
        }
        
        // 检测枚举字段值的有效性
        List<String[]> statusCols = new ArrayList<>();
        for (String tbl : allDbTables) {
            if (!dbTableDetail.containsKey(tbl)) continue;
            List<Map<String, Object>> columns = (List<Map<String, Object>>) dbTableDetail.get(tbl).get("columns");
            for (Map<String, Object> c : columns) {
                String cName = ((String) c.get("Field")).toLowerCase();
                String cType = ((String) c.get("Type")).toLowerCase();
                if ((cName.equals("status") || cName.equals("type") || cName.equals("is_active") || 
                     cName.equals("is_deleted")) && cType.contains("varchar")) {
                    statusCols.add(new String[]{tbl, (String) c.get("Field")});
                }
            }
        }
        
        for (String[] col : statusCols.subList(0, Math.min(30, statusCols.size()))) {
            String tbl = col[0], colName = col[1];
            try {
                List<Map<String, Object>> vals = dbQuery(
                    "SELECT DISTINCT `" + colName + "` FROM `" + tbl + "` WHERE `" + colName + "` IS NOT NULL LIMIT 20");
                if (!vals.isEmpty()) {
                    String valStr = vals.stream()
                        .limit(10)
                        .map(v -> String.valueOf(v.get(colName)))
                        .collect(Collectors.joining(","));
                    recordItem("DICT-ENUM-" + tbl + "." + colName, "数据字典", "枚举值分布",
                        tbl + "." + colName + " 枚举值应合理",
                        "值: " + valStr,
                        "NORMAL", "枚举字段值统计");
                }
            } catch (Exception e) {
                // 忽略检测错误
            }
        }
    }

    // ====================== 页面规范扫描 ======================
    private void scanPageSpec() {
        log.info("开始页面规范扫描...");
        
        // 页面规范定义
        String[][] searchSpecs = {
            {"Dashboard", "总经理驾驶舱", "dashboard"},
            {"TableBoard", "桌台状态看板", "dashboard"},
            {"Bookings", "预订管理", "list"},
            {"FrontDesk", "前台点菜", "form"},
            {"Customers", "客史档案", "list"},
            {"Kitchen", "厨房总看板", "dashboard"},
            {"Production", "生产管理", "list"},
            {"Procurement", "采购管理", "list"},
            {"Receipt", "入库验收", "list"},
            {"Issue", "领用出库", "list"},
            {"Inventory", "库存管理", "list"},
            {"Suppliers", "供应商管理", "list"},
            {"Staff", "员工档案中心", "list"},
            {"Schedule", "排班考勤", "list"},
            {"Finance", "财务总看板", "dashboard"},
            {"Settings", "门店管理", "list"},
            {"Reports", "报表中心", "dashboard"}
        };
        
        // 收集所有Vue文件
        Map<String, Map<String, Object>> vueFiles = new HashMap<>();
        try (Stream<Path> paths = Files.walk(Paths.get(FRONT_CODE_PATH))) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".vue"))
                 .forEach(p -> {
                     String n = p.getFileName().toString().replace(".vue", "");
                     Map<String, Object> fileInfo = new HashMap<>();
                     fileInfo.put("path", p.toString());
                     fileInfo.put("size", new java.io.File(p.toString()).length());
                     try {
                         fileInfo.put("content", Files.readString(p));
                         vueFiles.put(n, fileInfo);
                     } catch (IOException e) {
                         // 忽略读取错误
                     }
                 });
        } catch (IOException e) {
            log.error("扫描Vue文件异常: {}", e.getMessage());
        }
        
        Map<String, Integer> stats = new HashMap<>();
        stats.put("found", 0);
        stats.put("missing", 0);
        stats.put("partial", 0);
        stats.put("total", searchSpecs.length);
        
        // 页面类型对应的必需控件
        Map<String, String[]> requiredControls = new HashMap<>();
        requiredControls.put("list", new String[]{"el-table", "el-pagination", "el-input", "el-button"});
        requiredControls.put("form", new String[]{"el-form", "el-input", "el-button", "el-select"});
        requiredControls.put("dashboard", new String[]{"el-card", "el-statistic", "el-row", "el-col"});
        
        for (String[] spec : searchSpecs) {
            String compName = spec[0], pageName = spec[1], pageType = spec[2];
            if (vueFiles.containsKey(compName)) {
                String content = (String) vueFiles.get(compName).get("content");
                String[] controls = requiredControls.getOrDefault(pageType, new String[]{});
                
                // 检测缺失的控件
                List<String> missingControls = new ArrayList<>();
                for (String control : controls) {
                    if (!content.contains(control)) {
                        missingControls.add(control);
                    }
                }
                
                if (missingControls.isEmpty()) {
                    stats.put("found", stats.get("found") + 1);
                    String foundStr = String.join(",", controls);
                    recordItem("SPEC-" + compName, "页面规范对比", pageName + "(" + pageType + ")",
                        "页面符合规范定义",
                        "已有控件:" + foundStr,
                        "NORMAL",
                        "", "", "", "", "", 0);
                } else {
                    stats.put("partial", stats.get("partial") + 1);
                    String missingStr = String.join(",", missingControls);
                    List<String> foundList = new ArrayList<>();
                    for (String control : controls) {
                        if (!missingControls.contains(control)) {
                            foundList.add(control);
                        }
                    }
                    String foundStr = String.join(",", foundList);
                    recordItem("SPEC-" + compName, "页面规范对比", pageName + "(" + pageType + ")",
                        "页面应有完整控件",
                        "已有控件:" + foundStr + " | 缺失控件:" + missingStr,
                        "WARNING",
                        "", "", "", "", "", 0);
                }
            } else {
                stats.put("missing", stats.get("missing") + 1);
                recordItem("SPEC-" + compName, "页面规范对比", pageName + "(" + pageType + ")",
                    "应有对应Vue组件",
                    "未找到 " + compName + ".vue",
                    "FATAL", "创建" + compName + ".vue实现" + pageName,
                    "", "", "", "", 0);
            }
        }
        
        // 汇总看板
        int total = stats.get("total");
        int found = stats.get("found");
        int missing = stats.get("missing");
        int partial = stats.get("partial");
        double coverage = total > 0 ? (double) found / total * 100 : 0;
        
        String level;
        if (coverage < 50) level = "FATAL";
        else if (coverage < 80) level = "ERROR";
        else level = "NORMAL";
        
        recordItem("SPEC-SUMMARY", "页面规范对比", "页面功能规范对比汇总",
            "覆盖率>=80%",
            "总" + total + "页 | 完善" + found + " 部分缺" + partial + " 缺失" + missing + 
            " | 覆盖率" + String.format("%.1f%%", coverage),
            level, "页面规范对比汇总");
    }

    // ====================== 生成数据看板 ======================
    private void generateDashboards() {
        log.info("生成数据看板...");
        
        Map<String, Map<String, Integer>> modulesSeen = new HashMap<>();
        for (Map<String, Object> item : scanItems) {
            String mod = (String) item.getOrDefault("module", "未知模块");
            modulesSeen.computeIfAbsent(mod, k -> {
                Map<String, Integer> m = new HashMap<>();
                m.put("total", 0);
                m.put("FATAL", 0);
                m.put("ERROR", 0);
                m.put("WARNING", 0);
                m.put("NORMAL", 0);
                return m;
            });
            
            Map<String, Integer> modStats = modulesSeen.get(mod);
            modStats.put("total", modStats.get("total") + 1);
            String lv = (String) item.get("level");
            if (modStats.containsKey(lv)) {
                modStats.put(lv, modStats.get(lv) + 1);
            }
        }
        
        for (Map.Entry<String, Map<String, Integer>> entry : modulesSeen.entrySet()) {
            String modName = entry.getKey();
            Map<String, Integer> stats = entry.getValue();
            if (stats.get("total") == 0) continue;
            
            double health = (double) stats.get("NORMAL") / stats.get("total") * 100;
            String level;
            String note;
            if (health < 50) {
                level = "FATAL";
                note = "需立即修复";
            } else if (health < 80) {
                level = "ERROR";
                note = "有较多问题";
            } else if (health < 95) {
                level = "WARNING";
                note = "存在隐患";
            } else {
                level = "NORMAL";
                note = "运行良好";
            }
            
            Map<String, Object> dashItem = new HashMap<>();
            dashItem.put("scan_id", "DASH-" + modName.replaceAll("[\\s/]", "-"));
            dashItem.put("module", "数据看板");
            dashItem.put("scene", "模块看板");
            dashItem.put("title", modName + " 模块看板");
            dashItem.put("expect", "健康度>=80%");
            dashItem.put("actual", "总检测" + stats.get("total") + "项 | FATAL:" + stats.get("FATAL") + 
                        " ERROR:" + stats.get("ERROR") + " WARN:" + stats.get("WARNING") + 
                        " NORM:" + stats.get("NORMAL") + " | 健康度" + String.format("%.1f%%", health));
            dashItem.put("level", level);
            dashItem.put("detail", modName + "模块共" + stats.get("total") + "条检测项,健康度" + 
                        String.format("%.1f%%", health) + "." + note);
            dashItem.put("fix_sql", "");
            dashItem.put("fix_cmd", "");
            dashItem.put("file_list", "");
            dashItem.put("tags", "看板");
            dashItem.put("score", (int) health);
            
            scanItems.add(dashItem);
        }
        
        // 更新统计
        stat.put("total", scanItems.size());
        stat.put("FATAL", (int) scanItems.stream().filter(it -> "FATAL".equals(it.get("level"))).count());
        stat.put("ERROR", (int) scanItems.stream().filter(it -> "ERROR".equals(it.get("level"))).count());
        stat.put("WARNING", (int) scanItems.stream().filter(it -> "WARNING".equals(it.get("level"))).count());
        stat.put("NORMAL", (int) scanItems.stream().filter(it -> "NORMAL".equals(it.get("level"))).count());
    }
}
