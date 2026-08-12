package com.youjian.banquet.service;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrAttendance;
import com.youjian.banquet.repository.HrAttendanceRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 考勤服务类
 * 复刻自HR系统 AttendanceService，ORM从MyBatis Plus改为JPA+JdbcTemplate
 *
 * @author cow
 * @since 2022-03-29
 */
@Service
public class HrAttendanceService {

    /** 考勤状态枚举：0正常，1迟到，2早退，3旷工，4休假 */
    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_LATE = 1;
    private static final int STATUS_LEAVE_EARLY = 2;
    private static final int STATUS_ABSENTEEISM = 3;
    private static final int STATUS_LEAVE = 4;

    private static final Map<Integer, String> STATUS_MAP = new LinkedHashMap<>();
    private static final Map<Integer, String> STATUS_TAG_TYPE_MAP = new LinkedHashMap<>();

    static {
        STATUS_MAP.put(STATUS_NORMAL, "正常");
        STATUS_TAG_TYPE_MAP.put(STATUS_NORMAL, "success");
        STATUS_MAP.put(STATUS_LATE, "迟到");
        STATUS_TAG_TYPE_MAP.put(STATUS_LATE, "");
        STATUS_MAP.put(STATUS_LEAVE_EARLY, "早退");
        STATUS_TAG_TYPE_MAP.put(STATUS_LEAVE_EARLY, "warning");
        STATUS_MAP.put(STATUS_ABSENTEEISM, "旷工");
        STATUS_TAG_TYPE_MAP.put(STATUS_ABSENTEEISM, "danger");
        STATUS_MAP.put(STATUS_LEAVE, "休假");
        STATUS_TAG_TYPE_MAP.put(STATUS_LEAVE, "info");
    }

    @Autowired
    private HrAttendanceRepository hrAttendanceRepository;

    @Autowired
    private JdbcTemplate jdbc;

    // ==================== 基础CRUD ====================

    public Result<HrAttendance> add(HrAttendance attendance) {
        HrAttendance saved = hrAttendanceRepository.save(attendance);
        return Result.success(saved);
    }

    public Result<String> deleteById(Integer id) {
        Optional<HrAttendance> opt = hrAttendanceRepository.findById(id);
        if (opt.isPresent()) {
            HrAttendance att = opt.get();
            att.setIsDeleted(1);
            hrAttendanceRepository.save(att);
            return Result.success();
        }
        return Result.error(500, "删除失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<String> deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            Optional<HrAttendance> opt = hrAttendanceRepository.findById(id);
            if (opt.isPresent()) {
                HrAttendance att = opt.get();
                att.setIsDeleted(1);
                hrAttendanceRepository.save(att);
            }
        }
        return Result.success();
    }

    public Result<HrAttendance> edit(HrAttendance attendance) {
        if (attendance.getId() == null) {
            return Result.error(500, "ID不能为空");
        }
        HrAttendance saved = hrAttendanceRepository.save(attendance);
        return Result.success(saved);
    }

    public Result<HrAttendance> findById(Integer id) {
        Optional<HrAttendance> opt = hrAttendanceRepository.findById(id);
        return opt.map(Result::success).orElseGet(() -> Result.error(500, "未找到"));
    }

    // ==================== 分页条件查询（含日历视图） ====================

