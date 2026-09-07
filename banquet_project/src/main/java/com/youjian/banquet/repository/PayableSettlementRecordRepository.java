package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PayableSettlementRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayableSettlementRecordRepository extends JpaRepository<PayableSettlementRecord, Long> {

    /**
     * 按幂等键查已有结算。<b>刻意不加锁</b>，这一点踩过两次坑，写清楚免得后人改回去。
     * <p>
     * 一开始用的是加锁读，想着"对不存在的行加间隙锁就能把同键并发挡在外面"。
     * 实测不行：间隙锁彼此兼容，两个事务都拿得到，然后各自插入，
     * 形成经典的 insert-intention 死锁，调用方收到的是说不清的底层异常。
     * <p>
     * 现在的做法是：调用方先对应付单行做加锁读（同单并发在那里排队），
     * 之后这里的普通读才建立一致性快照——InnoDB 的 read view 是在<b>第一次一致性读</b>时创建的，
     * 加锁读不创建，所以这次普通读能看到前一个事务刚提交的流水，不会读到过期快照。
     * <p>
     * 跨单撞同一个 key 的情况这里挡不住（两个事务锁的是不同的应付单行），
     * 由 request_id 唯一键兜底，服务层把约束冲突翻译成可解释的冲突提示。
     */
    @Query("select r from PayableSettlementRecord r where r.requestId = :requestId")
    Optional<PayableSettlementRecord> findForIdempotency(@Param("requestId") String requestId);

    /** 某张应付单的全部结算流水，按发生顺序。对账用。 */
    List<PayableSettlementRecord> findByPayableIdOrderBySettlementIdAsc(Long payableId);

    Optional<PayableSettlementRecord> findBySettlementNo(String settlementNo);

    /** 流水金额合计，用于校验与原单已付金额守恒。 */
    @Query("select coalesce(sum(r.settleAmount), 0) from PayableSettlementRecord r where r.payableId = :payableId")
    BigDecimal sumSettledByPayableId(@Param("payableId") Long payableId);
}
