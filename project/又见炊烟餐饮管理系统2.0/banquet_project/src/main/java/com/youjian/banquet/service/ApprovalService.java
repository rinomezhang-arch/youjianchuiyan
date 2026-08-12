package com.youjian.banquet.service;

import com.youjian.banquet.dto.InventoryDTO;
import com.youjian.banquet.entity.ApprovalFlow;
import com.youjian.banquet.entity.ApprovalNode;
import com.youjian.banquet.entity.ApprovalTemplate;
import com.youjian.banquet.entity.IngredientPurchase;
import com.youjian.banquet.entity.LeaveRecord;
import com.youjian.banquet.entity.Overtime;
import com.youjian.banquet.repository.ApprovalFlowRepository;
import com.youjian.banquet.repository.ApprovalNodeRepository;
import com.youjian.banquet.repository.ApprovalTemplateRepository;
import com.youjian.banquet.repository.IngredientPurchaseRepository;
import com.youjian.banquet.repository.LeaveRecordRepository;
import com.youjian.banquet.repository.OvertimeRepository;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 通用审批引擎服务。
 * <p>
 * 业务规则：
 * <ul>
 *   <li>分店单据（leave / overtime / stock_loss）自动流转至分店店长（store_manager）审批</li>
 *   <li>全局单据（purchase / expense）流转总经理（general_manager）审批</li>
 *   <li>审批通过后自动更新业务单据状态</li>
 * </ul>
 * 所有写方法均标注 {@link Transactional}，保证审批流与业务单据状态原子变更。
 */
@Service
public class ApprovalService {

    @Autowired private ApprovalFlowRepository flowRepo;
    @Autowired private ApprovalNodeRepository nodeRepo;
    @Autowired private ApprovalTemplateRepository templateRepo;
    @Autowired private LeaveRecordRepository leaveRepo;
    @Autowired private OvertimeRepository overtimeRepo;
    @Autowired private IngredientPurchaseRepository purchaseRepo;
    @Autowired private InventoryService inventoryService;
    @Autowired private JdbcTemplate jdbc;

    private static final String NODE_NAME_PREFIX = "审批节点";
    private static final String ROLE_STORE_MANAGER = "store_manager";
    private static final String ROLE_GENERAL_MANAGER = "general_manager";

    // ============================================================
    // 提交审批申请
    // ============================================================

    /**
     * 提交审批申请：创建审批流主表 + 按模板生成节点。
     *
     * @param flowType       审批类型 leave/overtime/purchase/expense/stock_loss
     * @param businessId     业务单据ID
     * @param businessNo     业务单据编号（可空）
     * @param storeId        门店ID
     * @param applicantId    申请人ID（可空）
     * @param applicantName  申请人姓名（可空）
     * @return 创建的审批流
     */
    @Transactional
    public ApprovalFlow submit(String flowType, Long businessId, String businessNo,
                               Long storeId, Integer applicantId, String applicantName) {
        if (flowType == null || businessId == null || storeId == null) {
            throw new IllegalArgumentException("flowType/businessId/storeId 不能为空");
        }
        // 若该业务单据已存在 pending 流程，直接返回（避免重复提交）
        Optional<ApprovalFlow> existing = flowRepo.findFirstByFlowTypeAndBusinessId(flowType, businessId);
        if (existing.isPresent() && "pending".equals(existing.get().getStatus())) {
            return existing.get();
        }

        ApprovalTemplate template = findTemplate(flowType, storeId);
        int nodeCount = (template != null && template.getNodeCount() != null) ? template.getNodeCount() : 1;

        ApprovalFlow flow = new ApprovalFlow();
        flow.setFlowNo(generateFlowNo(flowType));
        flow.setFlowType(flowType);
        flow.setBusinessId(businessId);
        flow.setBusinessNo(businessNo);
        flow.setApplicantId(applicantId);
        flow.setApplicantName(applicantName);
        flow.setStoreId(storeId);
        flow.setStatus("pending");
        flow.setCurrentNode(1);
        flow = flowRepo.save(flow);

        // 按模板创建各节点，节点1置为 pending，其余 pending（等待流转）
        for (int i = 1; i <= nodeCount; i++) {
            String approverRole = getApproverRole(template, i);
            Map<String, Object> approver = resolveApprover(approverRole, storeId);
            ApprovalNode node = new ApprovalNode();
            node.setFlowId(flow.getId());
            node.setNodeOrder(i);
            node.setNodeName(NODE_NAME_PREFIX + i);
            if (approver != null) {
                node.setApproverId(toInt(approver.get("staff_id")));
                node.setApproverName((String) approver.get("staff_name"));
            }
            node.setStatus("pending");
            nodeRepo.save(node);
        }
        return flow;
    }