    /**
     * 分页条件查询考勤数据，构建日历视图
     *
     * @param current 当前页
     * @param size    每页条数
     * @param name    员工姓名（模糊搜索）
     * @param deptId  部门ID
     * @param month   月份 yyyyMM
     */
    public Result<Map<String, Object>> list(Integer current, Integer size, String name, Integer deptId, String month) {
        // 如果没有指明月份，默认显示当前月份
        if (month == null || month.isEmpty()) {
            month = java.time.YearMonth.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        }

        // 组装查询SQL
        StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(*) FROM staff_master s LEFT JOIN department d ON s.dept_id = d.dept_id WHERE s.employment_status IN ('1', '2', '在职', '试用期')");
        StringBuilder dataSql = new StringBuilder(
                "SELECT s.staff_id, s.staff_name, s.staff_phone, s.home_address, s.store_id, " +
                        "s.dept_id, d.dept_name, s.staff_code " +
                        "FROM staff_master s LEFT JOIN department d ON s.dept_id = d.dept_id " +
                        "WHERE s.employment_status IN ('1', '2', '在职', '试用期')");

        List<Object> params = new ArrayList<>();

        if (name != null && !name.isEmpty()) {
            String likeName = "%" + name + "%";
            countSql.append(" AND s.staff_name LIKE ?");
            dataSql.append(" AND s.staff_name LIKE ?");
            params.add(likeName);
        }

        if (deptId != null) {
            countSql.append(" AND s.dept_id = ?");
            dataSql.append(" AND s.dept_id = ?");
            params.add(deptId);
        }

        // 计算总数
        int total = jdbc.queryForObject(countSql.toString(), Integer.class, params.toArray());

        // 分页
        int offset = (current - 1) * size;
        dataSql.append(" LIMIT ?, ?");
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(offset);
        dataParams.add(size);

        List<Map<String, Object>> staffList = jdbc.queryForList(dataSql.toString(), dataParams.toArray());

        // 获取当月日期列表
        String[] monthDayList = getMonthDayList(month);

        // 为每个员工填充考勤日历数据
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Map<String, Object> staff : staffList) {
            Integer staffId = (Integer) staff.get("staff_id");
            Map<String, Object> staffVO = new LinkedHashMap<>();
            staffVO.put("staffId", staffId);
            staffVO.put("deptId", staff.get("dept_id"));
            staffVO.put("code", staff.get("staff_code"));
            staffVO.put("name", staff.get("staff_name"));
            staffVO.put("phone", staff.get("staff_phone"));
            staffVO.put("address", staff.get("home_address"));
            staffVO.put("deptName", staff.get("dept_name"));

            List<Map<String, Object>> attendanceList = new ArrayList<>();
            for (String day : monthDayList) {
                Map<String, Object> dayMap = new LinkedHashMap<>();
                HrAttendance attendance = hrAttendanceRepository.findByStaffIdAndDay(staffId, day);

                if (attendance == null) {
                    LocalDate localDate = LocalDate.parse(day, DateTimeFormatter.ofPattern("yyyyMMdd"));
                    // 如果是周末就休假
                    if (isWeekend(localDate)) {
                        dayMap.put("message", STATUS_MAP.get(STATUS_LEAVE));
                        dayMap.put("tagType", STATUS_TAG_TYPE_MAP.get(STATUS_LEAVE));
                    } else {
                        dayMap.put("message", STATUS_MAP.get(STATUS_NORMAL));
                        dayMap.put("tagType", STATUS_TAG_TYPE_MAP.get(STATUS_NORMAL));
                    }
                    dayMap.put("attendanceDate", localDate);
                } else {
                    Integer status = attendance.getStatus();
                    dayMap.put("message", status != null ? STATUS_MAP.getOrDefault(status, "未知") : "正常");
                    dayMap.put("tagType", status != null ? STATUS_TAG_TYPE_MAP.getOrDefault(status, "") : "success");
                    dayMap.put("attendanceDate", attendance.getAttendanceDate());
                }
                attendanceList.add(dayMap);
            }
            staffVO.put("attendanceList", attendanceList);
            resultList.add(staffVO);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        int pages = (int) Math.ceil((double) total / size);
        result.put("pages", pages);
        result.put("total", total);
        result.put("list", resultList);
        result.put("dayNum", monthDayList.length);
        result.put("month", month);

        return Result.success(result);
    }

    // ==================== 导出 ====================

