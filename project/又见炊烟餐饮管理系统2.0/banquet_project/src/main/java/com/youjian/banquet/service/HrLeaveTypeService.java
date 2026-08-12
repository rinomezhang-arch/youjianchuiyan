package com.youjian.banquet.service;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrLeaveType;
import com.youjian.banquet.repository.HrLeaveTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 请假类型配置 Service
 * 对应参考系统: LeaveService
 */
@Service
public class HrLeaveTypeService {

    @Autowired
    private HrLeaveTypeRepository hrLeaveTypeRepository;

    /**
     * 请假类型枚举
     */
    private static final List<Map<String, Object>> LEAVE_ENUMS = new ArrayList<>();

    static {
        LEAVE_ENUMS.add(createEnumMap(0, "事假"));
        LEAVE_ENUMS.add(createEnumMap(1, "产假"));
        LEAVE_ENUMS.add(createEnumMap(2, "病假"));
        LEAVE_ENUMS.add(createEnumMap(3, "婚假"));
        LEAVE_ENUMS.add(createEnumMap(4, "探亲假"));
        LEAVE_ENUMS.add(createEnumMap(5, "陪产假"));
    }

    private static Map<String, Object> createEnumMap(Integer code, String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("message", message);
        return map;
    }

    public Result<HrLeaveType> add(HrLeaveType leaveType) {
        HrLeaveType saved = hrLeaveTypeRepository.save(leaveType);
        if (saved != null) {
            return Result.success(saved);
        }
        return Result.error(500, "新增失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteById(Integer id) {
        Optional<HrLeaveType> opt = hrLeaveTypeRepository.findById(id);
        if (opt.isPresent()) {
            HrLeaveType entity = opt.get();
            entity.setIsDeleted(1);
            hrLeaveTypeRepository.save(entity);
            return Result.success();
        }
        return Result.error(500, "删除失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteBatch(List<Integer> ids) {
        List<HrLeaveType> entities = hrLeaveTypeRepository.findAllById(ids);
        for (HrLeaveType entity : entities) {
            entity.setIsDeleted(1);
        }
        hrLeaveTypeRepository.saveAll(entities);
        return Result.success();
    }

    public Result<HrLeaveType> edit(HrLeaveType leaveType) {
        if (leaveType.getId() == null) {
            return Result.error(500, "ID不能为空");
        }
        HrLeaveType updated = hrLeaveTypeRepository.save(leaveType);
        if (updated != null) {
            return Result.success(updated);
        }
        return Result.error(500, "更新失败");
    }

    public Result<HrLeaveType> findById(Integer id) {
        Optional<HrLeaveType> opt = hrLeaveTypeRepository.findById(id);
        if (opt.isPresent()) {
            return Result.success(opt.get());
        }
        return Result.error(500, "未找到记录");
    }

    /**
     * 根据部门ID和类型查询请假配置
     * 对应参考系统: find(Integer deptId, Integer typeNum)
     */
    public Result<HrLeaveType> find(Long storeId, Integer deptId, Integer typeNum) {
        Optional<HrLeaveType> opt = hrLeaveTypeRepository
                .findByStoreIdAndDeptIdAndTypeNumAndIsDeleted(storeId, deptId, typeNum, 0);
        if (opt.isPresent()) {
            return Result.success(opt.get());
        }
        return Result.error(500, "未找到记录");
    }

    /**
     * 设置假期（不存在则新增，存在则更新）
     * 对应参考系统: setLeave(Leave leave)
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> setLeave(HrLeaveType leaveType) {
        Optional<HrLeaveType> opt = hrLeaveTypeRepository
                .findByStoreIdAndDeptIdAndTypeNumAndIsDeleted(
                        leaveType.getStoreId(), leaveType.getDeptId(), leaveType.getTypeNum(), 0);
        if (opt.isPresent()) {
            HrLeaveType existing = opt.get();
            existing.setDays(leaveType.getDays());
            existing.setStatus(leaveType.getStatus());
            existing.setRemark(leaveType.getRemark());
            hrLeaveTypeRepository.save(existing);
        } else {
            hrLeaveTypeRepository.save(leaveType);
        }
        return Result.success();
    }

    /**
     * 根据部门ID查询所有请假类型配置
     * 对应参考系统: findByDeptId(Integer id)
     */
    public Result<List<HrLeaveType>> findByDeptId(Long storeId, Integer deptId) {
        List<HrLeaveType> list = hrLeaveTypeRepository.findByStoreIdAndDeptIdAndIsDeleted(storeId, deptId, 0);
        return Result.success(list);
    }

    /**
     * 获取所有请假类型枚举
     * 对应参考系统: findAll() → EnumUtil.getEnumList(LeaveEnum.class)
     */
    public Result<List<Map<String, Object>>> findAll() {
        return Result.success(LEAVE_ENUMS);
    }

    /**
     * 获取所有请假类型配置列表
     */
    public Result<List<HrLeaveType>> listAll(Long storeId) {
        List<HrLeaveType> list = hrLeaveTypeRepository.findByStoreIdAndIsDeletedOrderByCreateTimeDesc(storeId, 0);
        return Result.success(list);
    }
}