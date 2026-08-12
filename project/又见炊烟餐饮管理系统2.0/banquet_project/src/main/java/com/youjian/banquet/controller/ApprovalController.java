package com.youjian.banquet.controller;

import com.youjian.banquet.config.ApiResponse;
import com.youjian.banquet.entity.ApprovalFlow;
import com.youjian.banquet.service.ApprovalService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 通用审批引擎接口。
 * <p>
 * 接口列表：
 * <ul>
 *   <li>POST /api/approval/submit          提交审批申请</li>
 *   <li>POST /api/approval/{flowId}/approve 审批通过</li>
 *   <li>POST /api/approval/{flowId}/reject  审批驳回</li>
 *   <li>GET  /api/approval/pending          待审批列表（店长仅本店，总经理全部）</li>
 *   <li>GET  /api/approval/history          历史审批记录</li>
 *   <li>GET  /api/approval/{flowId}         审批详情</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/approval")
@CrossOrigin(origins = "*")
public class ApprovalController {

    @Autowired
    private ApprovalService approvalService;

    /**
     * 提交审批申请。
     * <p>
     * 请求体字段：
     * <ul>
     *   <li>flowType — 审批类型 leave/overtime/purchase/expense/stock_loss</li>
     *   <li>businessId — 业务单据ID</li>
     *   <li>businessNo — 业务单据编号（可选）</li>
     *   <li>storeId — 门店ID（店长自动覆盖为本店）</li>
     *   <li>applicantId — 申请人ID（可选，回退当前登录用户）</li>
     *   <li>applicantName — 申请人姓名（可选）</li>
     * </ul>
     */
    @PostMapping("/submit")
    public ApiResponse<ApprovalFlow> submit(@RequestBody Map<String, Object> body) {
        try {
            String flowType = asString(body.get("flowType"));
            Long businessId = asLong(body.get("businessId"));
            String businessNo = asString(body.get("businessNo"));
            Long storeId = asLong(body.get("storeId"));
            Integer applicantId = asInt(body.get("applicantId"));
            String applicantName = asString(body.get("applicantName"));

            if (flowType == null || businessId == null) {
                return ApiResponse.error(400, "flowType 与 businessId 必填");
            }
            // 店长仅可提交本店审批：强制覆盖 storeId / applicantId
            if (!UserContext.isGeneralManager()) {
                Long current = UserContext.getCurrentStoreId();
                if (current == null || current == 0L) {
                    return ApiResponse.error(403, "未登录或无权限提交审批");
                }
                storeId = current;
                if (applicantId == null) {
                    Long sid = UserContext.getStaffId();
                    if (sid != null) {
                        applicantId = sid.intValue();
                    }
                }
            }
            if (storeId == null) {
                return ApiResponse.error(400, "storeId 必填");
            }
            ApprovalFlow flow = approvalService.submit(flowType, businessId, businessNo,
                    storeId, applicantId, applicantName);
            return ApiResponse.success(flow);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("提交审批失败: " + e.getMessage());
        }
    }

    /**
     * 审批通过。请求体可选字段：comment — 审批意见。
     */
    @PostMapping("/{flowId:[0-9]+}/approve")
    public ApiResponse<ApprovalFlow> approve(@PathVariable Long flowId,
                                             @RequestBody(required = false) Map<String, Object> body) {
        try {
            String comment = body != null ? asString(body.get("comment")) : null;
            return ApiResponse.success(approvalService.approve(flowId, comment));
        } catch (SecurityException e) {
            return ApiResponse.error(403, e.getMessage());
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("审批通过失败: " + e.getMessage());
        }
    }

    /**
     * 审批驳回。请求体可选字段：comment — 驳回原因。
     */
    @PostMapping("/{flowId:[0-9]+}/reject")
    public ApiResponse<ApprovalFlow> reject(@PathVariable Long flowId,
                                            @RequestBody(required = false) Map<String, Object> body) {
        try {
            String comment = body != null ? asString(body.get("comment")) : null;
            return ApiResponse.success(approvalService.reject(flowId, comment));
        } catch (SecurityException e) {
            return ApiResponse.error(403, e.getMessage());
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("审批驳回失败: " + e.getMessage());
        }
    }

    /**
     * 待审批列表：店长仅本店，总经理全部。
     */
    @GetMapping("/pending")
    public ApiResponse<List<ApprovalFlow>> pending() {
        try {
            return ApiResponse.success(approvalService.getPendingList());
        } catch (SecurityException e) {
            return ApiResponse.error(403, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("获取待审批列表失败: " + e.getMessage());
        }
    }

    /**
     * 历史审批记录：店长仅本店，总经理全部。
     */
    @GetMapping("/history")
    public ApiResponse<List<ApprovalFlow>> history() {
        try {
            return ApiResponse.success(approvalService.getHistory());
        } catch (SecurityException e) {
            return ApiResponse.error(403, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("获取审批历史失败: " + e.getMessage());
        }
    }

    /**
     * 审批模板列表:供前端下拉选择
     */
    @GetMapping("/templates")
    public ApiResponse<List<Object>> templates() {
        try {
            return ApiResponse.success(approvalService.listAllTemplates());
        } catch (SecurityException e) {
            return ApiResponse.error(403, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("获取审批模板失败: " + e.getMessage());
        }
    }

    /**
     * 审批流列表:所有审批记录
     */
    @GetMapping("/flows")
    public ApiResponse<List<com.youjian.banquet.entity.ApprovalFlow>> flows() {
        try {
            return ApiResponse.success(approvalService.listAllFlows());
        } catch (SecurityException e) {
            return ApiResponse.error(403, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("获取审批流失败: " + e.getMessage());
        }
    }

    /**
     * 审批详情：返回 flow + nodes。
     */
    @GetMapping("/{flowId:[0-9]+}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long flowId) {
        try {
            return ApiResponse.success(approvalService.getDetail(flowId));
        } catch (SecurityException e) {
            return ApiResponse.error(403, e.getMessage());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("获取审批详情失败: " + e.getMessage());
        }
    }

    // ===== 类型转换辅助 =====

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }

    private static Long asLong(Object o) {
        if (o == null) return null;
        try {
            return Long.valueOf(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer asInt(Object o) {
        if (o == null) return null;
        try {
            return Integer.valueOf(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
