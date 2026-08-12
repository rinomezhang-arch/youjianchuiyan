package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BtDishInfo;
import com.youjian.banquet.entity.BtOrder;
import com.youjian.banquet.repository.BtDishInfoRepository;
import com.youjian.banquet.repository.BtOrderRepository;
import com.youjian.banquet.service.BtDishInfoService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 菜品信息控制器
 * 来源：点餐系统 CaipinxinxiController
 */
@RestController
@RequestMapping("/api/bt/dish-info")
@CrossOrigin(origins = "*")
public class BtDishInfoController {

    @Autowired
    private BtDishInfoService btDishInfoService;

    @Autowired
    private BtDishInfoRepository btDishInfoRepo;

    @Autowired
    private BtOrderRepository btOrderRepo;

    private Specification<BtDishInfo> hasStoreId(Long storeId) {
        return (Root<BtDishInfo> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    /**
     * 分页列表（后端管理）
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "id") String sortField,
                                            @RequestParam(defaultValue = "desc") String sortOrder,
                                            @RequestParam(required = false) String caipinmingcheng,
                                            @RequestParam(required = false) String caipinleixing,
                                            @RequestParam(required = false) Double pricestart,
                                            @RequestParam(required = false) Double priceend,
                                            @RequestParam(required = false) Long storeId) {
        Page<BtDishInfo> pageResult;
        if (caipinmingcheng != null || caipinleixing != null || pricestart != null || priceend != null) {
            pageResult = btDishInfoService.searchWithPrice(caipinmingcheng, caipinleixing, pricestart, priceend, page, size);
        } else {
            pageResult = btDishInfoService.page(page, size, sortField, sortOrder, storeId);
        }
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    /**
     * 智能排序（按点击时间降序）
     */
    @GetMapping("/autoSort")
    public Result<Map<String, Object>> autoSort(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("clicktime").descending());
        Page<BtDishInfo> pageResult = btDishInfoRepo.findAll(hasStoreId(storeId), pageable);
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    /**
     * 协同推荐排序（按用户购买的菜品类型推荐）
     */
    @GetMapping("/autoSort2")
    public Result<Map<String, Object>> autoSort2(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) Long userid,
                                                 @RequestParam(required = false) Integer limit,
                                                 @RequestParam(required = false) Long storeId) {
        int pageLimit = limit != null ? limit : 10;
        List<BtDishInfo> resultList = new ArrayList<>();
        if (userid != null) {
            List<BtOrder> userOrders = btOrderRepo.findByUseridAndTablenameOrderByAddtimeDesc(userid, "caipinxinxi");
            Set<String> goodtypes = new LinkedHashSet<>();
            for (BtOrder o : userOrders) {
                if (o.getGoodtype() != null) goodtypes.add(o.getGoodtype());
            }
            if (!goodtypes.isEmpty()) {
                for (String gt : goodtypes) {
                    resultList.addAll(btDishInfoRepo.findByCaipinleixingList(gt, PageRequest.of(0, pageLimit)));
                    if (resultList.size() >= pageLimit) break;
                }
            }
        }
        if (resultList.size() < pageLimit) {
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
            Page<BtDishInfo> pageResult = btDishInfoRepo.findAll(hasStoreId(storeId), pageable);
            for (BtDishInfo d : pageResult.getContent()) {
                boolean exists = resultList.stream().anyMatch(r -> r.getId().equals(d.getId()));
                if (!exists) {
                    resultList.add(d);
                    if (resultList.size() >= pageLimit) break;
                }
            }
        }
        return Result.success(Map.of(
                "list", resultList,
                "total", (long) resultList.size(),
                "page", page,
                "size", size
        ));
    }

    /**
     * 前端列表（忽略鉴权）
     */
    @GetMapping("/list")
    public Result<List<BtDishInfo>> list(@RequestParam(required = false) Long storeId) {
        return Result.success(btDishInfoService.listAll(storeId));
    }

    /**
     * 详情（后端管理，点击时更新点击时间）
     */
    @GetMapping("/info/{id}")
    public Result<BtDishInfo> info(@PathVariable Long id) {
        return btDishInfoService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "菜品信息不存在"));
    }

    /**
     * 前端详情（忽略鉴权，点击时更新点击时间）
     */
    @GetMapping("/detail/{id}")
    public Result<BtDishInfo> detail(@PathVariable Long id) {
        return btDishInfoService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "菜品信息不存在"));
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<BtDishInfo> save(@RequestBody BtDishInfo entity) {
        return Result.success(btDishInfoService.save(entity));
    }

    /**
     * 前端添加
     */
    @PostMapping("/add")
    public Result<BtDishInfo> add(@RequestBody BtDishInfo entity) {
        return Result.success(btDishInfoService.save(entity));
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result<BtDishInfo> update(@RequestBody BtDishInfo entity) {
        return Result.success(btDishInfoService.update(entity));
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        btDishInfoService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    /**
     * 提醒接口
     */
    @GetMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable String columnName,
                                                   @PathVariable String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = btDishInfoService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}