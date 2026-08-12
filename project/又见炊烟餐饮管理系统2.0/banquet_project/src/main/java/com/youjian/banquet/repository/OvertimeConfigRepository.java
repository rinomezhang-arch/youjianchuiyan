package com.youjian.banquet.repository;

import com.youjian.banquet.entity.OvertimeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OvertimeConfigRepository extends JpaRepository<OvertimeConfig, Integer>, JpaSpecificationExecutor<OvertimeConfig> {

    List<OvertimeConfig> findByStoreIdAndIsDeleted(Long storeId, Integer isDeleted);

    List<OvertimeConfig> findByStoreIdAndIsDeletedOrderByCreateTimeDesc(Long storeId, Integer isDeleted);

    List<OvertimeConfig> findByStoreIdAndDeptIdAndIsDeleted(Long storeId, Integer deptId, Integer isDeleted);

    Optional<OvertimeConfig> findByStoreIdAndDeptIdAndTypeNumAndIsDeleted(Long storeId, Integer deptId, Integer typeNum, Integer isDeleted);
}