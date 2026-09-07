package com.youjian.banquet.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 应收创建与收款登记的幂等登记：哪个请求产生了哪条记录。
 * <p>
 * 与应付那套同构：同一个 {@link #requestId} 再来一次，参数一致就把原回执还回去，
 * 参数变了就明确拒绝，不会照着新参数再记一笔。{@link #paramsHash} 存指纹不存原文，
 * 冲突时无从泄漏别人那条单据的内容。
 * <p>
 * 单表承载两类操作，靠 {@link #opType} 区分：一天几十条的量，拆两张表只会让查询和回执变复杂。
 */
@Entity
@Table(name = "receivable_payment_request")
public class ReceivablePaymentRequest {

    /** 幂等键即主键：同一个 requestId 只可能有一条登记，并发靠主键冲突兜底。 */
    @Id
    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    /** receivable=创建应收，payment=登记收款。 */
    @Column(name = "op_type", nullable = false, length = 16)
    private String opType;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    /** 本次请求产生的 receivable_id 或 payment_id。 */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /** 回执里给前端核对的单号。 */
    @Column(name = "target_no", nullable = false, length = 64)
    private String targetNo;

    /**
     * 指纹列显式声明为 CHAR(64)，与迁移里的定义一致。
     * <p>
     * 只写 length=64 时 Hibernate 生成的是 VARCHAR(64)，而迁移建的是 CHAR(64)：
     * 实体与真实结构不一致，任何按实体建表的环境与生产就此分叉，
     * 结构校验（hbm2ddl validate）也会报错。应付那批同类问题已修，这里同口径处理。
     */
    @Column(name = "params_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String paramsHash;

    /** 操作人取自登录身份；基线那种取不到就写死默认名的兜底不再使用。 */
    @Column(name = "operator_name", length = 40)
    private String operatorName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getOpType() { return opType; }
    public void setOpType(String opType) { this.opType = opType; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getTargetNo() { return targetNo; }
    public void setTargetNo(String targetNo) { this.targetNo = targetNo; }

    public String getParamsHash() { return paramsHash; }
    public void setParamsHash(String paramsHash) { this.paramsHash = paramsHash; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
