package com.youjian.banquet.service;

import com.youjian.banquet.entity.PrMaterialInfo;
import com.youjian.banquet.repository.PrMaterialInfoRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PrMaterialInfoService {

    @Autowired
    private PrMaterialInfoRepository repository;

    public Page<PrMaterialInfo> queryPage(Map<String, Object> params, PrMaterialInfo entity, Double pricestart, Double priceend) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<PrMaterialInfo> spec = buildSpecification(entity, pricestart, priceend);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public List<PrMaterialInfo> selectList(PrMaterialInfo entity) {
        Specification<PrMaterialInfo> spec = buildSpecification(entity, null, null);
        return repository.findAll(spec);
    }

    public Optional<PrMaterialInfo> selectById(Long id) {
        return repository.findById(id);
    }

    public PrMaterialInfo selectOne(Specification<PrMaterialInfo> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public PrMaterialInfo insert(PrMaterialInfo entity) {
        if (entity.getId() == null) {
            entity.setId(System.currentTimeMillis() + (long) (Math.random() * 1000));
        }
        return repository.save(entity);
    }

    @Transactional
    public PrMaterialInfo updateById(PrMaterialInfo entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public long selectCount(Specification<PrMaterialInfo> spec) {
        return repository.count(spec);
    }

    private Specification<PrMaterialInfo> buildSpecification(PrMaterialInfo entity, Double pricestart, Double priceend) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getStoreId() != null) {
                predicates.add(cb.equal(root.get("storeId"), entity.getStoreId()));
            }
            if (entity.getCailiaomingcheng() != null && !entity.getCailiaomingcheng().isEmpty()) {
                predicates.add(cb.like(root.get("cailiaomingcheng"), "%" + entity.getCailiaomingcheng() + "%"));
            }
            if (entity.getCailiaozhonglei() != null && !entity.getCailiaozhonglei().isEmpty()) {
                predicates.add(cb.equal(root.get("cailiaozhonglei"), entity.getCailiaozhonglei()));
            }
            if (entity.getCailiaoguige() != null && !entity.getCailiaoguige().isEmpty()) {
                predicates.add(cb.like(root.get("cailiaoguige"), "%" + entity.getCailiaoguige() + "%"));
            }
            if (entity.getGongyingshangzhanghao() != null && !entity.getGongyingshangzhanghao().isEmpty()) {
                predicates.add(cb.equal(root.get("gongyingshangzhanghao"), entity.getGongyingshangzhanghao()));
            }
            if (entity.getGongyingshangmingcheng() != null && !entity.getGongyingshangmingcheng().isEmpty()) {
                predicates.add(cb.equal(root.get("gongyingshangmingcheng"), entity.getGongyingshangmingcheng()));
            }
            if (pricestart != null) {
                predicates.add(cb.ge(root.get("price"), pricestart));
            }
            if (priceend != null) {
                predicates.add(cb.le(root.get("price"), priceend));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}