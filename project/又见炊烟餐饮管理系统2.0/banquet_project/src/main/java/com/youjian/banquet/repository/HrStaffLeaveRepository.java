package com.youjian.banquet.repository;

import com.youjian.banquet.entity.HrStaffLeave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 员工请假 Repository
 * 对应参考系统: StaffLeaveMapper
 */
@Repository
public interface HrStaffLeaveRepository extends JpaRepository<HrStaffLeave, Integer>, JpaSpecificationExecutor<HrStaffLeave> {

    List<HrStaffLeave> findByStoreIdAndIsDeleted(Long storeId, Integer isDeleted);

    Page<HrStaffLeave> findByStoreIdAndIsDeleted(Long storeId, Integer isDeleted, Pageable pageable);

    Page<HrStaffLeave> findByStaffIdAndIsDeleted(Integer staffId, Integer isDeleted, Pageable pageable);

    List<HrStaffLeave> findByStaffIdAndStatusAndIsDeleted(Integer staffId, Integer status, Integer isDeleted);

    /**
     * 分页查询员工请假记录（不显示已撤销的），关联员工表和部门表
     */
    @Query(value = "SELECT hsl.* FROM hr_staff_leave hsl " +
            "INNER JOIN staff_master sm ON hsl.staff_id = sm.staff_id " +
            "LEFT JOIN department d ON sm.dept_id = d.dept_id " +
            "WHERE hsl.is_deleted = 0 AND hsl.store_id = :storeId " +
            "AND hsl.status != :cancelStatus " +
            "AND (:name IS NULL OR sm.staff_name LIKE CONCAT('%', :name, '%')) " +
            "ORDER BY hsl.create_time DESC",
            countQuery = "SELECT COUNT(*) FROM hr_staff_leave hsl " +
            "INNER JOIN staff_master sm ON hsl.staff_id = sm.staff_id " +
            "WHERE hsl.is_deleted = 0 AND hsl.store_id = :storeId " +
            "AND hsl.status != :cancelStatus " +
            "AND (:name IS NULL OR sm.staff_name LIKE CONCAT('%', :name, '%'))",
            nativeQuery = true)
    Page<Object[]> listStaffLeaveVO(@Param("storeId") Long storeId,
                                   @Param("cancelStatus") Integer cancelStatus,
                                   @Param("name") String name,
                                   Pageable pageable);

    /**
     * 分页查询员工请假记录（按部门过滤），关联员工表和部门表
     */
    @Query(value = "SELECT hsl.* FROM hr_staff_leave hsl " +
            "INNER JOIN staff_master sm ON hsl.staff_id = sm.staff_id " +
            "LEFT JOIN department d ON sm.dept_id = d.dept_id " +
            "WHERE hsl.is_deleted = 0 AND hsl.store_id = :storeId " +
            "AND sm.dept_id = :deptId " +
            "AND hsl.status != :cancelStatus " +
            "AND (:name IS NULL OR sm.staff_name LIKE CONCAT('%', :name, '%')) " +
            "ORDER BY hsl.create_time DESC",
            countQuery = "SELECT COUNT(*) FROM hr_staff_leave hsl " +
            "INNER JOIN staff_master sm ON hsl.staff_id = sm.staff_id " +
            "WHERE hsl.is_deleted = 0 AND hsl.store_id = :storeId " +
            "AND sm.dept_id = :deptId " +
            "AND hsl.status != :cancelStatus " +
            "AND (:name IS NULL OR sm.staff_name LIKE CONCAT('%', :name, '%'))",
            nativeQuery = true)
    Page<Object[]> listStaffDeptLeaveVO(@Param("storeId") Long storeId,
                                       @Param("cancelStatus") Integer cancelStatus,
                                       @Param("name") String name,
                                       @Param("deptId") Integer deptId,
                                       Pageable pageable);
}