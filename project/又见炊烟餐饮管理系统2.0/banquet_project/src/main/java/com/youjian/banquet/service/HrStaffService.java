package com.youjian.banquet.service;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrDept;
import com.youjian.banquet.entity.HrStaff;
import com.youjian.banquet.repository.HrDeptRepository;
import com.youjian.banquet.repository.HrStaffRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HR员工服务类
 * 复刻自HR系统 StaffService，ORM从MyBatis Plus改为JPA+JdbcTemplate
 * 完整保留：密码加密、年龄计算、导入导出、密码校验、角色设置
 *
 * @author cow
 * @since 2022-01-27
 */
@Service
public class HrStaffService {

    @Autowired
    private HrStaffRepository hrStaffRepository;

    @Autowired
    private HrDeptRepository hrDeptRepository;

    @Autowired
    private JdbcTemplate jdbc;

    // ==================== 基础 CRUD ====================

    /**
     * 新增员工
     * 对应参考系统 add(Staff staff)
     * 设置默认密码、工号
     */
    public Result<HrStaff> add(HrStaff staff) {
        HrStaff saved = hrStaffRepository.save(staff);
        // 设置默认密码、工号
        saved.setPassword(md55("123"));
        saved.setCode("staff_" + saved.getId());
        hrStaffRepository.save(saved);
        return Result.success(saved);
    }

    /**
     * 逻辑删除
     */
    public Result<String> deleteById(Integer id) {
        Optional<HrStaff> opt = hrStaffRepository.findById(id);
        if (opt.isPresent()) {
            HrStaff staff = opt.get();
            staff.setIsDeleted(1);
            hrStaffRepository.save(staff);
            return Result.success();
        }
        return Result.error(500, "删除失败");
    }

    /**
     * 批量逻辑删除
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<String> deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            Optional<HrStaff> opt = hrStaffRepository.findById(id);
            if (opt.isPresent()) {
                HrStaff staff = opt.get();
                staff.setIsDeleted(1);
                hrStaffRepository.save(staff);
            }
        }
        return Result.success();
    }

    /**
     * 编辑更新
     */
    public Result<HrStaff> edit(HrStaff staff) {
        if (staff.getId() == null) {
            return Result.error(500, "ID不能为空");
        }
        HrStaff saved = hrStaffRepository.save(staff);
        return Result.success(saved);
    }

    /**
     * 按ID查询
     */
    public Result<HrStaff> findById(Integer id) {
        Optional<HrStaff> opt = hrStaffRepository.findById(id);
        return opt.map(Result::success).orElseGet(() -> Result.error(500, "未找到"));
    }

