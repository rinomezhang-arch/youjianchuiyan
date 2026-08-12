package com.youjian.banquet.service;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.PerStaffRole;
import com.youjian.banquet.repository.PerStaffRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PerStaffRoleService {

    @Autowired
    private PerStaffRoleRepository perStaffRoleRepository;

    public Result<PerStaffRole> add(PerStaffRole perStaffRole) {
        PerStaffRole saved = perStaffRoleRepository.save(perStaffRole);
        return Result.success(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteById(Integer id) {
        Optional<PerStaffRole> opt = perStaffRoleRepository.findById(id);
        if (opt.isPresent()) {
            PerStaffRole entity = opt.get();
            entity.setIsDeleted(1);
            perStaffRoleRepository.save(entity);
            return Result.success();
        }
        return Result.error(500, "删除失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteBatch(List<Integer> ids) {
        List<PerStaffRole> entities = perStaffRoleRepository.findAllById(ids);
        for (PerStaffRole entity : entities) {
            entity.setIsDeleted(1);
        }
        perStaffRoleRepository.saveAll(entities);
        return Result.success();
    }

    public Result<PerStaffRole> edit(PerStaffRole perStaffRole) {
        if (perStaffRole.getId() == null) {
            return Result.error(500, "ID不能为空");
        }
        PerStaffRole updated = perStaffRoleRepository.save(perStaffRole);
        return Result.success(updated);
    }

    public Result<PerStaffRole> findById(Integer id) {
        Optional<PerStaffRole> opt = perStaffRoleRepository.findById(id);
        return opt.map(Result::success).orElse(Result.error(500, "未找到记录"));
    }

    public Result<Page<PerStaffRole>> list(Integer current, Integer size, Long storeId) {
        PageRequest pageRequest = PageRequest.of(current - 1, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<PerStaffRole> page = perStaffRoleRepository.findAll(
                (root, query, cb) -> cb.and(
                        cb.equal(root.get("storeId"), storeId),
                        cb.equal(root.get("isDeleted"), 0)
                ), pageRequest);
        return Result.success(page);
    }

    public Result<List<PerStaffRole>> listAll(Long storeId) {
        List<PerStaffRole> list = perStaffRoleRepository.findByStoreIdAndIsDeletedOrderByCreateTimeDesc(storeId, 0);
        return Result.success(list);
    }

    public Result<List<PerStaffRole>> findByStaffId(Long storeId, Integer staffId) {
        List<PerStaffRole> list = perStaffRoleRepository.findByStoreIdAndStaffIdAndIsDeleted(storeId, staffId, 0);
        return Result.success(list);
    }

    public Result<List<PerStaffRole>> findByRoleId(Long storeId, Integer roleId) {
        List<PerStaffRole> list = perStaffRoleRepository.findByStoreIdAndRoleIdAndIsDeleted(storeId, roleId, 0);
        return Result.success(list);
    }
}