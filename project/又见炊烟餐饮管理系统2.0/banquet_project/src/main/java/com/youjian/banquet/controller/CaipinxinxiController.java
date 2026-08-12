package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BtDishInfo;
import com.youjian.banquet.entity.BtOrder;
import com.youjian.banquet.repository.BtOrderRepository;
import com.youjian.banquet.service.BtDishInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/caipinxinxi")
@CrossOrigin(origins = "*")
public class CaipinxinxiController {

    @Autowired
    private BtDishInfoService btDishInfoService;

    @Autowired
    private BtOrderRepository btOrderRepo;

    @RequestMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam Map<String, Object> params,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int limit,
                                            @RequestParam(defaultValue = "id") String sort,
                                            @RequestParam(defaultValue = "desc") String order,
                                            @RequestParam(required = false) String caipinmingcheng,
                                            @RequestParam(required = false) String caipinleixing,
                                            @RequestParam(required = false) Double pricestart,
                                            @RequestParam(required = false) Double priceend,
                                            @RequestParam(required = false) Long storeId) {
        Page<BtDishInfo> pageResult;
        if (caipinmingcheng != null || caipinleixing != null || pricestart != null || priceend != null) {
            pageResult = btDishInfoService.searchWithPrice(caipinmingcheng, caipinleixing, pricestart, priceend, page, limit);
        } else {
            pageResult = btDishInfoService.page(page, limit, sort, order, storeId);
        }
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "limit", limit
        ));
    }

    @RequestMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam Map<String, Object> params,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int limit,
                                            @RequestParam(required = false) Double pricestart,
                                            @RequestParam(required = false) Double priceend,
                                            @RequestParam(required = false) Long storeId) {
        Page<BtDishInfo> pageResult = btDishInfoService.page(page, limit, storeId);
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "limit", limit
        ));
    }

    @RequestMapping("/lists")
    public Result<List<BtDishInfo>> lists(BtDishInfo query) {
        return Result.success(btDishInfoService.listAll(null));
    }

    @RequestMapping("/query")
    public Result<BtDishInfo> query(BtDishInfo query) {
        if (query.getId() != null) {
            return btDishInfoService.getById(query.getId())
                    .map(Result::success)
                    .orElse(Result.error(404, "菜品信息不存在"));
        }
        return Result.success(null);
    }

    @RequestMapping("/info/{id}")
    public Result<BtDishInfo> info(@PathVariable("id") Long id) {
        return btDishInfoService.getById(id)
                .map(d -> {
                    d.setClicktime(LocalDateTime.now());
                    btDishInfoService.update(d);
                    return Result.success(d);
                })
                .orElse(Result.error(404, "菜品信息不存在"));
    }

    @RequestMapping("/detail/{id}")
    public Result<BtDishInfo> detail(@PathVariable("id") Long id) {
        return btDishInfoService.getById(id)
                .map(d -> {
                    d.setClicktime(LocalDateTime.now());
                    btDishInfoService.update(d);
                    return Result.success(d);
                })
                .orElse(Result.error(404, "菜品信息不存在"));
    }

    @RequestMapping("/save")
    public Result<BtDishInfo> save(@RequestBody BtDishInfo entity) {
        if (entity.getId() == null) {
            entity.setId(System.currentTimeMillis() + (long)(Math.random() * 1000));
        }
        return Result.success(btDishInfoService.save(entity));
    }

    @RequestMapping("/add")
    public Result<BtDishInfo> add(@RequestBody BtDishInfo entity) {
        if (entity.getId() == null) {
            entity.setId(System.currentTimeMillis() + (long)(Math.random() * 1000));
        }
        return Result.success(btDishInfoService.save(entity));
    }

    @RequestMapping("/update")
    public Result<BtDishInfo> update(@RequestBody BtDishInfo entity) {
        return Result.success(btDishInfoService.update(entity));
    }

    @RequestMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        btDishInfoService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @RequestMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable("columnName") String columnName,
                                                   @PathVariable("type") String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = btDishInfoService.count(storeId);
        return Result.success(Map.of("count", count));
    }

    @RequestMapping("/autoSort")
    public Result<Map<String, Object>> autoSort(@RequestParam Map<String, Object> params,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int limit,
                                                @RequestParam(required = false) Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("clicktime").descending());
        Page<BtDishInfo> pageResult = btDishInfoService.searchWithPrice(null, null, null, null, page, limit);
        List<BtDishInfo> sorted = new ArrayList<>(pageResult.getContent());
        sorted.sort((a, b) -> {
            LocalDateTime ta = a.getClicktime() != null ? a.getClicktime() : LocalDateTime.MIN;
            LocalDateTime tb = b.getClicktime() != null ? b.getClicktime() : LocalDateTime.MIN;
            return tb.compareTo(ta);
        });
        return Result.success(Map.of(
                "list", sorted,
                "total", pageResult.getTotalElements(),
                "page", page,
                "limit", limit
        ));
    }

    @RequestMapping("/autoSort2")
    public Result<Map<String, Object>> autoSort2(@RequestParam Map<String, Object> params,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int limit,
                                                 @RequestParam(required = false) Long userid,
                                                 @RequestParam(required = false) Integer lim,
                                                 @RequestParam(required = false) Long storeId) {
        int pageLimit = lim != null ? lim : 10;
        List<BtDishInfo> resultList = new ArrayList<>();
        if (userid != null) {
            List<BtOrder> userOrders = btOrderRepo.findByUseridAndTablenameOrderByAddtimeDesc(userid, "caipinxinxi");
            Set<String> goodtypes = new LinkedHashSet<>();
            for (BtOrder o : userOrders) {
                if (o.getGoodtype() != null) goodtypes.add(o.getGoodtype());
            }
            if (!goodtypes.isEmpty()) {
                for (String gt : goodtypes) {
                    Page<BtDishInfo> byType = btDishInfoService.pageByType(gt, 1, pageLimit, storeId);
                    resultList.addAll(byType.getContent());
                    if (resultList.size() >= pageLimit) break;
                }
            }
        }
        if (resultList.size() < pageLimit) {
            Page<BtDishInfo> pageResult = btDishInfoService.page(page, limit, storeId);
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
                "limit", limit
        ));
    }
}
