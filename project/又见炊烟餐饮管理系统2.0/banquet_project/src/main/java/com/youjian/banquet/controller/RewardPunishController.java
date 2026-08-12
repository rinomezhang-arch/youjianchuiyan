package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.RewardPunish;
import com.youjian.banquet.service.RewardPunishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 奖惩接口
 * 对应规划手册 5.txt 4.3 - 奖惩模块
 */
@RestController
@RequestMapping("/api/hr-admin/reward-punish")
@CrossOrigin(origins = "*")
public class RewardPunishController {

    @Autowired private RewardPunishService rpService;

    /** 奖惩列表 */
    @GetMapping
    public Result<List<RewardPunish>> list(@RequestParam(defaultValue = "1") Long storeId,
                                           @RequestParam(required = false) Integer finalStatus) {
        try {
            return Result.success(rpService.list(storeId, finalStatus));
        } catch (Exception e) {
            return Result.error(500, "获取奖惩列表失败: " + e.getMessage());
        }
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<RewardPunish> get(@PathVariable Long id) {
        try {
            RewardPunish rp = rpService.get(id);
            if (rp == null) return Result.error(404, "奖惩单不存在");
            return Result.success(rp);
        } catch (Exception e) {
            return Result.error(500, "获取奖惩详情失败: " + e.getMessage());
        }
    }

    /** 新增奖惩 */
    @PostMapping
    public Result<RewardPunish> create(@RequestBody RewardPunish rp) {
        try {
            return Result.success(rpService.create(rp));
        } catch (Exception e) {
            return Result.error(500, "新增奖惩失败: " + e.getMessage());
        }
    }

    /**
     * 两级审批
     * @param level 1-店长级 2-总经理级
     * @param status 2-通过 3-驳回
     */
    @PutMapping("/{id}/approve")
    public Result<RewardPunish> approve(@PathVariable Long id,
                                        @RequestParam Long approverId,
                                        @RequestParam int level,
                                        @RequestParam int status,
                                        @RequestParam(required = false) String remark) {
        try {
            return Result.success(rpService.approve(id, level, approverId, status, remark));
        } catch (Exception e) {
            return Result.error(500, "审批失败: " + e.getMessage());
        }
    }
}
