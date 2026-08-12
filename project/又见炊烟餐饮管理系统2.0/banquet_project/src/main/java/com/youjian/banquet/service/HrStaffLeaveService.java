package com.youjian.banquet.service;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.Attendance;
import com.youjian.banquet.entity.HrStaffLeave;
import com.youjian.banquet.repository.AttendanceRepository;
import com.youjian.banquet.repository.HrStaffLeaveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 员工请假 Service
 * 对应参考系统: StaffLeaveService
 */
@Service
public class HrStaffLeaveService {

    @Autowired
    private HrStaffLeaveRepository hrStaffLeaveRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    /**
     * 审核状态枚举
     */
    private static final int STATUS_UNAUDITED = 0;
    private static final int STATUS_APPROVE = 1;
    private static final int STATUS_REJECT = 2;
    private static final int STATUS_CANCEL = 3;

    private static final List<Map<String, Object>> AUDIT_STATUS_ENUMS = new ArrayList<>();

    static {
        AUDIT_STATUS_ENUMS.add(createEnumMap(0, "未审核", "info"));
        AUDIT_STATUS_ENUMS.add(createEnumMap(1, "审核通过", "success"));
        AUDIT_STATUS_ENUMS.add(createEnumMap(2, "驳回", "danger"));
        AUDIT_STATUS_ENUMS.add(createEnumMap(3, "撤销", ""));
    }

