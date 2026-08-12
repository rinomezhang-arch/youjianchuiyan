package com.youjian.banquet.service;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.PerRoleMenu;
import com.youjian.banquet.repository.PerRoleMenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PerRoleMenuService {

    @Autowired
    private PerRoleMenuRepository perRoleMenuRepository;

    public Result<PerRoleMenu> add(PerRoleMenu perRoleMenu) {
        PerRoleMenu saved = perRoleMenuRepository.save(perRoleMenu);
        return Result.success(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteById(Integer id) {
        Optional<PerRoleMenu> opt = perRoleMenuRepository.findById(id);
        if (opt.isPresent()) {
            PerRoleMenu entity = opt.get();
            entity.setIsDeleted(1);
            perRoleMenuRepository.save(entity);
            return Result.success();
        }
        return Result.error(500, "删除失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteBatch(List<Integer> ids) {
        List<PerRoleMenu> entities = perRoleMenuRepository.findAllById(ids);
        for (PerRoleMenu entity : entities) {
            entity.setIsDeleted(1);
        }
        perRoleMenuRepository.saveAll(entities);
        return Result.success();
    }

    public Result<PerRoleMenu> edit(PerRoleMenu perRoleMenu) {
        if (perRoleMenu.getId() == null) {
            return Result.error(500, "ID不能为空");
        }
        PerRoleMenu updated = perRoleMenuRepository.save(perRoleMenu);
        return Result.success(updated);
    }

    public Result<PerRoleMenu> findById(Integer id) {
        Optional<PerRoleMenu> opt = perRoleMenuRepository.findById(id);
        return opt.map(Result::success).orElse(Result.error(500, "未找到记录"));
    }

    public Result<Page<PerRoleMenu>> list(Integer current, Integer size, Long storeId) {
        PageRequest pageRequest = PageRequest.of(current - 1, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<PerRoleMenu> page = perRoleMenuRepository.findAll(
                (root, query, cb) -> cb.and(
                        cb.equal(root.get("storeId"), storeId),
                        cb.equal(root.get("isDeleted"), 0)
                ), pageRequest);
        return Result.success(page);
    }

    public Result<List<PerRoleMenu>> listAll(Long storeId) {
        List<PerRoleMenu> list = perRoleMenuRepository.findByStoreIdAndIsDeletedOrderByCreateTimeDesc(storeId, 0);
        return Result.success(list);
    }

    public Result<List<PerRoleMenu>> findByRoleId(Long storeId, Integer roleId) {
        List<PerRoleMenu> list = perRoleMenuRepository.findByStoreIdAndRoleIdAndIsDeleted(storeId, roleId, 0);
        return Result.success(list);
    }

    public Result<List<PerRoleMenu>> findByMenuId(Long storeId, Integer menuId) {
        List<PerRoleMenu> list = perRoleMenuRepository.findByStoreIdAndMenuIdAndIsDeleted(storeId, menuId, 0);
        return Result.success(list);
    }
}