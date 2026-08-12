package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.Department;
import com.youjian.banquet.entity.Post;
import com.youjian.banquet.repository.DepartmentRepository;
import com.youjian.banquet.repository.PostRepository;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门与岗位管理 API。
 * <p>
 * 部门：
 * <ul>
 *   <li>GET    /api/hr/departments/tree — 部门树（带 parent_id 层级）</li>
 *   <li>POST   /api/hr/departments     — 新增部门</li>
 *   <li>PUT    /api/hr/departments/{id} — 更新部门</li>
 *   <li>DELETE /api/hr/departments/{id} — 删除部门</li>
 * </ul>
 * 岗位：
 * <ul>
 *   <li>GET    /api/hr/posts            — 按 dept_id 查询岗位列表</li>
 *   <li>POST   /api/hr/posts            — 新增岗位</li>
 *   <li>PUT    /api/hr/posts/{id}        — 更新岗位</li>
 *   <li>DELETE /api/hr/posts/{id}        — 删除岗位</li>
 * </ul>
 * 注：GET /api/hr/departments（扁平列表）已在 HRController 中实现，本类不重复定义。
 */
@RestController
@RequestMapping("/api/hr-admin/dept-post")
@CrossOrigin(origins = "*")
public class DepartmentPostController {

    @Autowired
    private DepartmentRepository deptRepo;

    @Autowired
    private PostRepository postRepo;

    // ===== 部门 =====