    // ============================================================
    // 审批通过 / 驳回
    // ============================================================

    /**
     * 审批通过：通过当前节点，若为末节点则完成审批并更新业务单据状态。
     */
    @Transactional
    public ApprovalFlow approve(Long flowId, String comment) {
        ApprovalFlow flow = loadFlow(flowId);
        if (!"pending".equals(flow.getStatus())) {
            throw new IllegalStateException("当前审批流状态非 pending，无法通过: " + flow.getStatus());
        }
        assertCanApprove(flow);

        ApprovalNode currentNode = nodeRepo.findFirstByFlowIdAndNodeOrder(flowId, flow.getCurrentNode())
                .orElseThrow(() -> new IllegalStateException("当前节点不存在: " + flow.getCurrentNode()));
        if (!"pending".equals(currentNode.getStatus())) {
            throw new IllegalStateException("当前节点已处理: " + currentNode.getStatus());
        }

        currentNode.setStatus("approved");
        currentNode.setComment(comment);
        currentNode.setApprovedTime(LocalDateTime.now());
        currentNode.setApproverId(safeInt(currentNode.getApproverId(), currentStaffId()));
        currentNode.setApproverName(safeStr(currentNode.getApproverName(), currentStaffName()));
        nodeRepo.save(currentNode);

        long totalNodes = nodeRepo.countByFlowId(flowId);
        if (flow.getCurrentNode() < totalNodes) {
            // 流转至下一节点
            flow.setCurrentNode(flow.getCurrentNode() + 1);
            ApprovalNode nextNode = nodeRepo.findFirstByFlowIdAndNodeOrder(flowId, flow.getCurrentNode())
                    .orElse(null);
            if (nextNode != null) {
                nextNode.setStatus("pending");
                nodeRepo.save(nextNode);
            }
        } else {
            // 末节点通过，完成审批
            flow.setStatus("approved");
            updateBusinessStatus(flow, "approved", currentNode);
        }
        return flowRepo.save(flow);
    }

    /**
     * 审批驳回：驳回当前节点，终止审批流并更新业务单据状态为 rejected。
     */
    @Transactional
    public ApprovalFlow reject(Long flowId, String comment) {
        ApprovalFlow flow = loadFlow(flowId);
        if (!"pending".equals(flow.getStatus())) {
            throw new IllegalStateException("当前审批流状态非 pending，无法驳回: " + flow.getStatus());
        }
        assertCanApprove(flow);

        ApprovalNode currentNode = nodeRepo.findFirstByFlowIdAndNodeOrder(flowId, flow.getCurrentNode())
                .orElseThrow(() -> new IllegalStateException("当前节点不存在: " + flow.getCurrentNode()));

        currentNode.setStatus("rejected");
        currentNode.setComment(comment);
        currentNode.setApprovedTime(LocalDateTime.now());
        currentNode.setApproverId(safeInt(currentNode.getApproverId(), currentStaffId()));
        currentNode.setApproverName(safeStr(currentNode.getApproverName(), currentStaffName()));
        nodeRepo.save(currentNode);

        flow.setStatus("rejected");
        updateBusinessStatus(flow, "rejected", currentNode);
        return flowRepo.save(flow);
    }

    /**
     * 撤销审批流（申请人撤销）。
     */
    @Transactional
    public ApprovalFlow cancel(Long flowId) {
        ApprovalFlow flow = loadFlow(flowId);
        if (!"pending".equals(flow.getStatus())) {
            throw new IllegalStateException("仅 pending 状态可撤销: " + flow.getStatus());
        }
        flow.setStatus("cancelled");
        return flowRepo.save(flow);
    }

    // ============================================================
    // 查询
    // ============================================================

