package com.youjian.banquet.service;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.dto.PageResult;
import com.youjian.banquet.entity.HrMenu;
import com.youjian.banquet.entity.HrRoleMenu;
import com.youjian.banquet.entity.HrStaffRole;
import com.youjian.banquet.repository.HrMenuRepository;
import com.youjian.banquet.repository.HrRoleMenuRepository;
import com.youjian.banquet.repository.HrStaffRoleRepository;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HR菜单服务
 * 完整保留原系统 MenuService 的RBAC权限逻辑（菜单树构建、员工菜单获取）
 * 原框架：MyBatis Plus → 现框架：JPA
 */
@Service
public class HrMenuService {

    @Autowired
    private HrMenuRepository menuRepository;

    @Autowired
    private HrRoleMenuRepository roleMenuRepository;

    @Autowired
    private HrStaffRoleRepository staffRoleRepository;

    /**
     * 新增菜单
     * 对应原系统 MenuService.add(Menu)
     */
    @Transactional
    public Result<Void> add(HrMenu menu) {
        menuRepository.save(menu);
        return Result.success();
    }

    /**
     * 逻辑删除菜单
     * 对应原系统 MenuService.deleteById(Integer)
     */
    @Transactional
    public Result<Void> deleteById(Integer id) {
        if (menuRepository.existsById(id)) {
            menuRepository.deleteById(id);
            return Result.success();
        }
        return Result.error(404, "菜单不存在");
    }

    /**
     * 批量逻辑删除
     * 对应原系统 MenuService.deleteBatch(List<Integer>)
     */
    @Transactional
    public Result<Void> deleteBatch(List<Integer> ids) {
        List<HrMenu> menus = menuRepository.findAllById(ids);
        menuRepository.deleteAll(menus);
        return Result.success();
    }

    /**
     * 编辑更新菜单
     * 对应原系统 MenuService.edit(Menu)
     */
    @Transactional
    public Result<Void> edit(HrMenu menu) {
        if (menu.getId() == null || !menuRepository.existsById(menu.getId())) {
            return Result.error(404, "菜单不存在");
        }
        menuRepository.save(menu);
        return Result.success();
    }

    /**
     * 根据ID查询菜单
     * 对应原系统 MenuService.findById(Integer)
     */
    public Result<HrMenu> findById(Integer id) {
        Optional<HrMenu> menu = menuRepository.findById(id);
        return menu.map(Result::success)
                .orElse(Result.error(404, "菜单不存在"));
    }

    /**
     * 查找所有菜单（构建树形结构）
     * 对应原系统 MenuService.findAll()
     */
    public Result<List<HrMenu>> findAll() {
        List<HrMenu> list = menuRepository.findAll();
        return Result.success(setSubMenu(list));
    }

