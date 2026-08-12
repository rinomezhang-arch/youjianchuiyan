package com.youjian.banquet.repository;

import com.youjian.banquet.entity.HrDocs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * HR文件 Repository
 * 对应 hr_docs 表
 *
 * @author cow
 * @since 2022-02-24
 */
@Repository
public interface HrDocsRepository extends JpaRepository<HrDocs, Integer>, JpaSpecificationExecutor<HrDocs> {

    /**
     * 按MD5查找未删除的文件（用于去重）
     */
    List<HrDocs> findByMd5AndIsDeleted(String md5, Integer isDeleted);

    /**
     * 按门店ID查找所有未删除的文件
     */
    List<HrDocs> findByStoreIdAndIsDeleted(Long storeId, Integer isDeleted);

    /**
     * 按上传者ID查找
     */
    List<HrDocs> findByStaffIdAndIsDeleted(Integer staffId, Integer isDeleted);
}