package com.youjian.banquet.service;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrRole;
import com.youjian.banquet.repository.HrRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.*;

/**
 * HR角色服务
 * 完整保留原系统 RoleService 的RBAC权限逻辑
 * 原框架：MyBatis Plus → 现框架：JPA
 */
@Service
public class HrRoleService {

    @Autowired
    private HrRoleRepository roleRepository;

    /**
     * 新增角色
     * 对应原系统 RoleService.add(Role)
     */
    @Transactional
    public Result<Void> add(HrRole role) {
        roleRepository.save(role);
        return Result.success();
    }

    /**
     * 逻辑删除角色
     * 对应原系统 RoleService.deleteById(Integer)
     */
    @Transactional
    public Result<Void> deleteById(Integer id) {
        if (roleRepository.existsById(id)) {
            roleRepository.deleteById(id);
            return Result.success();
        }
        return Result.error(404, "角色不存在");
    }

    /**
     * 批量逻辑删除
     * 对应原系统 RoleService.deleteBatch(List<Integer>)
     */
    @Transactional
    public Result<Void> deleteBatch(List<Integer> ids) {
        List<HrRole> roles = roleRepository.findAllById(ids);
        roleRepository.deleteAll(roles);
        return Result.success();
    }

    /**
     * 编辑更新角色
     * 对应原系统 RoleService.edit(Role)
     */
    @Transactional
    public Result<Void> edit(HrRole role) {
        if (role.getId() == null || !roleRepository.existsById(role.getId())) {
            return Result.error(404, "角色不存在");
        }
        roleRepository.save(role);
        return Result.success();
    }

    /**
     * 根据ID查询角色
     * 对应原系统 RoleService.findById(Integer)
     */
    public Result<HrRole> findById(Integer id) {
        Optional<HrRole> role = roleRepository.findById(id);
        return role.map(Result::success)
                .orElse(Result.error(404, "角色不存在"));
    }

    /**
     * 查询所有角色
     * 对应原系统 RoleService.findAll()
     */
    public Result<List<HrRole>> findAll() {
        List<HrRole> list = roleRepository.findAll();
        return Result.success(list);
    }

    /**
     * 分页条件查询角色
     * 对应原系统 RoleService.list(Integer, Integer, String)
     * 支持多租户store_id过滤和名称模糊搜索
     */
    public Result<Map<String, Object>> list(Integer current, Integer size, String name, Long storeId) {
        PageRequest pageRequest = PageRequest.of(current - 1, size, Sort.by(Sort.Direction.ASC, "id"));

        Specification<HrRole> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 多租户：门店过滤
            if (storeId != null) {
                predicates.add(cb.equal(root.get("storeId"), storeId));
            }
            // 名称模糊搜索
            if (name != null && !name.isEmpty()) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<HrRole> page = roleRepository.findAll(spec, pageRequest);

        Map<String, Object> map = new HashMap<>();
        map.put("pages", page.getTotalPages());
        map.put("total", page.getTotalElements());
        map.put("list", page.getContent());
        return Result.success(map);
    }

    /**
     * 按门店ID查询所有角色
     */
    public Result<List<HrRole>> findByStoreId(Long storeId) {
        List<HrRole> list = roleRepository.findByStoreId(storeId);
        return Result.success(list);
    }
}