package com.youjian.banquet.repository;

import com.youjian.banquet.entity.ScheduleMonth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleMonthRepository extends JpaRepository<ScheduleMonth, Long> {

    List<ScheduleMonth> findByStoreId(Long storeId);

    List<ScheduleMonth> findByStoreIdAndScheduleMonth(Long storeId, String scheduleMonth);

    Optional<ScheduleMonth> findByStoreIdAndDeptIdAndScheduleMonth(Long storeId, Long deptId, String scheduleMonth);

    List<ScheduleMonth> findByDeptIdAndScheduleMonth(Long deptId, String scheduleMonth);
}