    /**
     * 导出员工月考勤数据（CSV格式）
     */
    public Result<String> export(HttpServletResponse response, String month) throws IOException {
        // 查询所有在职员工
        List<Map<String, Object>> staffList = jdbc.queryForList(
                "SELECT s.staff_id, s.staff_code, s.staff_name, s.staff_phone, s.home_address, " +
                        "s.dept_id, d.dept_name " +
                        "FROM staff_master s LEFT JOIN department d ON s.dept_id = d.dept_id " +
                        "WHERE s.employment_status IN ('1', '2', '在职', '试用期')");

        String yearMonth = month.substring(0, 4) + "年" + month.substring(4) + "月";
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" +
                new String((yearMonth + "考勤报表.csv").getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1) + "\"");

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));
        // BOM for Excel UTF-8 compatibility
        writer.write('\ufeff');
        writer.println("员工工号,员工姓名,电话,地址,部门,迟到次数,早退次数,旷工次数,休假天数");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        for (Map<String, Object> staff : staffList) {
            Integer staffId = (Integer) staff.get("staff_id");

            // 迟到次数
            Integer lateTimes = hrAttendanceRepository.countByStaffIdAndStatusAndMonth(staffId, STATUS_LATE, month);
            // 早退次数
            Integer leaveEarlyTimes = hrAttendanceRepository.countByStaffIdAndStatusAndMonth(staffId, STATUS_LEAVE_EARLY, month);
            // 旷工次数
            Integer absenteeismTimes = hrAttendanceRepository.countByStaffIdAndStatusAndMonth(staffId, STATUS_ABSENTEEISM, month);
            // 休假天数（排除周末）
            List<LocalDate> leaveDateList = hrAttendanceRepository.findLeaveDateByStaffIdAndStatusAndMonth(staffId, STATUS_LEAVE, month);
            int leaveDays = 0;
            for (LocalDate date : leaveDateList) {
                if (!isWeekend(date)) {
                    leaveDays++;
                }
            }

            writer.printf("%s,%s,%s,%s,%s,%d,%d,%d,%d%n",
                    nvl(staff.get("staff_code")),
                    nvl(staff.get("staff_name")),
                    nvl(staff.get("staff_phone")),
                    nvl(staff.get("home_address")),
                    nvl(staff.get("dept_name")),
                    lateTimes != null ? lateTimes : 0,
                    leaveEarlyTimes != null ? leaveEarlyTimes : 0,
                    absenteeismTimes != null ? absenteeismTimes : 0,
                    leaveDays);
        }
        writer.flush();
        return Result.success();
    }

    // ==================== 导入 ====================

    /**
     * 数据导入（CSV格式）
     * 根据员工打卡时间和部门规定的工作时间，自动判断考勤状态
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<String> imp(MultipartFile file) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        // 跳过表头
        String header = reader.readLine();
        if (header == null) {
            return Result.error(500, "文件为空");
        }

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String line;
        int successCount = 0;
        int skipCount = 0;
        while ((line = reader.readLine()) != null) {
            String[] cols = line.split(",");
            if (cols.length < 6) continue;

            try {
                // CSV格式: staffId, morStartTime, morEndTime, aftStartTime, aftEndTime, attendanceDate
                Integer staffId = Integer.parseInt(cols[0].trim());
                LocalTime morStart = cols[1].trim().isEmpty() ? null : LocalTime.parse(cols[1].trim(), timeFormatter);
                LocalTime morEnd = cols[2].trim().isEmpty() ? null : LocalTime.parse(cols[2].trim(), timeFormatter);
                LocalTime aftStart = cols[3].trim().isEmpty() ? null : LocalTime.parse(cols[3].trim(), timeFormatter);
                LocalTime aftEnd = cols[4].trim().isEmpty() ? null : LocalTime.parse(cols[4].trim(), timeFormatter);
                LocalDate attendanceDate = LocalDate.parse(cols[5].trim(), dateFormatter);

                HrAttendance attendance = new HrAttendance();
                attendance.setStaffId(staffId);
                attendance.setMorStartTime(morStart);
                attendance.setMorEndTime(morEnd);
                attendance.setAftStartTime(aftStart);
                attendance.setAftEndTime(aftEnd);
                attendance.setAttendanceDate(attendanceDate);

                // 判断是否是周末，如果是周末就不记录；判断是否请假
                if (staffId == null || attendanceDate == null || isWeekend(attendanceDate) || isLeave(attendance)) {
                    skipCount++;
                    continue;
                }

                // 获取部门信息
                Map<String, Object> dept = getDeptByStaffId(staffId);
                if (dept == null) {
                    skipCount++;
                    continue;
                }

                // 设置门店ID
                Object storeIdObj = dept.get("store_id");
                if (storeIdObj != null) {
                    attendance.setStoreId(((Number) storeIdObj).longValue());
                } else {
                    attendance.setStoreId(1L);
                }

                // 判断考勤状态
                if (isAbsenteeism(attendance, dept)) {
                    attendance.setStatus(STATUS_ABSENTEEISM);
                } else if (isLate(attendance, dept)) {
                    attendance.setStatus(STATUS_LATE);
                } else if (isLeaveEarly(attendance, dept)) {
                    attendance.setStatus(STATUS_LEAVE_EARLY);
                } else {
                    attendance.setStatus(STATUS_NORMAL);
                }

                // 按staffId和日期查找已存在的记录，存在则更新
                Optional<HrAttendance> existing = hrAttendanceRepository.findByStaffIdAndAttendanceDate(staffId, attendanceDate);
                if (existing.isPresent()) {
                    HrAttendance exist = existing.get();
                    exist.setMorStartTime(morStart);
                    exist.setMorEndTime(morEnd);
                    exist.setAftStartTime(aftStart);
                    exist.setAftEndTime(aftEnd);
                    exist.setStatus(attendance.getStatus());
                    exist.setStoreId(attendance.getStoreId());
                    hrAttendanceRepository.save(exist);
                } else {
                    hrAttendanceRepository.save(attendance);
                }
                successCount++;
            } catch (Exception e) {
                // 跳过格式错误的行
                skipCount++;
            }
        }
        reader.close();
        return Result.success("导入成功：" + successCount + "条，跳过：" + skipCount + "条");
    }

    // ==================== 查询 ====================

    /**
     * 查找员工最近一次休假的日期
     */
    public Result<HrAttendance> findByStaffId(Integer id) {
        List<HrAttendance> list = hrAttendanceRepository.findByStaffIdAndIsDeleted(id, 0);
        // 找最近的休假记录
        HrAttendance latestLeave = null;
        for (HrAttendance att : list) {
            if (att.getStatus() != null && att.getStatus() == STATUS_LEAVE) {
                if (latestLeave == null || (att.getAttendanceDate() != null &&
                        latestLeave.getAttendanceDate() != null &&
                        att.getAttendanceDate().isAfter(latestLeave.getAttendanceDate()))) {
                    latestLeave = att;
                }
            }
        }
        return Result.success(latestLeave);
    }

    /**
     * 按员工ID和日期查询
     */
    public Result<HrAttendance> findByStaffIdAndDate(Integer id, String date) {
        HrAttendance attendance = hrAttendanceRepository.findByStaffIdAndDay(id, date.replace("-", ""));
        if (attendance != null) {
            return Result.success(attendance);
        }
        return Result.error(500, "未找到");
    }

    /**
     * 设置考勤（存在则更新，不存在则新增）
     */
    public Result<HrAttendance> setAttendance(HrAttendance attendance) {
        if (attendance.getStaffId() != null && attendance.getAttendanceDate() != null) {
            Optional<HrAttendance> existing = hrAttendanceRepository.findByStaffIdAndAttendanceDate(
                    attendance.getStaffId(), attendance.getAttendanceDate());
            if (existing.isPresent()) {
                HrAttendance exist = existing.get();
                exist.setMorStartTime(attendance.getMorStartTime());
                exist.setMorEndTime(attendance.getMorEndTime());
                exist.setAftStartTime(attendance.getAftStartTime());
                exist.setAftEndTime(attendance.getAftEndTime());
                exist.setStatus(attendance.getStatus());
                exist.setRemark(attendance.getRemark());
                exist.setStoreId(attendance.getStoreId());
                HrAttendance saved = hrAttendanceRepository.save(exist);
                return Result.success(saved);
            }
        }
        HrAttendance saved = hrAttendanceRepository.save(attendance);
        return Result.success(saved);
    }

    /**
     * 获取所有考勤状态枚举
     */
    public Result<List<Map<String, Object>>> findAll() {
        List<Map<String, Object>> enumList = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : STATUS_MAP.entrySet()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("code", entry.getKey());
            map.put("message", entry.getValue());
            map.put("tagType", STATUS_TAG_TYPE_MAP.get(entry.getKey()));
            enumList.add(map);
        }
        return Result.success(enumList);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 是否迟到
     */
    private boolean isLate(HrAttendance attendance, Map<String, Object> dept) {
        java.sql.Time deptMorStart = (java.sql.Time) dept.get("mor_start_time");
        java.sql.Time deptAftStart = (java.sql.Time) dept.get("aft_start_time");

        if (deptMorStart != null && attendance.getMorStartTime() != null) {
            if (attendance.getMorStartTime().isAfter(deptMorStart.toLocalTime())) {
                return true;
            }
        }
        if (deptAftStart != null && attendance.getAftStartTime() != null) {
            if (attendance.getAftStartTime().isAfter(deptAftStart.toLocalTime())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否早退
     */
    private boolean isLeaveEarly(HrAttendance attendance, Map<String, Object> dept) {
        java.sql.Time deptMorEnd = (java.sql.Time) dept.get("mor_end_time");
        java.sql.Time deptAftEnd = (java.sql.Time) dept.get("aft_end_time");

        if (deptMorEnd != null && attendance.getMorEndTime() != null) {
            if (attendance.getMorEndTime().isBefore(deptMorEnd.toLocalTime())) {
                return true;
            }
        }
        if (deptAftEnd != null && attendance.getAftEndTime() != null) {
            if (attendance.getAftEndTime().isBefore(deptAftEnd.toLocalTime())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否旷工：4个打卡时间有一个为null则旷工；既迟到又早退也视为旷工
     */
    private boolean isAbsenteeism(HrAttendance attendance, Map<String, Object> dept) {
        if (attendance.getMorStartTime() == null || attendance.getMorEndTime() == null
                || attendance.getAftStartTime() == null || attendance.getAftEndTime() == null) {
            return true;
        }
        return isLate(attendance, dept) && isLeaveEarly(attendance, dept);
    }

    /**
     * 判断员工是否请假（当日已有休假记录）
     */
    private boolean isLeave(HrAttendance attendance) {
        if (attendance.getStaffId() == null || attendance.getAttendanceDate() == null) {
            return false;
        }
        Optional<HrAttendance> existing = hrAttendanceRepository.findByStaffIdAndAttendanceDate(
                attendance.getStaffId(), attendance.getAttendanceDate());
        return existing.isPresent() && existing.get().getStatus() != null
                && existing.get().getStatus() == STATUS_LEAVE;
    }

    /**
     * 获取员工所属部门信息
     */
    private Map<String, Object> getDeptByStaffId(Integer staffId) {
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT d.* FROM department d INNER JOIN staff_master s ON s.dept_id = d.dept_id WHERE s.staff_id = ? LIMIT 1",
                staffId);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 获取指定月份的所有日期字符串 yyyyMMdd
     */
    private String[] getMonthDayList(String month) {
        int year = Integer.parseInt(month.substring(0, 4));
        int mon = Integer.parseInt(month.substring(4, 6));
        java.time.YearMonth yearMonth = java.time.YearMonth.of(year, mon);
        int daysInMonth = yearMonth.lengthOfMonth();
        String[] days = new String[daysInMonth];
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        for (int i = 0; i < daysInMonth; i++) {
            days[i] = yearMonth.atDay(i + 1).format(formatter);
        }
        return days;
    }

    /**
     * 判断是否是周末
     */
    private boolean isWeekend(LocalDate date) {
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY;
    }

    /**
     * null转空字符串
     */
    private String nvl(Object obj) {
        return obj == null ? "" : obj.toString();
    }
}