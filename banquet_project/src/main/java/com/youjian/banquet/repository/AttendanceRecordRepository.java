package com.youjian.banquet.repository;

import com.youjian.banquet.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Integer> {
    List<AttendanceRecord> findByStaffId(Integer staffId);
    List<AttendanceRecord> findByMonth(String month);
    List<AttendanceRecord> findByStaffIdAndMonth(Integer staffId, String month);
}
