package com.youjian.banquet.repository;

import com.youjian.banquet.entity.ScheduleDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleDayRepository extends JpaRepository<ScheduleDay, Long> {

    List<ScheduleDay> findByScheduleId(Long scheduleId);

    List<ScheduleDay> findByStaffIdAndWorkDateBetween(Long staffId, LocalDate start, LocalDate end);

    List<ScheduleDay> findByScheduleIdAndWorkDateBetween(Long scheduleId, LocalDate start, LocalDate end);

    List<ScheduleDay> findByWorkDate(LocalDate workDate);

    void deleteByScheduleId(Long scheduleId);
}
