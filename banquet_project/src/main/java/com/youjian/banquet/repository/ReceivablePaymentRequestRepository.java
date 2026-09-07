package com.youjian.banquet.repository;

import com.youjian.banquet.entity.ReceivablePaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReceivablePaymentRequestRepository extends JpaRepository<ReceivablePaymentRequest, String> {

    /**
     * 按幂等键查登记。<b>刻意不加锁</b>——这一点在应付那轮踩过两次，写清楚免得后人改回去。
     * <p>
     * 加锁读看似能挡住同键并发，实际不行：对不存在的行做加锁读，两个事务都能拿到间隙锁
     * （间隙锁互相兼容），随后各自插入，触发 insert-intention 死锁，调用方收到说不清的底层异常。
     * <p>
     * 正确做法是：需要序列化的地方（收款要改应收余额）先对应收行做加锁读，
     * 之后这里的普通读才建立一致性快照，能看到前一个事务刚提交的登记；
     * 创建应收没有可锁的父行，一律交给 request_id 主键冲突兜底。
     */
    Optional<ReceivablePaymentRequest> findByRequestId(String requestId);

    /** 某条应收或某笔收款是由哪个请求产生的。对账与排查重复时用。 */
    List<ReceivablePaymentRequest> findByOpTypeAndTargetId(String opType, Long targetId);
}
