package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.JobPosting;
import com.youjian.banquet.repository.JobPostingRepository;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 招聘岗位信息。/open 是唯一公开(免登录)接口，供员工自助登记页
 * "点击岗位信息"浏览——应聘人此时还没有账号，见 WebMvcConfig 里的放行配置。
 * 其余管理接口(列表全部/新增/编辑/关闭)要求 HR 或总经理登录(can_manage_hr=1)。
 */
@RestController
@RequestMapping("/api/hr/job-postings")
@CrossOrigin(origins = "*")
public class JobPostingController {

    @Autowired
    private JobPostingRepository jobPostingRepo;

    @Autowired
    private JdbcTemplate jdbc;

    /** GET /api/hr/job-postings/open —— 公开接口，只返回招聘中的岗位。 */
    @GetMapping("/open")
    public Result<List<Map<String, Object>>> listOpen() {
        List<JobPosting> list = jobPostingRepo.findByStatusOrderByCreatedAtDesc("open");
        return Result.success(list.stream().map(this::toRow).toList());
    }

    /** GET /api/hr/job-postings —— HR/总经理查看全部岗位(含已关闭)。 */
    @GetMapping
    public Result<List<Map<String, Object>>> listAll() {
        try {
            assertHrAccess();
            return Result.success(jobPostingRepo.findAllByOrderByCreatedAtDesc().stream().map(this::toRow).toList());
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        }
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        try {
            assertHrAccess();
            String department = asString(body.get("department"));
            String position = asString(body.get("position"));
            if (department == null || position == null) return Result.error(400, "部门和职位不能为空");

            JobPosting jp = new JobPosting();
            jp.setStoreId(asLong(body.get("storeId"), 1L));
            jp.setDepartment(department);
            jp.setPosition(position);
            jp.setHeadcount(asInt(body.get("headcount"), 1));
            jp.setSalaryRange(asString(body.get("salaryRange")));
            jp.setWorkTime(asString(body.get("workTime")));
            jp.setRequirements(asString(body.get("requirements")));
            jp.setDescription(asString(body.get("description")));
            jp.setStatus("open");
            Long staffId = UserContext.getStaffId();
            jp.setCreatedBy(staffId != null ? staffId.intValue() : null);
            jp.setCreatedAt(LocalDateTime.now());
            jp.setUpdatedAt(LocalDateTime.now());
            JobPosting saved = jobPostingRepo.save(jp);
            return Result.success(Map.of("id", saved.getId()));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            assertHrAccess();
            JobPosting jp = jobPostingRepo.findById(id).orElse(null);
            if (jp == null) return Result.error(404, "岗位不存在");
            if (body.containsKey("department")) jp.setDepartment(asString(body.get("department")));
            if (body.containsKey("position")) jp.setPosition(asString(body.get("position")));
            if (body.containsKey("headcount")) jp.setHeadcount(asInt(body.get("headcount"), jp.getHeadcount()));
            if (body.containsKey("salaryRange")) jp.setSalaryRange(asString(body.get("salaryRange")));
            if (body.containsKey("workTime")) jp.setWorkTime(asString(body.get("workTime")));
            if (body.containsKey("requirements")) jp.setRequirements(asString(body.get("requirements")));
            if (body.containsKey("description")) jp.setDescription(asString(body.get("description")));
            if (body.containsKey("status")) jp.setStatus(asString(body.get("status")));
            jp.setUpdatedAt(LocalDateTime.now());
            jobPostingRepo.save(jp);
            return Result.success(null);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        }
    }

    @PostMapping("/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        try {
            assertHrAccess();
            JobPosting jp = jobPostingRepo.findById(id).orElse(null);
            if (jp == null) return Result.error(404, "岗位不存在");
            jp.setStatus("closed");
            jp.setUpdatedAt(LocalDateTime.now());
            jobPostingRepo.save(jp);
            return Result.success(null);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            assertHrAccess();
            jobPostingRepo.deleteById(id);
            return Result.success(null);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        }
    }

    private Map<String, Object> toRow(JobPosting jp) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("id", jp.getId());
        row.put("storeId", jp.getStoreId());
        row.put("department", jp.getDepartment());
        row.put("position", jp.getPosition());
        row.put("headcount", jp.getHeadcount());
        row.put("salaryRange", jp.getSalaryRange());
        row.put("workTime", jp.getWorkTime());
        row.put("requirements", jp.getRequirements());
        row.put("description", jp.getDescription());
        row.put("status", jp.getStatus());
        row.put("createdAt", jp.getCreatedAt());
        return row;
    }

    private static String asString(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static Long asLong(Object v, Long def) {
        if (v == null) return def;
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return def; }
    }

    private static Integer asInt(Object v, Integer def) {
        if (v == null) return def;
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return def; }
    }

    /** 复用 SelfServiceController 同款校验：登录 + can_manage_hr=1 才能管理岗位。总经理不受此限制。 */
    private void assertHrAccess() {
        if (UserContext.isDataScopeAll()) return;
        Long currentStaffId = UserContext.getStaffId();
        if (currentStaffId == null) throw new SecurityException("未登录，无法管理招聘岗位");
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT can_manage_hr FROM staff_master WHERE staff_id = ? LIMIT 1", currentStaffId.intValue());
        if (rows.isEmpty()) throw new SecurityException("无权管理招聘岗位");
        int canManageHr = rows.get(0).get("can_manage_hr") == null ? 0 : ((Number) rows.get(0).get("can_manage_hr")).intValue();
        if (canManageHr != 1) throw new SecurityException("无权管理招聘岗位");
    }
}
