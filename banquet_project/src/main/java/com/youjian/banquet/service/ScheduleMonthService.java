package com.youjian.banquet.service;

import com.youjian.banquet.entity.ScheduleDay;
import com.youjian.banquet.entity.ScheduleMonth;
import com.youjian.banquet.repository.ScheduleDayRepository;
import com.youjian.banquet.repository.ScheduleMonthRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 排班服务
 * 对应规划手册 5.txt - 排班模块（月度排班+每日明细）
 */
@Service
public class ScheduleMonthService {

    @Autowired private ScheduleMonthRepository monthRepo;
    @Autowired private ScheduleDayRepository dayRepo;

    /** 查询某门店某月排班主表 */
    public List<ScheduleMonth> listSchedules(Long storeId, String month) {
        if (month != null && !month.isEmpty()) {
            return monthRepo.findByStoreIdAndScheduleMonth(storeId, month);
        }
        return monthRepo.findByStoreId(storeId);
    }

    /** 查询某排班主表的每日明细 */
    public List<ScheduleDay> listDays(Long scheduleId) {
        return dayRepo.findByScheduleId(scheduleId);
    }

    /** 查询某员工一个月内的排班 */
    public List<ScheduleDay> getStaffSchedule(Long staffId, String month) {
        // month 格式 YYYY-MM
        LocalDate start = LocalDate.parse(month + "-01");
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return dayRepo.findByStaffIdAndWorkDateBetween(staffId, start, end);
    }

    /**
     * 生成/覆盖月度排班
     * @param storeId 门店
     * @param month YYYY-MM
     * @param deptId 部门
     * @param days 每日排班明细列表
     */
    @Transactional
    public ScheduleMonth generateSchedule(Long storeId, String month, Long deptId, List<ScheduleDay> days) {
        // 查找或创建主表
        ScheduleMonth sm = monthRepo.findByStoreIdAndDeptIdAndScheduleMonth(storeId, deptId, month)
                .orElseGet(() -> {
                    ScheduleMonth n = new ScheduleMonth();
                    n.setStoreId(storeId);
                    n.setDeptId(deptId);
                    n.setScheduleMonth(month);
                    n.setStatus(0);
                    return n;
                });
        monthRepo.save(sm);

        // 删除旧明细，写入新明细
        dayRepo.deleteByScheduleId(sm.getScheduleId());
        for (ScheduleDay d : days) {
            d.setScheduleId(sm.getScheduleId());
            dayRepo.save(d);
        }
        return sm;
    }

    /** 发布排班 */
    @Transactional
    public ScheduleMonth publish(Long scheduleId, Long publishedBy) {
        ScheduleMonth sm = monthRepo.findById(scheduleId).orElse(null);
        if (sm == null) throw new RuntimeException("排班不存在: " + scheduleId);
        sm.setStatus(1);
        sm.setPublishedBy(publishedBy);
        sm.setPublishedTime(LocalDateTime.now());
        return monthRepo.save(sm);
    }

    /** 确认排班 */
    @Transactional
    public ScheduleMonth confirm(Long scheduleId) {
        ScheduleMonth sm = monthRepo.findById(scheduleId).orElse(null);
        if (sm == null) throw new RuntimeException("排班不存在: " + scheduleId);
        sm.setStatus(2);
        return monthRepo.save(sm);
    }
}
