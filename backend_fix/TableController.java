package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BanquetTable;
import com.youjian.banquet.repository.BanquetTableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TableController {

    @Autowired private BanquetTableRepository tableRepo;

    /** GET /api/tables — get all tables, with optional area filter */
    @GetMapping("/tables")
    public Result<List<BanquetTable>> getTables(@RequestParam(defaultValue = "1") Long storeId,
                                                @RequestParam(required = false) String area) {
        try {
            List<BanquetTable> list;
            if (area != null && !area.isEmpty() && !"all".equals(area)) {
                list = tableRepo.findByTableAreaAndStoreIdOrderBySortOrder(area, storeId);
            } else {
                list = tableRepo.findByStoreIdOrderBySortOrder(storeId);
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "获取桌台列表失败: " + e.getMessage());
        }
    }

    /** PUT /api/tables/{id}/status — update table status */
    @PutMapping("/tables/{id}/status")
    public Result<BanquetTable> updateStatus(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        try {
            BanquetTable t = tableRepo.findById(id).orElse(null);
            if (t == null) return Result.error(404, "桌台不存在");
            t.setTableStatus(body.getOrDefault("status", "available"));
            tableRepo.save(t);
            return Result.success(t);
        } catch (Exception e) {
            return Result.error(500, "更新状态失败: " + e.getMessage());
        }
    }

    /** PUT /api/tables/reorder — reorder tables (drag-and-drop) */
    @PutMapping("/tables/reorder")
    public Result<?> reorderTables(@RequestBody List<Map<String, Object>> orderList) {
        try {
            for (int i = 0; i < orderList.size(); i++) {
                Map<String, Object> item = orderList.get(i);
                Object idObj = item.get("table_id");
                if (idObj == null) idObj = item.get("tableId");
                if (idObj == null) continue;
                Integer id = idObj instanceof Number ? ((Number) idObj).intValue() : Integer.valueOf(idObj.toString());
                final int sort = i;
                tableRepo.findById(id).ifPresent(t -> { t.setSortOrder(sort); tableRepo.save(t); });
            }
            return Result.success("排序已更新");
        } catch (Exception e) {
            return Result.error(500, "排序失败: " + e.getMessage());
        }
    }

    /** POST /api/tables — add new table (用Map接收，防御snake_case/camelCase) */
    @PostMapping("/tables")
    public Result<BanquetTable> addTable(@RequestBody Map<String, Object> body) {
        try {
            BanquetTable table = new BanquetTable();
            table.setTableId(null);
            table.setIsActive(1);
            table.setTableStatus("available");

            // storeId（防御snake_case/camelCase）
            Object storeIdObj = body.get("storeId");
            if (storeIdObj == null) storeIdObj = body.get("store_id");
            if (storeIdObj == null) storeIdObj = 1;
            table.setStoreId(Long.valueOf(storeIdObj.toString()));

            // tableNumber
            Object tableNumberObj = body.get("tableNumber");
            if (tableNumberObj == null) tableNumberObj = body.get("table_number");
            if (tableNumberObj != null) table.setTableNumber(tableNumberObj.toString());

            // tableName
            Object tableNameObj = body.get("tableName");
            if (tableNameObj == null) tableNameObj = body.get("table_name");
            if (tableNameObj != null) table.setTableName(tableNameObj.toString());
            else if (tableNumberObj != null) table.setTableName(tableNumberObj.toString());

            // tableArea
            Object tableAreaObj = body.get("tableArea");
            if (tableAreaObj == null) tableAreaObj = body.get("table_area");
            if (tableAreaObj != null) table.setTableArea(tableAreaObj.toString());

            // tableCapacity
            Object capObj = body.get("tableCapacity");
            if (capObj == null) capObj = body.get("table_capacity");
            if (capObj != null) table.setTableCapacity(Integer.valueOf(capObj.toString()));
            else table.setTableCapacity(10);

            // tableType
            Object typeObj = body.get("tableType");
            if (typeObj == null) typeObj = body.get("table_type");
            if (typeObj != null) table.setTableType(typeObj.toString());
            else table.setTableType("round");

            // minCapacity / maxCapacity
            Object minObj = body.get("minCapacity");
            if (minObj == null) minObj = body.get("min_capacity");
            if (minObj != null) table.setMinCapacity(Integer.valueOf(minObj.toString()));

            Object maxObj = body.get("maxCapacity");
            if (maxObj == null) maxObj = body.get("max_capacity");
            if (maxObj != null) table.setMaxCapacity(Integer.valueOf(maxObj.toString()));

            Object sortOrderObj = body.get("sortOrder");
            if (sortOrderObj == null) sortOrderObj = body.get("sort_order");
            if (sortOrderObj != null) table.setSortOrder(Integer.valueOf(sortOrderObj.toString()));
            else table.setSortOrder(0);

            Object remarkObj = body.get("remark");
            if (remarkObj != null) table.setRemark(remarkObj.toString());

            table.setCreatedAt(LocalDateTime.now());
            table.setUpdatedAt(LocalDateTime.now());

            BanquetTable saved = tableRepo.save(table);
            return Result.success(saved);
        } catch (Exception e) {
            return Result.error(500, "添加桌台失败: " + e.getMessage());
        }
    }

    /** PUT /api/tables/{id} — update table */
    @PutMapping("/tables/{id}")
    public Result<BanquetTable> updateTable(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        try {
            BanquetTable t = tableRepo.findById(id).orElse(null);
            if (t == null) return Result.error(404, "桌台不存在");

            Object tableNumberObj = body.get("tableNumber");
            if (tableNumberObj == null) tableNumberObj = body.get("table_number");
            if (tableNumberObj != null) t.setTableNumber(tableNumberObj.toString());

            Object tableNameObj = body.get("tableName");
            if (tableNameObj == null) tableNameObj = body.get("table_name");
            if (tableNameObj != null) t.setTableName(tableNameObj.toString());

            Object tableAreaObj = body.get("tableArea");
            if (tableAreaObj == null) tableAreaObj = body.get("table_area");
            if (tableAreaObj != null) t.setTableArea(tableAreaObj.toString());

            Object capObj = body.get("tableCapacity");
            if (capObj == null) capObj = body.get("table_capacity");
            if (capObj != null) t.setTableCapacity(Integer.valueOf(capObj.toString()));

            Object statusObj = body.get("tableStatus");
            if (statusObj == null) statusObj = body.get("table_status");
            if (statusObj != null) t.setTableStatus(statusObj.toString());

            t.setUpdatedAt(LocalDateTime.now());
            tableRepo.save(t);
            return Result.success(t);
        } catch (Exception e) {
            return Result.error(500, "更新桌台失败: " + e.getMessage());
        }
    }

    /** DELETE /api/tables/{id} — delete table */
    @DeleteMapping("/tables/{id}")
    public Result<?> deleteTable(@PathVariable Integer id) {
        try {
            if (!tableRepo.existsById(id)) return Result.error(404, "桌台不存在");
            tableRepo.deleteById(id);
            return Result.success("桌台已删除");
        } catch (Exception e) {
            return Result.error(500, "删除失败: " + e.getMessage());
        }
    }
}
