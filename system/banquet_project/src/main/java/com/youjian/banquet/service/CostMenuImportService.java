package com.youjian.banquet.service;

import org.apache.poi.ss.usermodel.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class CostMenuImportService {
    private static final Pattern INGREDIENT_SPLITTER = Pattern.compile("[-—、,，/＋+和及]\s*");
    private final JdbcTemplate jdbc;
    private final DataFormatter formatter = new DataFormatter(Locale.CHINA);

    public CostMenuImportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public Map<String, Object> importWorkbook(MultipartFile file, long storeId, String operator) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择 Excel 文件");
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("成本菜单.xlsx");
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) throw new IllegalArgumentException("仅支持 .xlsx 文件");
        byte[] bytes = file.getBytes();
        String hash = sha256(bytes);
        List<Long> existing = jdbc.query("SELECT import_batch_id FROM data_import_batch WHERE store_id=? AND source_hash=?", (rs, n) -> rs.getLong(1), storeId, hash);
        if (!existing.isEmpty()) return batchSummary(existing.get(0), true);

        jdbc.update("INSERT INTO data_import_batch(store_id,source_file,source_hash,status,imported_by) VALUES(?,?,?,'PROCESSING',?)",
                storeId, filename, hash, operator);
        long batchId = Objects.requireNonNull(jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
        ImportStats stats = new ImportStats();
        try (InputStream in = new java.io.ByteArrayInputStream(bytes); Workbook workbook = WorkbookFactory.create(in)) {
            importIngredients(workbook.getSheet("成本信息"), storeId, batchId, stats);
            importDishes(workbook.getSheet("菜肴信息表"), storeId, batchId, stats);
            for (Sheet sheet : workbook) {
                if (sheet.getSheetName().startsWith("NO")) importCostCard(sheet, storeId, batchId, stats);
            }
            jdbc.update("UPDATE data_import_batch SET status='COMPLETED',dish_count=?,ingredient_count=?,cost_card_count=?,draft_recipe_count=?,warning_count=?,error_count=0,imported_at=? WHERE import_batch_id=?",
                    stats.dishes, stats.ingredients, stats.formalCards, stats.drafts, stats.issues, LocalDateTime.now(), batchId);
        } catch (Exception ex) {
            jdbc.update("UPDATE data_import_batch SET status='FAILED',error_count=error_count+1,imported_at=? WHERE import_batch_id=?", LocalDateTime.now(), batchId);
            throw ex;
        }
        return batchSummary(batchId, false);
    }

    private void importIngredients(Sheet sheet, long storeId, long batchId, ImportStats stats) {
        if (sheet == null) throw new IllegalArgumentException("缺少“成本信息”工作表");
        for (int rowNo = 3; rowNo <= sheet.getLastRowNum(); rowNo++) {
            Row row = sheet.getRow(rowNo);
            String name = text(row, 1);
            if (name.isBlank()) continue;
            String unit = defaultText(text(row, 2), "克");
            BigDecimal price = decimal(row, 3);
            String id = "ING" + String.format("%06d", rowNo - 2);
            jdbc.update("INSERT INTO ingredient_master(ingredient_id,store_id,ingredient_name,purchase_unit,usage_unit,conversion_rate,avg_price,yield_rate,is_active,unit,unit_price,status) " +
                            "VALUES(?,?,?,?,?,1,?,100,1,?,?,'active') ON DUPLICATE KEY UPDATE ingredient_name=VALUES(ingredient_name),usage_unit=VALUES(usage_unit),avg_price=VALUES(avg_price),unit_price=VALUES(unit_price),is_active=1",
                    id, storeId, name, unit, unit, price, unit, price);
            jdbc.update("UPDATE ingredient_price_history SET is_active=0,effective_to=? WHERE store_id=? AND ingredient_id=? AND is_active=1", LocalDateTime.now(), storeId, id);
            jdbc.update("INSERT INTO ingredient_price_history(store_id,ingredient_id,ingredient_name,unit,unit_price,effective_from,source_type,source_id,import_batch_id,is_active) VALUES(?,?,?,?,?,?,'EXCEL_IMPORT',?,?,1)",
                    storeId, id, name, unit, price, LocalDateTime.now(), String.valueOf(batchId), batchId);
            stats.ingredients++;
        }
    }

    private void importDishes(Sheet sheet, long storeId, long batchId, ImportStats stats) {
        if (sheet == null) throw new IllegalArgumentException("缺少“菜肴信息表”工作表");
        for (int rowNo = 2; rowNo <= sheet.getLastRowNum(); rowNo++) {
            Row row = sheet.getRow(rowNo);
            String dishId = text(row, 1);
            String dishName = text(row, 2);
            if (dishId.isBlank() || dishName.isBlank()) continue;
            String category = text(row, 3);
            int spicy = integer(row, 4, 0);
            String mainType = text(row, 5);
            String main = text(row, 6);
            BigDecimal importedCost = decimal(row, 8);
            BigDecimal salePrice = decimal(row, 9);
            BigDecimal costRate = salePrice.signum() > 0
                    ? importedCost.multiply(BigDecimal.valueOf(100)).divide(salePrice, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            if (costRate.compareTo(new BigDecimal("999.99")) > 0) {
                issue(batchId, "DISH", dishId, "ABNORMAL_COST_RATE", "导入成本率超过字段范围，已标记待复核", stats);
                costRate = new BigDecimal("999.99");
            }
            jdbc.update("INSERT INTO dish_master(dish_id,store_id,dish_name,dish_category,spicy_level,main_ingredient_type,main_ingredient,english_name,cost_price,sale_price,cost_rate,cooking_time,servings,is_active,usage_type) " +
                            "VALUES(?,?,?,?,?,?,?,?,?,?,?,15,1,0,'draft') ON DUPLICATE KEY UPDATE dish_name=VALUES(dish_name),dish_category=VALUES(dish_category),main_ingredient=VALUES(main_ingredient),sale_price=VALUES(sale_price),is_active=0,usage_type='draft'",
                    dishId, storeId, dishName, category, spicy, mainType, main, text(row, 7), importedCost, salePrice, costRate);
            createDrafts(storeId, batchId, dishId, main, stats);
            stats.dishes++;
        }
    }

    private void createDrafts(long storeId, long batchId, String dishId, String main, ImportStats stats) {
        if (main.isBlank()) {
            issue(batchId, "DISH", dishId, "MISSING_MAIN_INGREDIENT", "菜品未填写主料", stats);
            return;
        }
        jdbc.update("DELETE FROM recipe_draft_detail WHERE recipe_draft_id IN (SELECT recipe_draft_id FROM recipe_draft WHERE store_id=? AND dish_id=? AND status IN ('DRAFT','UNRESOLVED'))", storeId, dishId);
        jdbc.update("DELETE FROM recipe_draft WHERE store_id=? AND dish_id=? AND status IN ('DRAFT','UNRESOLVED')", storeId, dishId);
        jdbc.update("INSERT INTO recipe_draft(store_id,dish_id,source_text,source_sheet,status,import_batch_id) VALUES(?,?,?,'菜肴信息表','DRAFT',?)",
                storeId, dishId, main, batchId);
        long draftId = Objects.requireNonNull(jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
        int line = 0;
        boolean unresolved = false;
        for (String raw : INGREDIENT_SPLITTER.split(main)) {
            String token = raw.trim();
            if (token.isBlank()) continue;
            List<Map<String, Object>> matched = jdbc.queryForList("SELECT ingredient_id,ingredient_name,usage_unit,yield_rate FROM ingredient_master WHERE store_id=? AND ingredient_name=? LIMIT 1", storeId, token);
            String ingredientId = matched.isEmpty() ? null : String.valueOf(matched.get(0).get("ingredient_id"));
            String status = ingredientId == null ? "UNMATCHED" : "MATCHED";
            Object unit = matched.isEmpty() ? null : matched.get(0).get("usage_unit");
            Object yield = matched.isEmpty() ? null : matched.get(0).get("yield_rate");
            jdbc.update("INSERT INTO recipe_draft_detail(recipe_draft_id,token,ingredient_id,ingredient_name,match_status,match_confidence,unit,yield_rate,line_no) VALUES(?,?,?,?,?,?,?,?,?)",
                    draftId, token, ingredientId, token, status, ingredientId == null ? BigDecimal.ZERO : BigDecimal.valueOf(100), unit, yield, ++line);
            if (ingredientId == null) {
                unresolved = true;
                issue(batchId, "DISH", dishId, "UNRESOLVED_INGREDIENT", "无法匹配主料：" + token, stats);
            }
        }
        jdbc.update("UPDATE recipe_draft SET status=? WHERE recipe_draft_id=?", unresolved ? "UNRESOLVED" : "DRAFT", draftId);
        stats.drafts++;
    }

    private void importCostCard(Sheet sheet, long storeId, long batchId, ImportStats stats) {
        String dishName = valueBesideLabel(sheet, "NAME OF DISH");
        String recipeNo = valueBesideLabel(sheet, "RECIPE NO.");
        if (dishName.isBlank()) return;
        String dishId = jdbc.query("SELECT dish_id FROM dish_master WHERE store_id=? AND dish_name=? LIMIT 1", rs -> rs.next() ? rs.getString(1) : null, storeId, dishName);
        if (dishId == null) {
            issue(batchId, "COST_CARD", recipeNo, "DISH_NOT_FOUND", "成本卡菜名无法匹配：" + dishName, stats);
            return;
        }
        BigDecimal sellingPrice = decimalBesideLabel(sheet, "SALES PRICE");
        jdbc.update("INSERT INTO dish_cost_card(store_id,dish_id,dish_name,standard_yield,actual_yield,yield_rate,standard_cost,selling_price,status,approval_status,effective_from,source_import_batch_id,created_by) VALUES(?,?,?,1,1,100,0,?,'draft','DRAFT',?,?, 'excel-import')",
                storeId, dishId, dishName, sellingPrice, LocalDateTime.now(), batchId);
        long cardId = Objects.requireNonNull(jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
        int header = findRow(sheet, "Raw Material Name");
        BigDecimal total = BigDecimal.ZERO;
        int line = 0;
        for (int r = header + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            String ingredientName = text(row, 1);
            if (ingredientName.isBlank() || ingredientName.contains("Total") || ingredientName.contains("合计")) continue;
            List<Map<String, Object>> matched = jdbc.queryForList("SELECT ingredient_id,avg_price,yield_rate FROM ingredient_master WHERE store_id=? AND ingredient_name=? LIMIT 1", storeId, ingredientName);
            if (matched.isEmpty()) {
                issue(batchId, "COST_CARD", recipeNo, "UNRESOLVED_INGREDIENT", "成本卡原料无法匹配：" + ingredientName, stats);
                continue;
            }
            Map<String, Object> ingredient = matched.get(0);
            BigDecimal price = decimal(row, 3);
            BigDecimal gross = decimal(row, 4);
            BigDecimal yield = number(ingredient.get("yield_rate"), BigDecimal.valueOf(100));
            if (yield.signum() <= 0) yield = BigDecimal.valueOf(100);
            BigDecimal net = gross.multiply(yield).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal cost = price.multiply(gross).setScale(2, RoundingMode.HALF_UP);
            total = total.add(cost);
            jdbc.update("INSERT INTO dish_cost_card_detail(store_id,cost_card_id,line_no,ingredient_id,ingredient_name,unit,standard_quantity,gross_quantity,net_quantity,unit_price,price_snapshot,yield_rate,total_cost) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    storeId, cardId, ++line, ingredient.get("ingredient_id"), ingredientName, defaultText(text(row, 2), "克"), gross, gross, net, price, price, yield, cost);
        }
        jdbc.update("UPDATE dish_cost_card SET standard_cost=? WHERE cost_card_id=?", total, cardId);
        jdbc.update("UPDATE dish_master SET cost_price=?,is_active=0,usage_type='draft' WHERE store_id=? AND dish_id=?", total, storeId, dishId);
        stats.formalCards++;
    }

    public Map<String, Object> batchSummary(long batchId, boolean duplicate) {
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM data_import_batch WHERE import_batch_id=?", batchId);
        row.put("duplicate", duplicate);
        row.put("issues", jdbc.queryForList("SELECT entity_type,source_value,issue_code,severity,message FROM data_import_issue WHERE import_batch_id=? ORDER BY issue_id", batchId));
        return row;
    }

    private void issue(long batchId, String type, String key, String code, String message, ImportStats stats) {
        jdbc.update("INSERT INTO data_import_issue(import_batch_id,sheet_name,entity_type,source_value,issue_code,severity,message) VALUES(?,'自动导入',?,?,?,?,?)", batchId, type, key, code, "WARNING", message);
        stats.issues++;
    }

    private int findRow(Sheet sheet, String needle) {
        for (Row row : sheet) for (Cell cell : row) if (formatter.formatCellValue(cell).contains(needle)) return row.getRowNum();
        return sheet.getLastRowNum();
    }

    private String valueBesideLabel(Sheet sheet, String label) {
        for (Row row : sheet) for (Cell cell : row) if (formatter.formatCellValue(cell).contains(label)) {
            for (int c = cell.getColumnIndex() + 1; c < Math.min(row.getLastCellNum(), cell.getColumnIndex() + 4); c++) {
                String value = text(row, c); if (!value.isBlank()) return value;
            }
        }
        return "";
    }

    private BigDecimal decimalBesideLabel(Sheet sheet, String label) {
        try { return new BigDecimal(valueBesideLabel(sheet, label).replaceAll("[^0-9.-]", "")); } catch (Exception e) { return BigDecimal.ZERO; }
    }
    private String text(Row row, int col) { return row == null || row.getCell(col) == null ? "" : formatter.formatCellValue(row.getCell(col)).trim(); }
    private BigDecimal decimal(Row row, int col) { try { return new BigDecimal(text(row, col).replace(",", "").replaceAll("[^0-9.-]", "")); } catch (Exception e) { return BigDecimal.ZERO; } }
    private BigDecimal percentage(Row row, int col) { String v=text(row,col).replace("%",""); try{return new BigDecimal(v);}catch(Exception e){return BigDecimal.ZERO;} }
    private int integer(Row row, int col, int fallback) { try { return decimal(row,col).intValue(); } catch (Exception e) { return fallback; } }
    private BigDecimal number(Object value, BigDecimal fallback) { try { return new BigDecimal(String.valueOf(value)); } catch(Exception e) { return fallback; } }
    private String defaultText(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private String sha256(byte[] bytes) throws Exception { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static class ImportStats { int dishes; int ingredients; int formalCards; int drafts; int issues; }
}
