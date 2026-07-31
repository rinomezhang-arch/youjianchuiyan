package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 审批流主表。
 * <p>
 * flow_type 取值：leave / overtime / purchase / expense / stock_loss
 * status 取值：pending / approved / rejected / cancelled
 * <p>
 * 业务规则：
 * <ul>
 *   <li>分店单据（leave/overtime/stock_loss）自动流转至分店店长审批</li>
 *   <li>全局单据（purchase/expense）流转总经理审批</li>
 * </ul>
 */
@Entity
@Table(name = "approval_flow")
public class ApprovalFlow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "flow_no", nullable = false, length = 64)
    private String flowNo;

    @Column(name = "flow_type", nullable = false, length = 20)
    private String flowType;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "business_no", length = 64)
    private String businessNo;

    @Column(name = "applicant_id")
    private Integer applicantId;

    @Column(name = "applicant_name", length = 50)
    private String applicantName;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "current_node", nullable = false)
    private Integer currentNode;

    @Column(name = "created_time")
    private LocalDateTime createdTime;

    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    @PrePersist
    protected void onCreate() {
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
        if (this.status == null) {
            this.status = "pending";
        }
        if (this.currentNode == null) {
            this.currentNode = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFlowNo() { return flowNo; }
    public void setFlowNo(String flowNo) { this.flowNo = flowNo; }
    public String getFlowType() { return flowType; }
    public void setFlowType(String flowType) { this.flowType = flowType; }
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
    public String getBusinessNo() { return businessNo; }
    public void setBusinessNo(String businessNo) { this.businessNo = businessNo; }
    public Integer getApplicantId() { return applicantId; }
    public void setApplicantId(Integer applicantId) { this.applicantId = applicantId; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getCurrentNode() { return currentNode; }
    public void setCurrentNode(Integer currentNode) { this.currentNode = currentNode; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
