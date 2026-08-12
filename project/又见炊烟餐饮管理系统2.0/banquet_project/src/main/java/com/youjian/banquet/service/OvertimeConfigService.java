package com.youjian.banquet.service;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.OvertimeConfig;
import com.youjian.banquet.repository.OvertimeConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OvertimeConfigService {

    @Autowired
    private OvertimeConfigRepository overtimeConfigRepository;

    public Result<OvertimeConfig> add(OvertimeConfig overtimeConfig) {
        OvertimeConfig saved = overtimeConfigRepository.save(overtimeConfig);
        return Result.success(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteById(Integer id) {
        Optional<OvertimeConfig> opt = overtimeConfigRepository.findById(id);
        if (opt.isPresent()) {
            OvertimeConfig entity = opt.get();
            entity.setIsDeleted(1);
            overtimeConfigRepository.save(entity);
            return Result.success();
        }
        return Result.error(500, "删除失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteBatch(List<Integer> ids) {
        List<OvertimeConfig> entities = overtimeConfigRepository.findAllById(ids);
        for (OvertimeConfig entity : entities) {
            entity.setIsDeleted(1);
        }
        overtimeConfigRepository.saveAll(entities);
        return Result.success();
    }

    public Result<OvertimeConfig> edit(OvertimeConfig overtimeConfig) {
        if (overtimeConfig.getId() == null) {
            return Result.error(500, "ID不能为空");
        }
        OvertimeConfig updated = overtimeConfigRepository.save(overtimeConfig);
        return Result.success(updated);
    }

    public Result<OvertimeConfig> findById(Integer id) {
        Optional<OvertimeConfig> opt = overtimeConfigRepository.findById(id);
        return opt.map(Result::success).orElse(Result.error(500, "未找到记录"));
    }

    public Result<Page<OvertimeConfig>> list(Integer current, Integer size, Long storeId) {
        PageRequest pageRequest = PageRequest.of(current - 1, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<OvertimeConfig> page = overtimeConfigRepository.findAll(
                (root, query, cb) -> cb.and(
                        cb.equal(root.get("storeId"), storeId),
                        cb.equal(root.get("isDeleted"), 0)
                ), pageRequest);
        return Result.success(page);
    }

    public Result<List<OvertimeConfig>> listAll(Long storeId) {
        List<OvertimeConfig> list = overtimeConfigRepository.findByStoreIdAndIsDeletedOrderByCreateTimeDesc(storeId, 0);
        return Result.success(list);
    }

    public Result<List<OvertimeConfig>> findByDeptId(Long storeId, Integer deptId) {
        List<OvertimeConfig> list = overtimeConfigRepository.findByStoreIdAndDeptIdAndIsDeleted(storeId, deptId, 0);
        return Result.success(list);
    }
}