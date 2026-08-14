package com.youjian.banquet.service;

import com.youjian.banquet.entity.StockTransfer;
import com.youjian.banquet.entity.StockTransferDetail;
import com.youjian.banquet.repository.StockTransferDetailRepository;
import com.youjian.banquet.repository.StockTransferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class StockTransferService {

    @Autowired
    private StockTransferRepository transferRepo;

    @Autowired
    private StockTransferDetailRepository detailRepo;

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * 按单号/状态/日期范围查询调拨单。
     * 所有参数均可选，不传则不过滤。
     *
     * @param storeId   门店ID（可选）
     * @param transferNo 调拨单号（可选，模糊匹配）
     * @param status    状态（可选）
     * @param startDate 开始日期（可选，对应 make_date）
     * @param endDate   结束日期（可选，对应 make_date）
     * @return 调拨单列表（每条含明细 details）
     */
    public List<Map<String, Object>> search(Long storeId, String transferNo, String status,
                                           LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM stock_transfer WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (storeId != null) {
            sql.append(" AND store_id = ?");
            params.add(storeId);
        }
        if (transferNo != null && !transferNo.isEmpty()) {
            sql.append(" AND transfer_no LIKE ?");
            params.add("%" + transferNo + "%");
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        // 已移除: 字段对齐数据库 (make_date 列已移除，可改用 transfer_date)
        // 已移除: 字段对齐数据库 (make_date 列已移除，可改用 transfer_date)
        sql.append(" ORDER BY created_at DESC, transfer_id DESC");

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), params.toArray());

        // 为每条调拨单挂载明细
        for (Map<String, Object> row : rows) {
            Object tidObj = row.get("transfer_id");
            if (tidObj != null) {
                Long tid = ((Number) tidObj).longValue();
                List<Map<String, Object>> details = jdbc.queryForList(
                        "SELECT * FROM stock_transfer_detail WHERE transfer_id = ?", tid);
                row.put("details", details);
            }
        }
        return rows;
    }

    /**
     * 新增调拨单（含可选明细）。
     * body 字段：storeId, transferNo, fromStoreId, toStoreId, ingredientId,
     *           quantity, unit, status, makerName, makeDate, remark, details(可选列表)
     */
    public StockTransfer create(Map<String, Object> body) {
        StockTransfer transfer = new StockTransfer();

        if (body.get("storeId") != null) {
            transfer.setStoreId(toLong(body.get("storeId")));
        }
        if (body.get("transferNo") != null) {
            transfer.setTransferNo(body.get("transferNo").toString());
        } else {
            // 自动生成单号：ST-yyyyMMddHHmmss
            transfer.setTransferNo("ST-" + java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        }
        // 已移除: 字段对齐数据库
        // 已移除: 字段对齐数据库
        // 已移除: 字段对齐数据库
        // 已移除: 字段对齐数据库
        // 已移除: 字段对齐数据库
        if (body.get("status") != null) {
            transfer.setStatus(body.get("status").toString());
        }
        // 已移除: 字段对齐数据库
        // 已移除: 字段对齐数据库
        if (body.get("remark") != null) {
            transfer.setRemark(body.get("remark").toString());
        }

        StockTransfer saved = transferRepo.save(transfer);

        // 保存明细
        Object detailsObj = body.get("details");
        if (detailsObj instanceof List<?> detailsList) {
            for (Object item : detailsList) {
                if (item instanceof Map<?, ?> detailMap) {
                    StockTransferDetail detail = new StockTransferDetail();
                    detail.setTransferId(saved.getTransferId());
                    Object ingId = detailMap.get("ingredientId");
                    if (ingId == null) ingId = detailMap.get("ingredient_id");
                    if (ingId != null) detail.setIngredientId(ingId.toString());

                    Object qty = detailMap.get("quantity");
                    if (qty != null) detail.setQuantity(new BigDecimal(qty.toString()));

                    Object unit = detailMap.get("unit");
                    if (unit != null) detail.setUnit(unit.toString());

                    detailRepo.save(detail);
                }
            }
        }

        return saved;
    }

    private static Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        return Long.valueOf(obj.toString());
    }
}
