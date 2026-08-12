package com.youjian.banquet.repository;

import com.youjian.banquet.entity.SysDocs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysDocsRepository extends JpaRepository<SysDocs, Integer>, JpaSpecificationExecutor<SysDocs> {

    List<SysDocs> findByStoreIdAndIsDeleted(Long storeId, Integer isDeleted);

    List<SysDocs> findByStoreIdAndIsDeletedOrderByCreateTimeDesc(Long storeId, Integer isDeleted);

    List<SysDocs> findByStoreIdAndTypeAndIsDeleted(Long storeId, String type, Integer isDeleted);

    List<SysDocs> findByStoreIdAndStaffIdAndIsDeleted(Long storeId, Integer staffId, Integer isDeleted);
}