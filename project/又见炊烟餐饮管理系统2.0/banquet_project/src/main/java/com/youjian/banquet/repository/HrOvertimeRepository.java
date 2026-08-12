package com.youjian.banquet.repository;

import com.youjian.banquet.entity.HrOvertime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * HR加班配置 Repository
 * 来源：HR系统 OvertimeMapper
 */
@Repository
public interface HrOvertimeRepository extends JpaRepository<HrOvertime, Integer> {

    /**
     * 按门店ID查询（未删除）
     */
    List<HrOvertime> findByStoreIdAndIsDeletedOrderByCreateTimeDesc(Long storeId, Integer isDeleted);

    /**
     * 按门店ID查询全部
     */
    List<HrOvertime> findByStoreIdOrderByCreateTimeDesc(Long storeId);

    /**
     * 按部门ID和加班类型查询（未删除）
     */
    Optional<HrOvertime> findByDeptIdAndTypeNumAndIsDeleted(Integer deptId, Integer typeNum, Integer isDeleted);

    /**
     * 按部门ID和加班类型查询（不区分删除状态，用于setOvertime的saveOrUpdate逻辑）
     */
    Optional<HrOvertime> findByDeptIdAndTypeNum(Integer deptId, Integer typeNum);

    /**
     * 逻辑删除（将is_deleted设为1）
     */
    @Modifying
    @Query("UPDATE HrOvertime SET isDeleted = 1, updateTime = CURRENT_TIMESTAMP WHERE id = :id")
    int softDeleteById(@Param("id") Integer id);

    /**
     * 批量逻辑删除
     */
    @Modifying
    @Query("UPDATE HrOvertime SET isDeleted = 1, updateTime = CURRENT_TIMESTAMP WHERE id IN :ids")
    int softDeleteBatch(@Param("ids") List<Integer> ids);
}