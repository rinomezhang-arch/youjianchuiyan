package com.youjian.banquet.service;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrDept;
import com.youjian.banquet.repository.HrDeptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * HR部门服务类
 * 复刻自HR系统 DeptService，ORM从MyBatis Plus改为JPA+JdbcTemplate
 * 完整保留：树形结构、工作时长计算、导入导出
 *
 * @author cow
 * @since 2022-03-07
 */
@Service
public class HrDeptService {

    @Autowired
    private HrDeptRepository hrDeptRepository;

    @Autowired
    private JdbcTemplate jdbc;

    // ==================== 基础 CRUD ====================

    /**
     * 新增部门
     * 对应参考系统 add(Dept dept)
     * 父级部门（parentId=0）不需要计算上班时间
     */
    public Result<HrDept> add(HrDept dept) {
        // 父级部门不需要计算上班时间
        if (dept.getParentId() != null && dept.getParentId() != 0) {
            dept.setTotalWorkTime(calculateTotalWorkTime(dept));
        }
        HrDept saved = hrDeptRepository.save(dept);
        return Result.success(saved);
    }

    /**
     * 逻辑删除
     */
    public Result<String> deleteById(Integer id) {
        Optional<HrDept> opt = hrDeptRepository.findById(id);
        if (opt.isPresent()) {
            HrDept dept = opt.get();
            dept.setIsDeleted(1);
            hrDeptRepository.save(dept);
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
            Optional<HrDept> opt = hrDeptRepository.findById(id);
            if (opt.isPresent()) {
                HrDept dept = opt.get();
                dept.setIsDeleted(1);
                hrDeptRepository.save(dept);
            }
        }
        return Result.success();
    }

    /**
     * 编辑更新
     * 对应参考系统 edit(Dept dept)
     */
    public Result<HrDept> edit(HrDept dept) {
        if (dept.getId() == null) {
            return Result.error(500, "ID不能为空");
        }
        dept.setTotalWorkTime(calculateTotalWorkTime(dept));
        HrDept saved = hrDeptRepository.save(dept);
        return Result.success(saved);
    }

    /**
     * 按ID查询
     */
    public Result<HrDept> findById(Integer id) {
        Optional<HrDept> opt = hrDeptRepository.findById(id);
        return opt.map(Result::success).orElseGet(() -> Result.error(500, "未找到"));
    }

    // ==================== 树形结构查询 ====================

    /**
     * 查找所有部门（树形结构）
     * 对应参考系统 findAll()
     */
    public Result<List<HrDept>> findAll() {
        List<HrDept> list = hrDeptRepository.findByStoreIdAndIsDeleted(1L, 0);
        if (list.isEmpty()) {
            list = hrDeptRepository.findAll().stream()
                    .filter(d -> d.getIsDeleted() == 0)
                    .collect(Collectors.toList());
        }
        return Result.success(setSubDept(list));
    }

