package com.youjian.banquet.repository;

import com.youjian.banquet.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Integer>, JpaSpecificationExecutor<AttendanceRecord> {

    List<AttendanceRecord> findByStaffId(Integer staffId);

    List<AttendanceRecord> findByMonth(String month);

    List<AttendanceRecord> findByStaffIdAndMonth(Integer staffId, String month);

    Optional<AttendanceRecord> findByEmpIdAndMonth(String empId, String month);

    List<AttendanceRecord> findByStoreId(Long storeId);

    List<AttendanceRecord> findByStoreIdAndMonth(Long storeId, String month);

    List<AttendanceRecord> findByStoreIdAndMonthOrderByEmpIdAscDayNumAsc(Long storeId, String month);

    List<AttendanceRecord> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    @Query("SELECT a FROM AttendanceRecord a WHERE a.storeId = :storeId AND a.month = :month AND (a.staffName LIKE %:keyword% OR a.empName LIKE %:keyword%)")
    List<AttendanceRecord> searchByStoreIdAndMonthAndKeyword(@Param("storeId") Long storeId, @Param("month") String month, @Param("keyword") String keyword);

    void deleteByEmpIdAndMonth(String empId, String month);
}