    /**
     * 分页条件查询（仅查询顶级菜单，子菜单通过children返回）
     * 对应原系统 MenuService.list(Integer, Integer, String)
     * 支持多租户store_id过滤
     */
    public Result<Map<String, Object>> list(Integer current, Integer size, String name, Long storeId) {
        PageRequest pageRequest = PageRequest.of(current - 1, size, Sort.by(Sort.Direction.ASC, "id"));

        Specification<HrMenu> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 仅查询顶级菜单(parent_id = 0)
            predicates.add(cb.equal(root.get("parentId"), 0));
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

        Page<HrMenu> page = menuRepository.findAll(spec, pageRequest);
        // 查询所有菜单，为父级菜单设置子菜单
        List<HrMenu> allMenus = menuRepository.findAll();
        List<HrMenu> parentList = page.getContent();
        for (HrMenu parentMenu : parentList) {
            List<HrMenu> subList = allMenus.stream()
                    .filter(menu -> menu.getParentId().equals(parentMenu.getId()))
                    .collect(Collectors.toList());
            parentMenu.setChildren(subList);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("pages", page.getTotalPages());
        map.put("total", page.getTotalElements());
        map.put("list", page.getContent());
        return Result.success(map);
    }

    /**
     * 为父级菜单设置子菜单（菜单树构建核心逻辑）
     * 完整保留原系统 MenuService.setSubMenu(List<Menu>) 逻辑
     * 使用流处理数据，返回父级菜单
     */
    public List<HrMenu> setSubMenu(List<HrMenu> list) {
        // 父级菜单 (parentId == 0)
        List<HrMenu> parentList = list.stream().parallel()
                .filter(menu -> menu.getParentId() == 0)
                .collect(Collectors.toList());
        for (HrMenu parentMenu : parentList) {
            // 子菜单
            List<HrMenu> subList = list.stream().parallel()
                    .filter(menu -> menu.getParentId().equals(parentMenu.getId()))
                    .collect(Collectors.toList());
            parentMenu.setChildren(subList);
        }
        return parentList;
    }

    /**
     * 获取当前登录员工的菜单（从JWT token中解析员工ID）
     * 完整保留原系统 MenuService.getStaffMenu(HttpServletRequest) 逻辑
     * 通过 staff_id → staff_role → role_menu → menu 链路获取菜单
     */
    public Result<List<HrMenu>> getStaffMenu() {
        Long staffId = UserContext.getStaffId();
        if (staffId == null) {
            return Result.error(401, "未登录或token已过期");
        }
        return getStaffMenuByStaffId(staffId.intValue());
    }

    /**
     * 通过员工ID查询菜单（流式处理优化版）
     * 完整保留原系统 MenuService.getStaffMenuPlus(Integer) 逻辑
     * 减少对数据库的查询次数，使用流处理数据
     */
    public Result<List<HrMenu>> getStaffMenuByStaffId(Integer staffId) {
        Set<HrMenu> menuSet = new HashSet<>();

        // 查询员工启用的角色
        List<HrStaffRole> staffRoleList = staffRoleRepository.findAll().stream()
                .filter(sr -> sr.getStaffId().equals(staffId) && sr.getStatus() == 1)
                .collect(Collectors.toList());

        if (staffRoleList.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 查询所有启用的角色菜单关系
        List<HrRoleMenu> roleMenuList = roleMenuRepository.findAll().stream()
                .filter(rm -> rm.getStatus() == 1)
                .collect(Collectors.toList());

        // 查询所有菜单
        List<HrMenu> allMenus = menuRepository.findAll();

        // 流式处理：遍历员工角色，匹配角色菜单，匹配菜单
        for (HrStaffRole staffRole : staffRoleList) {
            List<HrRoleMenu> roleMenus = roleMenuList.stream().parallel()
                    .filter(rm -> rm.getRoleId().equals(staffRole.getRoleId()))
                    .collect(Collectors.toList());
            for (HrRoleMenu roleMenu : roleMenus) {
                List<HrMenu> menus = allMenus.stream().parallel()
                        .filter(menu -> menu.getId().equals(roleMenu.getMenuId()))
                        .collect(Collectors.toList());
                menuSet.addAll(menus);
            }
        }

        // 去重并构建菜单树
        List<HrMenu> staffMenus = menuSet.stream().parallel()
                .distinct()
                .collect(Collectors.toList());
        return Result.success(setSubMenu(staffMenus));
    }

    /**
     * 通过员工ID和门店ID查询菜单（多租户版）
     * 先按storeId过滤，再按staff_id → role → menu链路获取
     */
    public Result<List<HrMenu>> getStaffMenuByStaffIdAndStoreId(Integer staffId, Long storeId) {
        Set<HrMenu> menuSet = new HashSet<>();

        // 查询该门店下员工启用的角色
        List<HrStaffRole> staffRoleList = staffRoleRepository.findByStoreIdAndStaffIdAndStatus(storeId, staffId, 1);

        if (staffRoleList.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 查询该门店下所有启用的角色菜单关系
        List<HrRoleMenu> roleMenuList = roleMenuRepository.findByStoreIdAndStatus(storeId, 1);

        // 查询该门店下所有菜单
        List<HrMenu> allMenus = menuRepository.findByStoreId(storeId);

        // 流式处理
        for (HrStaffRole staffRole : staffRoleList) {
            List<HrRoleMenu> roleMenus = roleMenuList.stream().parallel()
                    .filter(rm -> rm.getRoleId().equals(staffRole.getRoleId()))
                    .collect(Collectors.toList());
            for (HrRoleMenu roleMenu : roleMenus) {
                List<HrMenu> menus = allMenus.stream().parallel()
                        .filter(menu -> menu.getId().equals(roleMenu.getMenuId()))
                        .collect(Collectors.toList());
                menuSet.addAll(menus);
            }
        }

        List<HrMenu> staffMenus = menuSet.stream().parallel()
                .distinct()
                .collect(Collectors.toList());
        return Result.success(setSubMenu(staffMenus));
    }
}