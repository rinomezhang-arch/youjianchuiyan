package com.youjian.banquet.service;

import com.youjian.banquet.entity.PrPurchaseIn;
import com.youjian.banquet.repository.PrPurchaseInRepository;
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
public class PrPurchaseInService {

    @Autowired
    private PrPurchaseInRepository repository;

    public Page<PrPurchaseIn> queryPage(Map<String, Object> params, PrPurchaseIn entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<PrPurchaseIn> spec = buildSpecification(entity);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public List<PrPurchaseIn> selectList(PrPurchaseIn entity) {
        Specification<PrPurchaseIn> spec = buildSpecification(entity);
        return repository.findAll(spec);
    }

    public Optional<PrPurchaseIn> selectById(Long id) {
        return repository.findById(id);
    }

    public PrPurchaseIn selectOne(Specification<PrPurchaseIn> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public PrPurchaseIn insert(PrPurchaseIn entity) {
        if (entity.getId() == null) {
            entity.setId(System.currentTimeMillis() + (long) (Math.random() * 1000));
        }
        return repository.save(entity);
    }

    @Transactional
    public PrPurchaseIn updateById(PrPurchaseIn entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public long selectCount(Specification<PrPurchaseIn> spec) {
        return repository.count(spec);
    }

    private Specification<PrPurchaseIn> buildSpecification(PrPurchaseIn entity) {
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
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}