package com.youjian.banquet.service;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrSocCity;
import com.youjian.banquet.repository.HrSocCityRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 参保城市服务 (HR系统复刻)
 * 来源: HR系统 CityService
 * 功能: 参保城市CRUD、分页查询、Excel导入导出
 */
@Service
public class HrSocCityService {

    @Autowired
    private HrSocCityRepository cityRepository;

    /**
     * 新增参保城市
     */
    @Transactional
    public Result<HrSocCity> add(HrSocCity city) {
        HrSocCity saved = cityRepository.save(city);
        return Result.success(saved);
    }

    /**
     * 根据ID逻辑删除（软删除）
     */
    @Transactional
    public Result<String> deleteById(Integer id) {
        Optional<HrSocCity> opt = cityRepository.findById(id);
        if (opt.isPresent()) {
            HrSocCity city = opt.get();
            city.setIsDeleted(1);
            cityRepository.save(city);
            return Result.success("删除成功");
        }
        return Result.error(404, "城市不存在");
    }

    /**
     * 批量逻辑删除
     */
    @Transactional
    public Result<String> deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            Optional<HrSocCity> opt = cityRepository.findById(id);
            if (opt.isPresent()) {
                HrSocCity city = opt.get();
                city.setIsDeleted(1);
                cityRepository.save(city);
            }
        }
        return Result.success("批量删除成功");
    }

    /**
     * 编辑更新
     */
    @Transactional
    public Result<HrSocCity> edit(HrSocCity city) {
        if (city.getId() == null || !cityRepository.existsById(city.getId())) {
            return Result.error(404, "城市不存在");
        }
        HrSocCity updated = cityRepository.save(city);
        return Result.success(updated);
    }

    /**
     * 根据ID查询
     */
    public Result<HrSocCity> findById(Integer id) {
        return cityRepository.findById(id)
                .map(Result::success)
                .orElse(Result.error(404, "城市不存在"));
    }

    /**
     * 查询所有（未删除）
     */
    public Result<List<HrSocCity>> findAll(Long storeId) {
        List<HrSocCity> list = cityRepository.findByStoreIdAndIsDeletedOrderByName(storeId, 0);
        return Result.success(list);
    }

    /**
     * 分页条件查询（按城市名称模糊搜索，过滤已删除）
     */
    public Result<Map<String, Object>> list(Integer current, Integer size, Long storeId, String name) {
        PageRequest pageRequest = PageRequest.of(current - 1, size, Sort.by(Sort.Direction.ASC, "name"));

        Specification<HrSocCity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("storeId"), storeId));
            predicates.add(cb.equal(root.get("isDeleted"), 0));
            if (StringUtils.hasText(name)) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<HrSocCity> page = cityRepository.findAll(spec, pageRequest);

        Map<String, Object> map = new HashMap<>();
        map.put("pages", page.getTotalPages());
        map.put("total", page.getTotalElements());
        map.put("list", page.getContent());
        return Result.success(map);
    }

    /**
     * 根据城市名称查询
     */
    public Result<HrSocCity> findByName(Long storeId, String name) {
        return cityRepository.findByStoreIdAndNameAndIsDeleted(storeId, name, 0)
                .map(Result::success)
                .orElse(Result.error(404, "城市不存在"));
    }
}