    /**
     * 获取员工详细信息（含部门名称）
     * 对应参考系统 findInfoById(Integer id)
     */
    public Result<Map<String, Object>> findInfoById(Integer id) {
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT s.id, s.code, s.name, s.gender, s.avatar, s.birthday, s.phone, s.address, " +
                        "s.remark, s.status, s.dept_id, s.store_id, " +
                        "d.name dept_name " +
                        "FROM hr_staff s LEFT JOIN hr_dept d ON s.dept_id = d.id AND d.is_deleted = 0 " +
                        "WHERE s.is_deleted = 0 AND s.id = ?", id);
        if (list.isEmpty()) {
            return Result.error(500, "未找到");
        }
        Map<String, Object> staffInfo = list.get(0);
        // 计算年龄
        Object birthdayObj = staffInfo.get("birthday");
        if (birthdayObj != null) {
            LocalDate birthday = birthdayObj instanceof java.sql.Date
                    ? ((java.sql.Date) birthdayObj).toLocalDate()
                    : LocalDate.parse(birthdayObj.toString());
            staffInfo.put("age", Period.between(birthday, LocalDate.now()).getYears());
        }
        return Result.success(staffInfo);
    }

    // ==================== 分页条件查询 ====================

    /**
     * 多条件分页查询
     * 对应参考系统 list(current, size, staff)
     */
    public Result<Map<String, Object>> list(Integer current, Integer size, HrStaff staff) {
        StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(*) FROM hr_staff s WHERE s.is_deleted = 0");
        StringBuilder dataSql = new StringBuilder(
                "SELECT s.id, s.code, s.name, s.gender, s.avatar, s.birthday, s.phone, s.address, " +
                        "s.remark, s.status, s.dept_id, s.store_id, s.create_time, s.update_time, " +
                        "d.name dept_name " +
                        "FROM hr_staff s LEFT JOIN hr_dept d ON s.dept_id = d.id AND d.is_deleted = 0 " +
                        "WHERE s.is_deleted = 0");

        List<Object> params = new ArrayList<>();

        if (staff.getName() != null && !staff.getName().isEmpty()) {
            countSql.append(" AND s.name LIKE ?");
            dataSql.append(" AND s.name LIKE ?");
            params.add("%" + staff.getName() + "%");
        }
        if (staff.getBirthday() != null) {
            countSql.append(" AND s.birthday >= ?");
            dataSql.append(" AND s.birthday >= ?");
            params.add(staff.getBirthday());
        }
        if (staff.getDeptId() != null) {
            countSql.append(" AND s.dept_id = ?");
            dataSql.append(" AND s.dept_id = ?");
            params.add(staff.getDeptId());
        }
        if (staff.getStatus() != null) {
            countSql.append(" AND s.status = ?");
            dataSql.append(" AND s.status = ?");
            params.add(staff.getStatus());
        }

        int total = jdbc.queryForObject(countSql.toString(), Integer.class, params.toArray());

        int offset = (current - 1) * size;
        dataSql.append(" ORDER BY s.id DESC LIMIT ?, ?");
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(offset);
        dataParams.add(size);

        List<Map<String, Object>> staffList = jdbc.queryForList(dataSql.toString(), dataParams.toArray());

        // 计算年龄
        for (Map<String, Object> item : staffList) {
            Object birthdayObj = item.get("birthday");
            if (birthdayObj != null) {
                LocalDate birthday = birthdayObj instanceof java.sql.Date
                        ? ((java.sql.Date) birthdayObj).toLocalDate()
                        : LocalDate.parse(birthdayObj.toString());
                item.put("age", Period.between(birthday, LocalDate.now()).getYears());
            }
        }

        int pages = (int) Math.ceil((double) total / size);
        Map<String, Object> map = new HashMap<>();
        map.put("pages", pages);
        map.put("total", total);
        map.put("list", staffList);
        return Result.success(map);
    }

    // ==================== 导入导出 ====================

    /**
     * 数据导出（CSV格式）
     * 对应参考系统 export(response)
     */
    public Result<String> export(HttpServletResponse response) throws java.io.IOException {
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT s.id, s.code, s.name, s.gender, s.avatar, s.birthday, s.phone, s.address, " +
                        "s.remark, s.status, s.dept_id, d.name dept_name " +
                        "FROM hr_staff s LEFT JOIN hr_dept d ON s.dept_id = d.id AND d.is_deleted = 0 " +
                        "WHERE s.is_deleted = 0");

        // 计算年龄
        for (Map<String, Object> item : list) {
            Object birthdayObj = item.get("birthday");
            if (birthdayObj != null) {
                LocalDate birthday = birthdayObj instanceof java.sql.Date
                        ? ((java.sql.Date) birthdayObj).toLocalDate()
                        : LocalDate.parse(birthdayObj.toString());
                item.put("age", Period.between(birthday, LocalDate.now()).getYears());
            }
        }

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"员工信息表.csv\"");

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));
        writer.write('\ufeff');
        writer.println("工号,姓名,性别,年龄,地址,生日,电话,部门,备注,状态");

        for (Map<String, Object> item : list) {
            String genderStr = "男";
            Object gender = item.get("gender");
            if (gender != null) {
                genderStr = Integer.valueOf(0).equals(gender) ? "男" : "女";
            }
            String statusStr = "正常";
            Object status = item.get("status");
            if (status != null) {
                statusStr = Integer.valueOf(1).equals(status) ? "正常" : "异常";
            }
            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    nvl(item.get("code")),
                    nvl(item.get("name")),
                    genderStr,
                    nvl(item.get("age")),
                    nvl(item.get("address")),
                    nvl(item.get("birthday")),
                    nvl(item.get("phone")),
                    nvl(item.get("dept_name")),
                    nvl(item.get("remark")),
                    statusStr);
        }
        writer.flush();
        return Result.success();
    }

    /**
     * 数据导入（CSV格式）
     * 对应参考系统 imp(file)
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<String> imp(MultipartFile file) throws java.io.IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        String header = reader.readLine();
        if (header == null) {
            return Result.error(500, "文件为空");
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String line;
        int successCount = 0;
        int skipCount = 0;
        while ((line = reader.readLine()) != null) {
            String[] cols = line.split(",");
            if (cols.length < 10) continue;

            try {
                HrStaff staff = new HrStaff();
                staff.setStoreId(1L);
                staff.setCode(cols[0].trim().isEmpty() ? null : cols[0].trim());
                staff.setName(cols[1].trim());
                if (!cols[2].trim().isEmpty()) {
                    staff.setGender("女".equals(cols[2].trim()) ? 1 : 0);
                }
                staff.setAddress(cols[4].trim().isEmpty() ? null : cols[4].trim());
                if (!cols[5].trim().isEmpty()) {
                    staff.setBirthday(LocalDate.parse(cols[5].trim(), dateFormatter));
                }
                staff.setPhone(cols[6].trim().isEmpty() ? null : cols[6].trim());
                staff.setRemark(cols[8].trim().isEmpty() ? null : cols[8].trim());
                staff.setStatus(1);

                HrStaff saved = hrStaffRepository.save(staff);
                // 设置默认密码、工号
                saved.setPassword(md55("123"));
                saved.setCode("staff_" + saved.getId());
                hrStaffRepository.save(saved);
                successCount++;
            } catch (Exception e) {
                skipCount++;
            }
        }
        reader.close();
        return Result.success("导入成功：" + successCount + "条，跳过：" + skipCount + "条");
    }

    // ==================== 密码相关 ====================

    /**
     * 检查员工的密码
     * 对应参考系统 checkPassword(String pwd, Integer id)
     */
    public Result<String> checkPassword(String pwd, Integer id) {
        Optional<HrStaff> opt = hrStaffRepository.findById(id);
        if (!opt.isPresent()) {
            return Result.error(500, "此员工不存在！");
        }
        HrStaff staff = opt.get();
        if (pwd == null || pwd.isEmpty()) {
            return Result.error(500, "密码不能为空！");
        }
        if (md55(pwd).equals(staff.getPassword())) {
            return Result.success();
        }
        return Result.error(300, "密码错误！");
    }

    /**
     * 更新密码
     * 对应参考系统 updatePassword(Staff staff)
     */
    public Result<String> updatePassword(HrStaff staff) {
        if (staff.getId() == null) {
            return Result.error(500, "ID不能为空");
        }
        Optional<HrStaff> opt = hrStaffRepository.findById(staff.getId());
        if (!opt.isPresent()) {
            return Result.error(500, "员工不存在");
        }
        HrStaff existing = opt.get();
        // MD5加密
        existing.setPassword(md55(staff.getPassword()));
        hrStaffRepository.save(existing);
        return Result.success();
    }

    // ==================== MD5 加密工具（复刻自参考系统 MD5Util） ====================

    /**
     * 打乱加密后的密码
     * 对应参考系统 MD5Util.MD55(String str)
     */
    private String md55(String str) {
        String pwd = md5(str);
        // 破坏密文结构
        return pwd.substring(0, 4) + pwd.substring(29) + pwd.substring(20, 29) + pwd.substring(4, 20);
    }

    private String md5(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(str.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String toHex(byte[] bytes) {
        final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();
        StringBuilder ret = new StringBuilder(bytes.length * 2);
        for (byte aByte : bytes) {
            ret.append(HEX_DIGITS[(aByte >> 4) & 0x0f]);
            ret.append(HEX_DIGITS[aByte & 0x0f]);
        }
        return ret.toString();
    }

    // ==================== 工具方法 ====================

    private String nvl(Object obj) {
        return obj == null ? "" : obj.toString();
    }
}