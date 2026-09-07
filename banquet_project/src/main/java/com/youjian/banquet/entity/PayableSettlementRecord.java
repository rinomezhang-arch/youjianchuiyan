package com.youjian.banquet.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 应付结算流水。每结算一次记一行。
 * <p>
 * 存在的理由有两个：
 * <ul>
 *   <li><b>幂等</b>——{@link #requestId} 由调用方生成并在重试时保持不变，
 *       同一个 requestId 再提交多少次都只会产生一笔结算；</li>
 *   <li><b>可追溯</b>——原来只看得到"已付 8000"，看不出是一次结的还是三次结的、谁结的。
 *       有了流水，每一笔的金额、结算后余额、操作人、时间都留得住，
 *       且流水金额合计必须等于原单的已付金额。</li>
 * </ul>
 * <p>
 * <b>这是账务流水，不是银行付款凭证。</b>系统不对接银行、不执行真实付款。
 */
@Entity
@Table(name = "payable_settlement_record",
        // 唯一约束必须在实体上也声明一份：只写在迁移 SQL 里的话，
        // 任何按实体生成表的环境（测试库、新环境初始化）都没有幂等保护。
        // 实测代价：跨单撞同一个 requestId 时两笔都记账成功，幂等形同虚设。
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payable_settlement_request", columnNames = "request_id"),
                @UniqueConstraint(name = "uk_payable_settlement_no", columnNames = "settlement_no")
        })
public class PayableSettlementRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long settlementId;

    /** 幂等键，全局唯一。调用方必须自己生成并在重试时复用同一个值。 */
    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    /** 对外可读回的结算号。 */
    @Column(name = "settlement_no", nullable = false, length = 50)
    private String settlementNo;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "payable_id", nullable = false)
    private Long payableId;

    @Column(name = "payable_no", length = 50)
    private String payableNo;

    @Column(name = "settle_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal settleAmount;

    /** 结算后的已付金额快照，对账时不必回放即可看到当时状态。 */
    @Column(name = "paid_after", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAfter;

    @Column(name = "pending_after", nullable = false, precision = 12, scale = 2)
    private BigDecimal pendingAfter;

    @Column(name = "status_after", nullable = false, length = 20)
    private String statusAfter;

    @Column(name = "operator_name", length = 40)
    private String operatorName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "note", length = 200)
    private String note;

    public Long getSettlementId() { return settlementId; }
    public void setSettlementId(Long settlementId) { this.settlementId = settlementId; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getSettlementNo() { return settlementNo; }
    public void setSettlementNo(String settlementNo) { this.settlementNo = settlementNo; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Long getPayableId() { return payableId; }
    public void setPayableId(Long payableId) { this.payableId = payableId; }

    public String getPayableNo() { return payableNo; }
    public void setPayableNo(String payableNo) { this.payableNo = payableNo; }

    public BigDecimal getSettleAmount() { return settleAmount; }
    public void setSettleAmount(BigDecimal settleAmount) { this.settleAmount = settleAmount; }

    public BigDecimal getPaidAfter() { return paidAfter; }
    public void setPaidAfter(BigDecimal paidAfter) { this.paidAfter = paidAfter; }

    public BigDecimal getPendingAfter() { return pendingAfter; }
    public void setPendingAfter(BigDecimal pendingAfter) { this.pendingAfter = pendingAfter; }

    public String getStatusAfter() { return statusAfter; }
    public void setStatusAfter(String statusAfter) { this.statusAfter = statusAfter; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