    /**
     * 待审批列表：总经理查全部，店长仅本店。
     */
    @Transactional(readOnly = true)
    public List<ApprovalFlow> getPendingList() {
        if (UserContext.isGeneralManager()) {
            return flowRepo.findByStatusOrderByCreatedTimeDesc("pending");
        }
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) {
            throw new SecurityException("未登录，无权查看待审批列表");
        }
        return flowRepo.findByStatusAndStoreIdOrderByCreatedTimeDesc("pending", storeId);
    }

    /**
     * 模板列表:返回所有审批模板(总经理/店长均可见,但店长只能使用本店适用的)
     */
    @Transactional(readOnly = true)
    public List<Object> listAllTemplates() {
        return templateRepo.findAll().stream().map(t -> (Object) t).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 流列表:返回所有审批流(按门店隔离)
     */
    @Transactional(readOnly = true)
    public List<ApprovalFlow> listAllFlows() {
        if (UserContext.isGeneralManager()) {
            return flowRepo.findAll();
        }
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) {
            throw new SecurityException("未登录，无权查看审批流");
        }
        // 用 sql 全量查本店
        return jdbc.query(
            "SELECT * FROM approval_flow WHERE store_id=? ORDER BY created_time DESC",
            (rs, i) -> {
                ApprovalFlow f = new ApprovalFlow();
                f.setId(rs.getLong("id"));
                f.setFlowNo(rs.getString("flow_no"));
                f.setFlowType(rs.getString("flow_type"));
                f.setBusinessId(rs.getLong("business_id"));
                f.setBusinessNo(rs.getString("business_no"));
                f.setStoreId(rs.getLong("store_id"));
                f.setStatus(rs.getString("status"));
                try { f.setCreatedTime(rs.getTimestamp("created_time").toLocalDateTime()); } catch(Exception e){}
                return f;
            },
            storeId
        );
    }

    /**
     * 历史审批记录：总经理查全部，店长仅本店。
     */
    @Transactional(readOnly = true)
    public List<ApprovalFlow> getHistory() {
        if (UserContext.isGeneralManager()) {
            return flowRepo.findByStatusNotOrderByCreatedTimeDesc("pending");
        }
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) {
            throw new SecurityException("未登录，无权查看审批历史");
        }
        return flowRepo.findByStatusNotAndStoreIdOrderByCreatedTimeDesc("pending", storeId);
    }

    /**
     * 审批详情：返回 flow + nodes。
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDetail(Long flowId) {
        ApprovalFlow flow = loadFlow(flowId);
        // 店长仅可查看本店审批流
        if (!UserContext.isGeneralManager()) {
            Long storeId = UserContext.getCurrentStoreId();
            if (storeId == null || !storeId.equals(flow.getStoreId())) {
                throw new SecurityException("无权限查看该审批流");
            }
        }
        List<ApprovalNode> nodes = nodeRepo.findByFlowIdOrderByNodeOrderAsc(flowId);
        Map<String, Object> result = new HashMap<>();
        result.put("flow", flow);
        result.put("nodes", nodes);
        return result;
    }

    /** 按业务类型 + 业务单据ID 查找审批流（供业务接入方查询） */
    @Transactional(readOnly = true)
    public Optional<ApprovalFlow> findByBusiness(String flowType, Long businessId) {
        return flowRepo.findFirstByFlowTypeAndBusinessId(flowType, businessId);
    }

    // ============================================================
    // 库存报损：提交报损审批（创建 stock_loss 记录 + 审批流）
    // ============================================================

    /**
     * 提交库存报损审批。
     * <p>
     * 报损明细（ingredientId / quantity / reason / operator）以分隔符形式记录到 stock_loss.remark，
     * 审批通过时由 {@link #executeStockLoss(ApprovalFlow)} 执行实际出库。
     *
     * @param storeId      门店ID
     * @param ingredientId 原料ID
     * @param quantity     报损数量
     * @param reason       报损原因
     * @param operator     操作人（可空，回退当前登录用户）
     * @return 创建的审批流
     */
    @Transactional
    public ApprovalFlow submitStockLoss(Long storeId, String ingredientId, BigDecimal quantity,
                                        String reason, String operator) {
        if (storeId == null || ingredientId == null || quantity == null) {
            throw new IllegalArgumentException("storeId/ingredientId/quantity 不能为空");
        }
        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException("报损数量必须大于0");
        }
        Integer applicantId = currentStaffId();
        String applicantName = operator != null ? operator : currentStaffName();
        String lossNo = "LOSS-" + System.currentTimeMillis();
        String remark = ingredientId + "|" + quantity.toPlainString() + "|"
                + (reason != null ? reason : "") + "|" + (operator != null ? operator : "");

        // 写入 stock_loss 主表（status=pending，待审批通过后执行出库）
        jdbc.update(
                "INSERT INTO stock_loss (store_id, loss_no, loss_date, loss_type, total_quantity, " +
                        "status, applicant_id, applicant_name, remark) VALUES (?, ?, ?, ?, ?, 'pending', ?, ?, ?)",
                storeId, lossNo, LocalDate.now(), "报损申请", quantity, applicantId, applicantName, remark);

        Long lossId = jdbc.queryForObject(
                "SELECT loss_id FROM stock_loss WHERE loss_no = ?", Long.class, lossNo);

        return submit("stock_loss", lossId, lossNo, storeId, applicantId, applicantName);
    }

    /**
     * 执行库存报损出库（审批通过时调用）。
     */
    private void executeStockLoss(ApprovalFlow flow) {
        Map<String, Object> row = jdbc.queryForList(
                "SELECT store_id, total_quantity, remark FROM stock_loss WHERE loss_id = ?",
                flow.getBusinessId()).stream().findFirst().orElse(null);
        if (row == null) {
            return;
        }
        String remark = row.get("remark") == null ? "" : row.get("remark").toString();
        String[] parts = remark.split("\\|", -1);
        if (parts.length < 2) {
            return;
        }
        String ingredientId = parts[0];
        BigDecimal quantity = new BigDecimal(parts[1]);
        String reason = parts.length > 2 ? parts[2] : "库存报损";
        String operator = parts.length > 3 && !parts[3].isEmpty() ? parts[3] : flow.getApplicantName();

        InventoryDTO dto = new InventoryDTO();
        dto.setIngredientId(ingredientId);
        dto.setStoreId(String.valueOf(row.get("store_id")));
        dto.setQuantity(quantity);
        dto.setReferenceId(flow.getFlowNo());
        dto.setReferenceType("STOCK_LOSS");
        dto.setOperator(operator);
        dto.setNotes("报损审批通过: " + reason);
        inventoryService.stockOut(dto);

        // 更新 stock_loss 状态
        jdbc.update(
                "UPDATE stock_loss SET status = 'approved', approve_time = NOW(), " +
                        "approver_id = ?, approver_name = ?, approve_remark = ? WHERE loss_id = ?",
                currentStaffId(), currentStaffName(), "审批流通过: " + flow.getFlowNo(), flow.getBusinessId());
    }

    // ============================================================
    // 审批通过/驳回后自动更新业务单据状态
    // ============================================================

    private void updateBusinessStatus(ApprovalFlow flow, String status, ApprovalNode node) {
        Integer approverId = node != null ? node.getApproverId() : currentStaffId();
        String approverName = node != null ? node.getApproverName() : currentStaffName();
        String remark = "审批流[" + flow.getFlowNo() + "]: " + status;
        switch (flow.getFlowType()) {
            case "leave":
                leaveRepo.findById(flow.getBusinessId().intValue()).ifPresent(l -> {
                    l.setStatus(status);
                    l.setApproverId(approverId);
                    l.setApproveTime(LocalDateTime.now());
                    l.setApproveRemark(remark);
                    leaveRepo.save(l);
                });
                break;
            case "overtime":
                overtimeRepo.findById(flow.getBusinessId().intValue()).ifPresent(o -> {
                    o.setStatus(status);
                    o.setApproverId(approverId);
                    o.setApproveTime(LocalDateTime.now());
                    o.setApproveRemark(remark);
                    overtimeRepo.save(o);
                });
                break;
            case "purchase":
                purchaseRepo.findById(flow.getBusinessId()).ifPresent(p -> {
                    p.setStatus(status);
                    if ("approved".equals(status)) {
                        p.setApprovedBy(approverName);
                        p.setApprovedAt(LocalDateTime.now());
                    }
                    purchaseRepo.save(p);
                });
                break;
            case "stock_loss":
                if ("approved".equals(status)) {
                    executeStockLoss(flow);
                } else {
                    jdbc.update(
                            "UPDATE stock_loss SET status = ?, approve_time = NOW(), approver_id = ?, " +
                                    "approver_name = ?, approve_remark = ? WHERE loss_id = ?",
                            status, approverId, approverName, remark, flow.getBusinessId());
                }
                break;
            // 安全修复 N7：删除 case "expense" 死分支
            // 原实现执行 UPDATE finance_expense，但全工程无 FinanceController，finance_expense 表无任何 API 能创建/查询
            // 该分支永远不可达，属于死代码。如需启用财务审批，需先实现 FinanceController 并补全 finance_expense 表 CRUD
            default:
                break;
        }
    }

    // ============================================================
    // 内部辅助
    // ============================================================

    private ApprovalFlow loadFlow(Long flowId) {
        return flowRepo.findById(flowId)
                .orElseThrow(() -> new IllegalArgumentException("审批流不存在: " + flowId));
    }

    /**
     * 审批权限校验：
     * <ul>
     *   <li>全局单据（purchase/expense）：仅总经理可审批</li>
     *   <li>分店单据（leave/overtime/stock_loss）：本店店长或总经理可审批</li>
     * </ul>
     */
    private void assertCanApprove(ApprovalFlow flow) {
        String type = flow.getFlowType();
        if ("purchase".equals(type) || "expense".equals(type)) {
            if (!UserContext.isGeneralManager()) {
                throw new SecurityException("无权限：仅总经理可审批 " + type + " 单据");
            }
            return;
        }
        // 分店单据
        if (UserContext.isGeneralManager()) {
            return;
        }
        Long current = UserContext.getCurrentStoreId();
        if (current == null || !current.equals(flow.getStoreId())) {
            throw new SecurityException("无权限：仅本店店长或总经理可审批");
        }
    }

    private ApprovalTemplate findTemplate(String flowType, Long storeId) {
        // 优先本店模板，回退全局模板（store_id=0）
        return templateRepo.findFirstByTemplateTypeAndStoreIdAndIsActive(flowType, storeId, 1)
                .orElseGet(() -> templateRepo.findFirstByTemplateTypeAndIsActive(flowType, 1).orElse(null));
    }

    private String getApproverRole(ApprovalTemplate template, int nodeOrder) {
        if (template == null) {
            return ROLE_STORE_MANAGER;
        }
        switch (nodeOrder) {
            case 1: return template.getNode1ApproverRole() != null ? template.getNode1ApproverRole() : ROLE_STORE_MANAGER;
            case 2: return template.getNode2ApproverRole();
            case 3: return template.getNode3ApproverRole();
            default: return null;
        }
    }

    /**
     * 按角色解析审批人：
     * <ul>
     *   <li>store_manager — 同门店的店长（role/staff_position 含 '店长'）</li>
     *   <li>general_manager — 总经理（store_id=0 或 can_view_all_stores=1）</li>
     * </ul>
     * 解析失败返回 null（流程仍创建，可由有权限者通过 /approve 接口处理）。
     */
    private Map<String, Object> resolveApprover(String role, Long storeId) {
        if (role == null) {
            return null;
        }
        try {
            if (ROLE_GENERAL_MANAGER.equals(role)) {
                return jdbc.queryForList(
                        "SELECT staff_id, staff_name FROM staff_master " +
                                "WHERE (store_id = 0 OR can_view_all_stores = 1) " +
                                "AND (employment_status IS NULL OR employment_status IN ('在职','active')) " +
                                "ORDER BY staff_id LIMIT 1").stream().findFirst().orElse(null);
            } else if (ROLE_STORE_MANAGER.equals(role)) {
                return jdbc.queryForList(
                        "SELECT staff_id, staff_name FROM staff_master " +
                                "WHERE store_id = ? AND (can_view_all_stores = 0 OR can_view_all_stores IS NULL) " +
                                "AND (role LIKE '%店长%' OR staff_position LIKE '%店长%' " +
                                "     OR role LIKE '%manager%' OR staff_position LIKE '%manager%') " +
                                "AND (employment_status IS NULL OR employment_status IN ('在职','active')) " +
                                "ORDER BY staff_id LIMIT 1", storeId).stream().findFirst().orElse(null);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String generateFlowNo(String flowType) {
        return "AP-" + flowType.toUpperCase() + "-" + System.currentTimeMillis();
    }

    private Integer currentStaffId() {
        Long idLong = UserContext.getStaffId();
        if (idLong != null) {
            return idLong.intValue();
        }
        try {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                Object sid = sra.getRequest().getAttribute("jwt_staff_id");
                if (sid instanceof Number) {
                    return ((Number) sid).intValue();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String currentStaffName() {
        String name = UserContext.getUsername();
        return name != null ? name : "系统";
    }

    private static Integer toInt(Object o) {
        return o == null ? null : ((Number) o).intValue();
    }

    private static Integer safeInt(Integer preferred, Integer fallback) {
        return preferred != null ? preferred : fallback;
    }

    private static String safeStr(String preferred, String fallback) {
        return preferred != null ? preferred : fallback;
    }
}