    private static Map<String, Object> createEnumMap(Integer code, String message, String tagType) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("message", message);
        map.put("tagType", tagType);
        return map;
    }

    public Result<HrStaffLeave> add(HrStaffLeave staffLeave) {
        HrStaffLeave saved = hrStaffLeaveRepository.save(staffLeave);
        if (saved != null) {
            return Result.success(saved);
        }
        return Result.error(500, "新增失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteById(Integer id) {
        Optional<HrStaffLeave> opt = hrStaffLeaveRepository.findById(id);
        if (opt.isPresent()) {
            HrStaffLeave entity = opt.get();
            entity.setIsDeleted(1);
            hrStaffLeaveRepository.save(entity);
            return Result.success();
        }
        return Result.error(500, "删除失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteBatch(List<Integer> ids) {
        List<HrStaffLeave> entities = hrStaffLeaveRepository.findAllById(ids);
        for (HrStaffLeave entity : entities) {
            entity.setIsDeleted(1);
        }
        hrStaffLeaveRepository.saveAll(entities);
        return Result.success();
    }

    /**
     * 设置请假（编辑），当请假通过之后，将休假的考勤状态设为休假
     * 对应参考系统: edit(StaffLeave staffLeave)
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<HrStaffLeave> edit(HrStaffLeave staffLeave) {
        if (staffLeave.getId() == null) {
            return Result.error(500, "ID不能为空");
        }

        // 如果审核通过，将考勤日的状态设置为休假
        if (staffLeave.getStatus() != null && staffLeave.getStatus() == STATUS_APPROVE) {
            Integer days = staffLeave.getDays();
            LocalDate startDate = staffLeave.getStartDate();
            if (days != null && startDate != null) {
                for (int i = 0; i < days; i++) {
                    LocalDate attendanceDate = startDate.plusDays(i);
                    // 因为周末本就要休息，所以只需记录在休假期间包括的工作日的考勤状态
                    java.time.DayOfWeek dayOfWeek = attendanceDate.getDayOfWeek();
                    if (dayOfWeek != java.time.DayOfWeek.SATURDAY && dayOfWeek != java.time.DayOfWeek.SUNDAY) {
                        // 查找是否已有该员工该日期的考勤记录
                        List<Attendance> existingAttendances = attendanceRepository.findByStaffId(staffLeave.getStaffId());
                        Optional<Attendance> existingOpt = existingAttendances.stream()
                                .filter(a -> attendanceDate.equals(a.getAttendanceDate()))
                                .findFirst();

                        Attendance attendance;
                        if (existingOpt.isPresent()) {
                            attendance = existingOpt.get();
                            attendance.setStatus("4"); // 休假状态
                        } else {
                            attendance = new Attendance();
                            attendance.setStaffId(staffLeave.getStaffId());
                            attendance.setStoreId(staffLeave.getStoreId());
                            attendance.setAttendanceDate(attendanceDate);
                            attendance.setStatus("4"); // 休假状态
                        }
                        attendanceRepository.save(attendance);
                    }
                }
            }
        }

        HrStaffLeave updated = hrStaffLeaveRepository.save(staffLeave);
        if (updated != null) {
            return Result.success(updated);
        }
        return Result.error(500, "更新失败");
    }

    public Result<HrStaffLeave> findById(Integer id) {
        Optional<HrStaffLeave> opt = hrStaffLeaveRepository.findById(id);
        if (opt.isPresent()) {
            return Result.success(opt.get());
        }
        return Result.error(500, "未找到记录");
    }

    /**
     * 分页条件查询
     * 对应参考系统: list(Integer current, Integer size, String name, Integer deptId)
     */
    public Result<Map<String, Object>> list(Long storeId, Integer current, Integer size, String name, Integer deptId) {
        int page = (current != null && current > 0) ? current - 1 : 0;
        int pageSize = (size != null && size > 0) ? size : 10;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));

        String searchName = (name == null || name.isEmpty()) ? null : name;

        Page<Object[]> resultPage;
        if (deptId == null) {
            resultPage = hrStaffLeaveRepository.listStaffLeaveVO(storeId, STATUS_CANCEL, searchName, pageable);
        } else {
            resultPage = hrStaffLeaveRepository.listStaffDeptLeaveVO(storeId, STATUS_CANCEL, searchName, deptId, pageable);
        }

        List<Object[]> records = resultPage.getContent();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : records) {
            HrStaffLeave staffLeave = parseStaffLeaveFromRow(row);
            Map<String, Object> map = new HashMap<>();
            map.put("staffLeave", staffLeave);
            map.put("tagType", getTagType(staffLeave.getStatus()));
            map.put("approve", STATUS_APPROVE);
            map.put("reject", STATUS_REJECT);
            map.put("unaudited", STATUS_UNAUDITED);
            list.add(map);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("pages", resultPage.getTotalPages());
        result.put("total", resultPage.getTotalElements());
        result.put("list", list);
        return Result.success(result);
    }

    /**
     * 数据导出
     * 对应参考系统: export(HttpServletResponse response)
     */
    public Result<Void> export(HttpServletResponse response, Long storeId) throws IOException {
        List<HrStaffLeave> list = hrStaffLeaveRepository.findByStoreIdAndIsDeleted(storeId, 0);

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=staff_leave_" + LocalDate.now() + ".csv");
        response.setCharacterEncoding("UTF-8");
        // 写入BOM以支持Excel打开中文
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        try (PrintWriter writer = new PrintWriter(response.getOutputStream(), true, StandardCharsets.UTF_8)) {
            writer.println("ID,门店ID,员工ID,请假天数,请假类型,开始日期,状态,备注,创建时间,更新时间");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (HrStaffLeave sl : list) {
                writer.println(
                        String.join(",",
                                String.valueOf(sl.getId()),
                                String.valueOf(sl.getStoreId()),
                                String.valueOf(sl.getStaffId()),
                                String.valueOf(sl.getDays()),
                                String.valueOf(sl.getTypeNum()),
                                sl.getStartDate() != null ? sl.getStartDate().toString() : "",
                                String.valueOf(sl.getStatus()),
                                sl.getRemark() != null ? "\"" + sl.getRemark() + "\"" : "",
                                sl.getCreateTime() != null ? sl.getCreateTime().format(dtf) : "",
                                sl.getUpdateTime() != null ? sl.getUpdateTime().format(dtf) : ""
                        )
                );
            }
        }
        return Result.success();
    }

    /**
     * 数据导入
     * 对应参考系统: imp(MultipartFile file)
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> imp(MultipartFile file, Long storeId) throws IOException {
        List<HrStaffLeave> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue; // 跳过标题行
                }
                String[] fields = parseCsvLine(line);
                if (fields.length >= 10) {
                    HrStaffLeave sl = new HrStaffLeave();
                    sl.setStoreId(storeId);
                    sl.setStaffId(parseIntSafe(fields[2]));
                    sl.setDays(parseIntSafe(fields[3]));
                    sl.setTypeNum(parseIntSafe(fields[4]));
                    if (fields[5] != null && !fields[5].isEmpty()) {
                        sl.setStartDate(LocalDate.parse(fields[5]));
                    }
                    sl.setStatus(parseIntSafe(fields[6]));
                    sl.setRemark(fields[7].replace("\"", ""));
                    list.add(sl);
                }
            }
        }

        if (!list.isEmpty()) {
            hrStaffLeaveRepository.saveAll(list);
        }
        return Result.success();
    }

    /**
     * 根据员工ID分页查询
     * 对应参考系统: findByStaffId(Integer current, Integer size, Integer id)
     */
    public Result<Map<String, Object>> findByStaffId(Long storeId, Integer current, Integer size, Integer staffId) {
        int page = (current != null && current > 0) ? current - 1 : 0;
        int pageSize = (size != null && size > 0) ? size : 10;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));

        Page<HrStaffLeave> resultPage = hrStaffLeaveRepository.findByStaffIdAndIsDeleted(staffId, 0, pageable);

        List<Map<String, Object>> list = new ArrayList<>();
        for (HrStaffLeave staffLeave : resultPage.getContent()) {
            Map<String, Object> map = new HashMap<>();
            map.put("staffLeave", staffLeave);
            map.put("tagType", getTagType(staffLeave.getStatus()));
            map.put("unaudited", STATUS_UNAUDITED);
            map.put("cancel", STATUS_CANCEL);
            list.add(map);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("pages", resultPage.getTotalPages());
        result.put("total", resultPage.getTotalElements());
        result.put("list", list);
        return Result.success(result);
    }

    /**
     * 查找未被审批的申请
     * 对应参考系统: findUnauditedByStaffId(Integer id)
     */
    public Result<Void> findUnauditedByStaffId(Long storeId, Integer staffId) {
        List<HrStaffLeave> list = hrStaffLeaveRepository.findByStaffIdAndStatusAndIsDeleted(staffId, STATUS_UNAUDITED, 0);
        if (!list.isEmpty()) {
            return Result.success();
        }
        return Result.error(500, "无未审核记录");
    }

    /**
     * 获取所有审核状态枚举
     * 对应参考系统: findAll() → EnumUtil.getEnumList(AuditStatusEnum.class)
     */
    public Result<List<Map<String, Object>>> findAll() {
        return Result.success(AUDIT_STATUS_ENUMS);
    }

    private String getTagType(Integer status) {
        for (Map<String, Object> enumMap : AUDIT_STATUS_ENUMS) {
            if (enumMap.get("code").equals(status)) {
                return (String) enumMap.get("tagType");
            }
        }
        return "";
    }

    private HrStaffLeave parseStaffLeaveFromRow(Object[] row) {
        HrStaffLeave sl = new HrStaffLeave();
        if (row.length > 0 && row[0] != null) sl.setId(((Number) row[0]).intValue());
        if (row.length > 1 && row[1] != null) sl.setStoreId(((Number) row[1]).longValue());
        if (row.length > 2 && row[2] != null) sl.setStaffId(((Number) row[2]).intValue());
        if (row.length > 3 && row[3] != null) sl.setDays(((Number) row[3]).intValue());
        if (row.length > 4 && row[4] != null) sl.setTypeNum(((Number) row[4]).intValue());
        if (row.length > 5 && row[5] != null) {
            if (row[5] instanceof java.sql.Date) {
                sl.setStartDate(((java.sql.Date) row[5]).toLocalDate());
            } else if (row[5] instanceof LocalDate) {
                sl.setStartDate((LocalDate) row[5]);
            }
        }
        if (row.length > 6 && row[6] != null) sl.setStatus(((Number) row[6]).intValue());
        if (row.length > 7 && row[7] != null) sl.setRemark(row[7].toString());
        if (row.length > 8 && row[8] != null) {
            if (row[8] instanceof java.sql.Timestamp) {
                sl.setCreateTime(((java.sql.Timestamp) row[8]).toLocalDateTime());
            } else if (row[8] instanceof LocalDateTime) {
                sl.setCreateTime((LocalDateTime) row[8]);
            }
        }
        if (row.length > 9 && row[9] != null) {
            if (row[9] instanceof java.sql.Timestamp) {
                sl.setUpdateTime(((java.sql.Timestamp) row[9]).toLocalDateTime());
            } else if (row[9] instanceof LocalDateTime) {
                sl.setUpdateTime((LocalDateTime) row[9]);
            }
        }
        return sl;
    }

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(sb.toString());
                sb = new StringBuilder();
            } else {
                sb.append(c);
            }
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }

    private Integer parseIntSafe(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}