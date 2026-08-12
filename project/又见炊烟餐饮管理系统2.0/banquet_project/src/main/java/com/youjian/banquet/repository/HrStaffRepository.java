package com.youjian.banquet.repository;

import com.youjian.banquet.entity.HrStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * HR员工 Repository
 * 对应 hr_staff 表
 *
 * @author cow
 * @since 2022-01-27
 */
@Repository
public interface HrStaffRepository extends JpaRepository<HrStaff, Integer>, JpaSpecificationExecutor<HrStaff> {

    /**
     * 按门店ID查找所有未删除的员工
     */
    List<HrStaff> findByStoreIdAndIsDeleted(Long storeId, Integer isDeleted);

    /**
     * 按门店ID和部门ID查找员工
     */
    List<HrStaff> findByStoreIdAndDeptIdAndIsDeleted(Long storeId, Integer deptId, Integer isDeleted);

    /**
     * 按姓名模糊查询
     */
    List<HrStaff> findByStoreIdAndNameContainingAndIsDeleted(Long storeId, String name, Integer isDeleted);

    /**
     * 按编码查找
     */
    Optional<HrStaff> findByCodeAndIsDeleted(String code, Integer isDeleted);
}