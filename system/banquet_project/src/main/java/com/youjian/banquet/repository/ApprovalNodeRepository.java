package com.youjian.banquet.repository;

import com.youjian.banquet.entity.ApprovalNode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalNodeRepository extends JpaRepository<ApprovalNode, Long> {

    /** 按审批流ID列出所有节点（按节点序号升序） */
    List<ApprovalNode> findByFlowIdOrderByNodeOrderAsc(Long flowId);

    /** 查找指定流的指定序号节点 */
    Optional<ApprovalNode> findFirstByFlowIdAndNodeOrder(Long flowId, Integer nodeOrder);

    /** 统计节点总数 */
    long countByFlowId(Long flowId);
}
