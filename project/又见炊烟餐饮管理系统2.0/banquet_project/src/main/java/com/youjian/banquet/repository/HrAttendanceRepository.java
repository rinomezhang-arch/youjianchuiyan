package com.youjian.banquet.repository;

import com.youjian.banquet.entity.HrAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 考勤表 Repository
 * 对应 att_attendance 表
 *
 * @author cow
 * @since 2022-03-29
 */
@Repository
public interface HrAttendanceRepository extends JpaRepository<HrAttendance, Integer> {

    /**
     * 按员工ID和考勤日期查询（格式化为yyyyMMdd）
     */
    @Query("SELECT a FROM HrAttendance a WHERE a.isDeleted = 0 AND a.staffId = :staffId AND FUNCTION('DATE_FORMAT', a.attendanceDate, '%Y%m%d') = :day")
    HrAttendance findByStaffIdAndDay(@Param("staffId") Integer staffId, @Param("day") String day);

    /**
     * 按员工ID和考勤日期查询（精确日期匹配）
     */
    @Query("SELECT a FROM HrAttendance a WHERE a.isDeleted = 0 AND a.staffId = :staffId AND a.attendanceDate = :date")
    Optional<HrAttendance> findByStaffIdAndAttendanceDate(@Param("staffId") Integer staffId, @Param("date") LocalDate date);

    /**
     * 统计员工迟到、早退、旷工的次数
     *
     * @param staffId 员工id
     * @param status  状态
     * @param month   月份 yyyyMM
     * @return 次数
     */
    @Query(value = "SELECT COUNT(*) FROM att_attendance WHERE is_deleted = 0 AND staff_id = :staffId AND status = :status AND DATE_FORMAT(attendance_date, '%Y%m') = :month", nativeQuery = true)
    Integer countByStaffIdAndStatusAndMonth(@Param("staffId") Integer staffId, @Param("status") Integer status, @Param("month") String month);

    /**
     * 查找员工休假的日期列表
     *
     * @param staffId 员工id
     * @param status  状态（4=休假）
     * @param month   月份 yyyyMM
     * @return 休假日期列表
     */
    @Query(value = "SELECT attendance_date FROM att_attendance WHERE is_deleted = 0 AND staff_id = :staffId AND status = :status AND DATE_FORMAT(attendance_date, '%Y%m') = :month", nativeQuery = true)
    List<LocalDate> findLeaveDateByStaffIdAndStatusAndMonth(@Param("staffId") Integer staffId, @Param("status") Integer status, @Param("month") String month);

    /**
     * 按员工ID查询（未删除）
     */
    List<HrAttendance> findByStaffIdAndIsDeleted(Integer staffId, Integer isDeleted);

    /**
     * 按门店ID查询（未删除，按考勤日期降序）
     */
    List<HrAttendance> findByStoreIdAndIsDeletedOrderByAttendanceDateDesc(Long storeId, Integer isDeleted);

    /**
     * 按员工ID和门店ID查询（未删除）
     */
    List<HrAttendance> findByStaffIdAndStoreIdAndIsDeleted(Integer staffId, Long storeId, Integer isDeleted);
}