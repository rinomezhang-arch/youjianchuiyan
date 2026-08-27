package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.SelfServiceSubmission;
import com.youjian.banquet.entity.StaffMaster;
import com.youjian.banquet.repository.SelfServiceSubmissionRepository;
import com.youjian.banquet.repository.StaffMasterRepository;
import com.youjian.banquet.util.AESUtil;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 员工自助入职登记（SelfService.vue 提交，ReviewQueue.vue 审核）。
 * <p>
 * 提交端(submit)是唯一不要求登录的接口——新员工扫码填资料时还没有账号，
 * 见 WebMvcConfig 里对 /api/hr/self-service/submit 的单独放行。查询列表/
 * 审核/驳回仍然要求 HR 或总经理登录(can_manage_hr=1)。
 * <p>
 * 范围说明：审核通过(approve)目前只把这条提交标记为"已通过"，并不会自动
 * 生成正式的 staff_master 记录——岗位/薪资/试用期/上班时间这些是公司这边
 * 要另外填的信息，不该由申请人自己的提交内容直接决定，所以"转正式员工"
 * 是 HR 看到"已通过"名单后，去人事管理页手动建档的下一步动作，不在这个
 * 接口里自动完成。
 */
@RestController
@RequestMapping("/api/hr/self-service")
@CrossOrigin(origins = "*")
public class SelfServiceController {

    @Autowired
    private SelfServiceSubmissionRepository submissionRepo;

