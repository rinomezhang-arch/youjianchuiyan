package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PayableCreateRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayableCreateRequestRepository extends JpaRepository<PayableCreateRequest, String> {

    /**
     * 按幂等键查登记。<b>刻意不加锁</b>。
     * <p>
     * 结算那边踩过这个坑：对不存在的行做加锁读，两个事务都能拿到间隙锁（间隙锁互相兼容），
     * 随后各自插入，触发 insert-intention 死锁，调用方收到说不清的底层异常。
     * <p>
     * 创建这边更没有可锁的父行——单据还不存在，锁无处可加。
     * 并发一律交给 request_id 主键兜底：先到的插入成功，后到的主键冲突，
     * 服务层翻译成「请用同一 requestId 重试取回结果」，重试时就走到重放分支。
     */
    Optional<PayableCreateRequest> findByRequestId(String requestId);

    /** 某张应付单是由哪个请求创建的。对账和排查重复单时用。 */
    List<PayableCreateRequest> findByPayableId(Long payableId);
}
