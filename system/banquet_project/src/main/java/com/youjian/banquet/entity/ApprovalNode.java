package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 审批节点表：每个审批流包含一个或多个节点，按 node_order 顺序流转。
 * status 取值：pending / approved / rejected
 */
@Entity
@Table(name = "approval_node")
public class ApprovalNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "flow_id", nullable = false)
    private Long flowId;

    @Column(name = "node_order", nullable = false)
    private Integer nodeOrder;

    @Column(name = "node_name", length = 50)
    private String nodeName;

    @Column(name = "approver_id")
    private Integer approverId;

    @Column(name = "approver_name", length = 50)
    private String approverName;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "comment", length = 500)
    private String comment;

    @Column(name = "approved_time")
    private LocalDateTime approvedTime;

    public ApprovalNode() {
        this.status = "pending";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFlowId() { return flowId; }
    public void setFlowId(Long flowId) { this.flowId = flowId; }
    public Integer getNodeOrder() { return nodeOrder; }
    public void setNodeOrder(Integer nodeOrder) { this.nodeOrder = nodeOrder; }
    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    public Integer getApproverId() { return approverId; }
    public void setApproverId(Integer approverId) { this.approverId = approverId; }
    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getApprovedTime() { return approvedTime; }
    public void setApprovedTime(LocalDateTime approvedTime) { this.approvedTime = approvedTime; }
}
