package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 审批模板表：按 (template_type, store_id) 配置节点数与各节点审批角色。
 * <p>
 * 角色约定：
 * <ul>
 *   <li>store_manager — 分店店长（由分店单据流转）</li>
 *   <li>general_manager — 总经理（由全局采购/财务单据流转）</li>
 * </ul>
 * store_id = 0 表示全局通用模板。
 */
@Entity
@Table(name = "approval_template")
public class ApprovalTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "template_type", nullable = false, length = 20)
    private String templateType;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "node_count", nullable = false)
    private Integer nodeCount;

    @Column(name = "node1_approver_role", length = 50)
    private String node1ApproverRole;

    @Column(name = "node2_approver_role", length = 50)
    private String node2ApproverRole;

    @Column(name = "node3_approver_role", length = 50)
    private String node3ApproverRole;

    @Column(name = "is_active", nullable = false)
    private Integer isActive;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTemplateType() { return templateType; }
    public void setTemplateType(String templateType) { this.templateType = templateType; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Integer getNodeCount() { return nodeCount; }
    public void setNodeCount(Integer nodeCount) { this.nodeCount = nodeCount; }
    public String getNode1ApproverRole() { return node1ApproverRole; }
    public void setNode1ApproverRole(String node1ApproverRole) { this.node1ApproverRole = node1ApproverRole; }
    public String getNode2ApproverRole() { return node2ApproverRole; }
    public void setNode2ApproverRole(String node2ApproverRole) { this.node2ApproverRole = node2ApproverRole; }
    public String getNode3ApproverRole() { return node3ApproverRole; }
    public void setNode3ApproverRole(String node3ApproverRole) { this.node3ApproverRole = node3ApproverRole; }
    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }
}