    /**
     * 获取部门树（带 parent_id 层级结构）。
     * 参数 storeId 可选；店长自动限定本店，总经理可查全部门店。
     */
    @GetMapping("/departments/tree")
    public Result<List<Map<String, Object>>> getDepartmentTree(
            @RequestParam(required = false) Long storeId) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            List<Department> all;
            if (effective != null) {
                all = deptRepo.findByStoreIdOrderBySortOrderAsc(effective);
            } else {
                all = deptRepo.findAll();
            }
            List<Map<String, Object>> tree = buildTree(all, null);
            return Result.success(tree);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取部门树失败: " + e.getMessage());
        }
    }

    /** 递归构建部门树 */
    private List<Map<String, Object>> buildTree(List<Department> all, Integer parentId) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Department dept : all) {
            boolean isChild = (parentId == null && dept.getParentId() == null)
                    || (parentId != null && parentId.equals(dept.getParentId()));
            if (isChild) {
                Map<String, Object> node = new HashMap<>();
                node.put("deptId", dept.getDeptId());
                node.put("storeId", dept.getStoreId());
                node.put("deptName", dept.getDeptName());
                node.put("deptCode", dept.getDeptCode());
                node.put("parentId", dept.getParentId());
                node.put("sortOrder", dept.getSortOrder());
                node.put("status", dept.getStatus());
                node.put("description", dept.getDescription());
                node.put("level", dept.getLevel());
                node.put("children", buildTree(all, dept.getDeptId()));
                nodes.add(node);
            }
        }
        return nodes;
    }

    /** 新增部门 */
    @PostMapping("/departments")
    public Result<Department> createDepartment(@RequestBody Department dept) {
        try {
            Long userStore = resolveWriteStoreId();
            if (userStore != null) {
                dept.setStoreId(userStore);
            } else if (dept.getStoreId() == null) {
                dept.setStoreId(1L);
            }
            if (dept.getStatus() == null) {
                dept.setStatus("active");
            }
            return Result.success(deptRepo.save(dept));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "新增部门失败: " + e.getMessage());
        }
    }

    /** 更新部门 */
    @PutMapping("/departments/{id}")
    public Result<Department> updateDepartment(@PathVariable Integer id, @RequestBody Department dept) {
        try {
            Department existing = deptRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "部门不存在");
            // 门店校验
            Long userStore = resolveWriteStoreId();
            if (userStore != null && (existing.getStoreId() == null || !userStore.equals(existing.getStoreId()))) {
                return Result.error(403, "无权编辑非本店部门");
            }
            if (dept.getDeptName() != null) existing.setDeptName(dept.getDeptName());
            if (dept.getDeptCode() != null) existing.setDeptCode(dept.getDeptCode());
            if (dept.getParentId() != null) existing.setParentId(dept.getParentId());
            if (dept.getSortOrder() != null) existing.setSortOrder(dept.getSortOrder());
            if (dept.getStatus() != null) existing.setStatus(dept.getStatus());
            if (dept.getDescription() != null) existing.setDescription(dept.getDescription());
            if (dept.getLevel() != null) existing.setLevel(dept.getLevel());
            // 店长不可跨店调动部门
            if (userStore == null && dept.getStoreId() != null) {
                existing.setStoreId(dept.getStoreId());
            }
            return Result.success(deptRepo.save(existing));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "更新部门失败: " + e.getMessage());
        }
    }

    /** 删除部门 */
    @DeleteMapping("/departments/{id}")
    public Result<?> deleteDepartment(@PathVariable Integer id) {
        try {
            Department existing = deptRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "部门不存在");
            Long userStore = resolveWriteStoreId();
            if (userStore != null && (existing.getStoreId() == null || !userStore.equals(existing.getStoreId()))) {
                return Result.error(403, "无权删除非本店部门");
            }
            deptRepo.delete(existing);
            return Result.success("部门已删除");
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "删除部门失败: " + e.getMessage());
        }
    }

    // ===== 岗位 =====

    /**
     * 按部门查询岗位列表。
     * 参数 deptId 必填。
     */
    @GetMapping("/posts")
    public Result<List<Post>> getPosts(@RequestParam Integer deptId) {
        try {
            return Result.success(postRepo.findByDeptIdOrderBySortOrderAsc(deptId));
        } catch (Exception e) {
            return Result.error(500, "获取岗位列表失败: " + e.getMessage());
        }
    }

    /** 新增岗位 */
    @PostMapping("/posts")
    public Result<Post> createPost(@RequestBody Post post) {
        try {
            return Result.success(postRepo.save(post));
        } catch (Exception e) {
            return Result.error(500, "新增岗位失败: " + e.getMessage());
        }
    }

    /** 更新岗位 */
    @PutMapping("/posts/{id}")
    public Result<Post> updatePost(@PathVariable Integer id, @RequestBody Post post) {
        try {
            Post existing = postRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "岗位不存在");
            if (post.getDeptId() != null) existing.setDeptId(post.getDeptId());
            if (post.getPostName() != null) existing.setPostName(post.getPostName());
            if (post.getPostCode() != null) existing.setPostCode(post.getPostCode());
            if (post.getHeadcount() != null) existing.setHeadcount(post.getHeadcount());
            if (post.getOnDutyCount() != null) existing.setOnDutyCount(post.getOnDutyCount());
            if (post.getSortOrder() != null) existing.setSortOrder(post.getSortOrder());
            if (post.getRemark() != null) existing.setRemark(post.getRemark());
            return Result.success(postRepo.save(existing));
        } catch (Exception e) {
            return Result.error(500, "更新岗位失败: " + e.getMessage());
        }
    }

    /** 删除岗位 */
    @DeleteMapping("/posts/{id}")
    public Result<?> deletePost(@PathVariable Integer id) {
        try {
            Post existing = postRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "岗位不存在");
            postRepo.delete(existing);
            return Result.success("岗位已删除");
        } catch (Exception e) {
            return Result.error(500, "删除岗位失败: " + e.getMessage());
        }
    }

    // ======== 门店数据隔离辅助方法 ========

    /**
     * 查询接口：解析有效门店ID。
     * @return null=全局（总经理）；非null=限制到指定门店（店长）
     */
    private Long resolveQueryStoreId(Long requestStoreId) {
        if (UserContext.isGeneralManager()) {
            return requestStoreId; // 总经理可指定门店，不传则查全部
        }
        Long current = UserContext.currentStoreId();
        if (current == null || current == 0L) {
            throw new SecurityException("未登录，无权访问部门数据");
        }
        return current;
    }

    /**
     * 写操作：解析当前用户门店ID。
     * @return 店长门店ID；null=总经理（允许跨门店操作）
     */
    private Long resolveWriteStoreId() {
        if (UserContext.isGeneralManager()) {
            return null;
        }
        Long current = UserContext.currentStoreId();
        if (current == null || current == 0L) {
            throw new SecurityException("未登录，无权操作部门数据");
        }
        return current;
    }
}