    @Autowired
    private AESUtil aesUtil;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StaffMasterRepository staffRepo;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** POST /api/hr/self-service/submit —— 唯一公开(免登录)的接口。 */
    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(@RequestBody Map<String, Object> body) {
        try {
            String name = asString(body.get("name"));
            String phone = asString(body.get("phone"));
            if (name == null || name.isBlank()) return Result.error(400, "姓名不能为空");
            if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) return Result.error(400, "请填写正确的11位手机号");

            SelfServiceSubmission s = new SelfServiceSubmission();
            s.setStoreId(asLong(body.get("storeId"), 1L));
            s.setSubmitType(asString(body.get("submitType")) != null ? asString(body.get("submitType")) : "new");
            s.setJobPostingId(asLong(body.get("jobPostingId"), null));
            s.setName(name);
            s.setPhone(phone);
            String idCard = asString(body.get("idCard"));
            s.setIdCard(idCard != null && !idCard.isBlank() ? aesUtil.encrypt(idCard) : null);
            s.setDepartment(asString(body.get("department")));
            s.setPosition(asString(body.get("position")));
            s.setGender(asString(body.get("gender")));
            s.setAddress(asString(body.get("address")));
            s.setEmergencyContact(asString(body.get("emergencyContact")));
            s.setEmergencyPhone(asString(body.get("emergencyPhone")));
            s.setAvatarUrl(asString(body.get("avatarUrl")));
            s.setRemark(asString(body.get("remark")));
            s.setStatus("pending");
            s.setCreatedAt(LocalDateTime.now());
            SelfServiceSubmission saved = submissionRepo.save(s);
            return Result.success(Map.of("id", saved.getId()));
        } catch (Exception e) {
            return Result.error(500, "提交失败: " + e.getMessage());
        }
    }

    /** GET /api/hr/self-service/submissions —— HR/总经理查看全部提交(前端自行按状态分 tab)。 */
    @GetMapping("/submissions")
    public Result<List<Map<String, Object>>> listSubmissions() {
        try {
            assertHrAccess();
            Long storeId = resolveQueryStoreId();
            List<SelfServiceSubmission> list = storeId == null
                    ? submissionRepo.findAllByOrderByCreatedAtDesc()
                    : submissionRepo.findByStoreIdOrderByCreatedAtDesc(storeId);
            return Result.success(list.stream().map(this::toRow).toList());
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取列表失败: " + e.getMessage());
        }
    }

    /**
     * POST /api/hr/self-service/approve/{id} —— 通过后写入/更新正式员工档案(staff_master)。
     * submitType=new 且手机号未注册过：新建员工记录，账号=手机号，初始密码=手机号后6位
     * (BCrypt加密存储，HR应在系统里当面告知新人后引导其自行修改)。
     * submitType=update，或手机号已存在员工记录：把提交里的字段合并更新到该员工身上，
     * 不新建重复记录。
     * 岗位/薪资/试用期/上班时间这些公司侧才能定的信息不在这里自动生成，需要 HR 在
     * 人事管理页对该员工做进一步补充——申请人自己提交的内容不能替公司做这些决定。
     */
    @PostMapping("/approve/{id}")
    @Transactional
    public Result<Map<String, Object>> approve(@PathVariable Long id) {
        try {
            assertHrAccess();
            SelfServiceSubmission s = submissionRepo.findById(id).orElse(null);
            if (s == null) return Result.error(404, "提交记录不存在");
            if (!"pending".equals(s.getStatus())) return Result.error(400, "该记录已处理，不能重复审核");

            StaffMaster staff = staffRepo.findByStaffPhone(s.getPhone()).orElse(null);
            boolean isNewStaff = staff == null;
            if (staff == null) {
                staff = new StaffMaster();
                staff.setStoreId(s.getStoreId() != null ? s.getStoreId() : 1L);
                staff.setStaffNo("SN" + System.currentTimeMillis());
                staff.setStaffAccount(s.getPhone());
                staff.setStaffPassword(passwordEncoder.encode(s.getPhone().substring(Math.max(0, s.getPhone().length() - 6))));
                staff.setRole("staff");
                staff.setPermissionLevel(1);
                staff.setEmploymentStatus("active");
            }
            staff.setStaffName(s.getName());
            staff.setStaffPhone(s.getPhone());
            if (s.getIdCard() != null) staff.setIdCard(s.getIdCard()); // 已是 AES 密文，直接复用
            if (s.getDepartment() != null) staff.setDepartment(s.getDepartment());
            if (s.getPosition() != null) staff.setStaffPosition(s.getPosition());
            if (s.getGender() != null) staff.setStaffGender(s.getGender());
            if (s.getAddress() != null) staff.setHomeAddress(s.getAddress());
            if (s.getEmergencyContact() != null) staff.setEmergencyContact(s.getEmergencyContact());
            if (s.getEmergencyPhone() != null) staff.setEmergencyPhone(s.getEmergencyPhone());
            if (s.getAvatarUrl() != null) staff.setAvatarUrl(s.getAvatarUrl());
            if (isNewStaff && staff.getHireDate() == null) staff.setHireDate(java.time.LocalDate.now());
            StaffMaster savedStaff = staffRepo.save(staff);

            s.setStatus("approved");
            s.setConvertedStaffId(savedStaff.getStaffId());
            Long staffId = UserContext.getStaffId();
            s.setReviewerId(staffId != null ? staffId.intValue() : null);
            s.setReviewerName(UserContext.getUsername());
            s.setReviewTime(LocalDateTime.now());
            submissionRepo.save(s);

            Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("staffId", savedStaff.getStaffId());
            data.put("isNewStaff", isNewStaff);
            return Result.success(data);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "审核失败: " + e.getMessage());
        }
    }

    /** POST /api/hr/self-service/reject/{id}  请求体: {note} */
    @PostMapping("/reject/{id}")
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            assertHrAccess();
            SelfServiceSubmission s = submissionRepo.findById(id).orElse(null);
            if (s == null) return Result.error(404, "提交记录不存在");
            if (!"pending".equals(s.getStatus())) return Result.error(400, "该记录已处理，不能重复审核");
            String note = asString(body.get("note"));
            if (note == null || note.isBlank()) return Result.error(400, "驳回原因不能为空");
            s.setStatus("rejected");
            s.setRejectNote(note);
            Long staffId = UserContext.getStaffId();
            s.setReviewerId(staffId != null ? staffId.intValue() : null);
            s.setReviewerName(UserContext.getUsername());
            s.setReviewTime(LocalDateTime.now());
            submissionRepo.save(s);
            return Result.success(null);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "驳回失败: " + e.getMessage());
        }
    }

    private Map<String, Object> toRow(SelfServiceSubmission s) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("id", s.getId());
        row.put("submitType", s.getSubmitType());
        row.put("jobPostingId", s.getJobPostingId());
        row.put("name", s.getName());
        row.put("phone", s.getPhone());
        row.put("idCard", s.getIdCard() != null ? aesUtil.decrypt(s.getIdCard()) : null);
        row.put("department", s.getDepartment());
        row.put("position", s.getPosition());
        row.put("gender", s.getGender());
        row.put("address", s.getAddress());
        row.put("emergencyContact", s.getEmergencyContact());
        row.put("emergencyPhone", s.getEmergencyPhone());
        row.put("avatarUrl", s.getAvatarUrl());
        row.put("remark", s.getRemark());
        row.put("status", s.getStatus());
        row.put("rejectNote", s.getRejectNote());
        row.put("createdAt", s.getCreatedAt());
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

    /** 复用 HRController 同款校验：登录 + can_manage_hr=1 才能查看/审核。总经理(数据范围=全部)不受此限制。 */
    private void assertHrAccess() {
        if (UserContext.isDataScopeAll()) return;
        Long currentStaffId = UserContext.getStaffId();
        if (currentStaffId == null) throw new SecurityException("未登录，无法访问自助登记数据");
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT can_manage_hr FROM staff_master WHERE staff_id = ? LIMIT 1", currentStaffId.intValue());
        if (rows.isEmpty()) throw new SecurityException("无权访问自助登记数据");
        int canManageHr = rows.get(0).get("can_manage_hr") == null ? 0 : ((Number) rows.get(0).get("can_manage_hr")).intValue();
        if (canManageHr != 1) throw new SecurityException("无权访问自助登记数据");
    }

    private Long resolveQueryStoreId() {
        if (UserContext.isDataScopeAll()) return null;
        Long sid = UserContext.currentStoreId();
        return (sid == null || sid == 0L) ? null : sid;
    }
}
