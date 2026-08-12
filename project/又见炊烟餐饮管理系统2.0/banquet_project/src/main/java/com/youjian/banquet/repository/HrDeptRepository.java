package com.youjian.banquet.repository;

import com.youjian.banquet.entity.HrDept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * HR部门 Repository
 * 对应 hr_dept 表
 *
 * @author cow
 * @since 2022-03-07
 */
@Repository
public interface HrDeptRepository extends JpaRepository<HrDept, Integer> {

    /**
     * 查找所有父级部门（分页支持）
     */
    @Query("SELECT d FROM HrDept d WHERE d.isDeleted = 0 AND d.parentId = 0 AND d.name LIKE %:name%")
    List<HrDept> listParentDept(@Param("name") String name);

    /**
     * 查找所有子部门
     */
    @Query("SELECT d FROM HrDept d WHERE d.isDeleted = 0 AND d.parentId != 0")
    List<HrDept> findSubDept();

    /**
     * 按门店ID查找所有未删除的部门
     */
    List<HrDept> findByStoreIdAndIsDeleted(Long storeId, Integer isDeleted);

    /**
     * 按门店ID和父级ID查找子部门
     */
    List<HrDept> findByStoreIdAndParentIdAndIsDeleted(Long storeId, Integer parentId, Integer isDeleted);

    /**
     * 按门店ID查找所有部门（包含已删除的）
     */
    List<HrDept> findByStoreId(Long storeId);
}