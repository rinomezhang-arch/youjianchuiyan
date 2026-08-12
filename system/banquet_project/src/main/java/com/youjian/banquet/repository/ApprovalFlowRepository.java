package com.youjian.banquet.repository;

import com.youjian.banquet.entity.ApprovalFlow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalFlowRepository extends JpaRepository<ApprovalFlow, Long> {

    /** 按业务类型 + 业务单据ID 查找审批流（每种业务单据仅一个活跃流） */
    Optional<ApprovalFlow> findFirstByFlowTypeAndBusinessId(String flowType, Long businessId);

    /** 待审批列表：按门店过滤 */
    List<ApprovalFlow> findByStatusAndStoreIdOrderByCreatedTimeDesc(String status, Long storeId);

    /** 待审批列表：全部门店 */
    List<ApprovalFlow> findByStatusOrderByCreatedTimeDesc(String status);

    /** 历史记录：按门店过滤（非 pending 状态） */
    List<ApprovalFlow> findByStatusNotAndStoreIdOrderByCreatedTimeDesc(String status, Long storeId);

    /** 历史记录：全部门店（非 pending 状态） */
    List<ApprovalFlow> findByStatusNotOrderByCreatedTimeDesc(String status);

    /** 申请人提交历史 */
    List<ApprovalFlow> findByApplicantIdOrderByCreatedTimeDesc(Integer applicantId);
}
