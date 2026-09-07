package com.youjian.banquet.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 手工应付创建的幂等登记：哪个请求创建了哪张单。
 * <p>
 * 解决两件事：
 * <ul>
 *   <li><b>重复创建</b>——前端超时重发、用户点两次，原本会多出一模一样的应付单；</li>
 *   <li><b>结果丢失后的恢复</b>——第一次的响应没收到时，调用方带同一个 requestId 再来一次，
 *       拿回的是原来那张单的回执，而不是又建一张。</li>
 * </ul>
 * <p>
 * {@link #paramsHash} 存的是业务参数指纹，不是参数原文：
 * 同键换了门店、供应商、金额、日期或备注时能识别出来并拒绝，
 * 而冲突提示里无从泄漏别人那张单的内容。
 */
@Entity
@Table(name = "payable_create_request")
public class PayableCreateRequest {

    /** 幂等键本身就是主键：同一个 requestId 只可能有一条登记，并发靠主键冲突兜底。 */
    @Id
    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "payable_id", nullable = false)
    private Long payableId;

    @Column(name = "payable_no", nullable = false, length = 50)
    private String payableNo;

    /** 业务参数的 SHA-256 十六进制指纹。 */
    @Column(name = "params_hash", nullable = false, length = 64)
    private String paramsHash;

    @Column(name = "operator_name", length = 40)
    private String operatorName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Long getPayableId() { return payableId; }
    public void setPayableId(Long payableId) { this.payableId = payableId; }

    public String getPayableNo() { return payableNo; }
    public void setPayableNo(String payableNo) { this.payableNo = payableNo; }

    public String getParamsHash() { return paramsHash; }
    public void setParamsHash(String paramsHash) { this.paramsHash = paramsHash; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