    /**
     * 分页条件查询（含子部门树形结构）
     * 对应参考系统 list(current, size, name)
     */
    public Result<Map<String, Object>> list(Integer current, Integer size, String name) {
        if (name == null) {
            name = "";
        }

        // 查询父级部门
        List<HrDept> parentList = hrDeptRepository.listParentDept(name);
        // 查询所有子部门
        List<HrDept> subList = hrDeptRepository.findSubDept();

        // 为父级部门设置子部门
        for (HrDept parentDept : parentList) {
            List<HrDept> children = subList.stream()
                    .filter(dept -> dept.getParentId().equals(parentDept.getId()))
                    .collect(Collectors.toList());
            parentDept.setChildren(children);
        }

        // 手动分页
        int total = parentList.size();
        int pages = (int) Math.ceil((double) total / size);
        int fromIndex = (current - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<HrDept> pageList = parentList.subList(Math.min(fromIndex, total), toIndex);

        Map<String, Object> map = new HashMap<>();
        map.put("pages", pages);
        map.put("total", total);
        map.put("list", pageList);
        return Result.success(map);
    }

    /**
     * 查询所有子部门
     * 对应参考系统 findAllSubDept()
     */
    public Result<List<HrDept>> findAllSubDept() {
        List<HrDept> list = hrDeptRepository.findSubDept();
        return Result.success(list);
    }

    // ==================== 导入导出 ====================

    /**
     * 数据导出（CSV格式）
     * 对应参考系统 export(response)
     */
    public Result<String> export(HttpServletResponse response) throws IOException {
        List<HrDept> list = hrDeptRepository.findSubDept();
        if (list.isEmpty()) {
            list = hrDeptRepository.findByStoreIdAndIsDeleted(1L, 0);
        }

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"部门数据.csv\"");

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));
        writer.write('\ufeff');
        writer.println("部门编码,部门名称,上午上班时间,上午下班时间,下午上班时间,下午下班时间,工作时长,备注,父级部门ID");

        for (HrDept dept : list) {
            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%d%n",
                    nvl(dept.getCode()),
                    nvl(dept.getName()),
                    dept.getMorStartTime() != null ? dept.getMorStartTime().toString() : "",
                    dept.getMorEndTime() != null ? dept.getMorEndTime().toString() : "",
                    dept.getAftStartTime() != null ? dept.getAftStartTime().toString() : "",
                    dept.getAftEndTime() != null ? dept.getAftEndTime().toString() : "",
                    dept.getTotalWorkTime() != null ? dept.getTotalWorkTime().toString() : "",
                    nvl(dept.getRemark()),
                    dept.getParentId() != null ? dept.getParentId() : 0);
        }
        writer.flush();
        return Result.success();
    }

    /**
     * 数据导入（CSV格式）
     * 对应参考系统 imp(file)
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<String> imp(MultipartFile file) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        String header = reader.readLine();
        if (header == null) {
            return Result.error(500, "文件为空");
        }

        String line;
        int successCount = 0;
        int skipCount = 0;
        while ((line = reader.readLine()) != null) {
            String[] cols = line.split(",");
            if (cols.length < 9) continue;

            try {
                HrDept dept = new HrDept();
                dept.setStoreId(1L);
                dept.setCode(cols[0].trim().isEmpty() ? null : cols[0].trim());
                dept.setName(cols[1].trim().isEmpty() ? null : cols[1].trim());
                dept.setMorStartTime(cols[2].trim().isEmpty() ? null : LocalTime.parse(cols[2].trim()));
                dept.setMorEndTime(cols[3].trim().isEmpty() ? null : LocalTime.parse(cols[3].trim()));
                dept.setAftStartTime(cols[4].trim().isEmpty() ? null : LocalTime.parse(cols[4].trim()));
                dept.setAftEndTime(cols[5].trim().isEmpty() ? null : LocalTime.parse(cols[5].trim()));
                dept.setRemark(cols[7].trim().isEmpty() ? null : cols[7].trim());
                dept.setParentId(Integer.parseInt(cols[8].trim()));

                // 计算工作时长
                if (dept.getParentId() != null && dept.getParentId() != 0) {
                    dept.setTotalWorkTime(calculateTotalWorkTime(dept));
                }

                hrDeptRepository.save(dept);
                successCount++;
            } catch (Exception e) {
                skipCount++;
            }
        }
        reader.close();
        return Result.success("导入成功：" + successCount + "条，跳过：" + skipCount + "条");
    }

    // ==================== 树形结构工具方法 ====================

    /**
     * 为父级部门设置子部门，使用流来处理数据，并返回父级部门
     * 对应参考系统 setSubDept(List<Dept> list)
     */
    public List<HrDept> setSubDept(List<HrDept> list) {
        // 父级部门
        List<HrDept> parentList = list.stream()
                .filter(dept -> dept.getParentId() == 0).collect(Collectors.toList());
        for (HrDept parentDept : parentList) {
            // 子部门
            List<HrDept> subList = list.stream()
                    .filter(dept -> dept.getParentId().equals(parentDept.getId())).collect(Collectors.toList());
            parentDept.setChildren(subList);
        }
        return parentList;
    }

    // ==================== 工作时长计算 ====================

    /**
     * 计算员工每天的上班时间
     * 对应参考系统 calculateTotalWorkTime(Dept dept)
     */
    public BigDecimal calculateTotalWorkTime(HrDept dept) {
        if (dept.getMorStartTime() == null || dept.getMorEndTime() == null
                || dept.getAftStartTime() == null || dept.getAftEndTime() == null) {
            return BigDecimal.ZERO;
        }

        long morDiff = dept.getMorEndTime().toSecondOfDay() - dept.getMorStartTime().toSecondOfDay();
        long aftDiff = dept.getAftEndTime().toSecondOfDay() - dept.getAftStartTime().toSecondOfDay();

        BigDecimal totalWorkTime = BigDecimal.valueOf((morDiff + aftDiff) / (60.0 * 60.0));
        return totalWorkTime.setScale(1, RoundingMode.HALF_UP);
    }

    // ==================== 工具方法 ====================

    private String nvl(Object obj) {
        return obj == null ? "" : obj.toString();
    }
}