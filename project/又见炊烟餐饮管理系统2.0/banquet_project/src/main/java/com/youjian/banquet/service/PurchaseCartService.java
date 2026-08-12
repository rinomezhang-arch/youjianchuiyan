package com.youjian.banquet.service;

import com.youjian.banquet.entity.PurchaseCart;
import com.youjian.banquet.repository.PurchaseCartRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PurchaseCartService {

    @Autowired
    private PurchaseCartRepository repository;

    public Page<PurchaseCart> queryPage(Map<String, Object> params, PurchaseCart entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<PurchaseCart> spec = buildSpecification(entity);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public List<PurchaseCart> selectList(PurchaseCart entity) {
        Specification<PurchaseCart> spec = buildSpecification(entity);
        return repository.findAll(spec);
    }

    public Optional<PurchaseCart> selectById(Long id) {
        return repository.findById(id);
    }

    public PurchaseCart selectOne(Specification<PurchaseCart> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public PurchaseCart insert(PurchaseCart entity) {
        return repository.save(entity);
    }

    @Transactional
    public PurchaseCart updateById(PurchaseCart entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public long selectCount(Specification<PurchaseCart> spec) {
        return repository.count(spec);
    }

    private Specification<PurchaseCart> buildSpecification(PurchaseCart entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getStoreId() != null) {
                predicates.add(cb.equal(root.get("storeId"), entity.getStoreId()));
            }
            if (entity.getUserId() != null) {
                predicates.add(cb.equal(root.get("userId"), entity.getUserId()));
            }
            if (entity.getGoodId() != null) {
                predicates.add(cb.equal(root.get("goodId"), entity.getGoodId()));
            }
            if (entity.getGoodName() != null && !entity.getGoodName().isEmpty()) {
                predicates.add(cb.like(root.get("goodName"), "%" + entity.getGoodName() + "%"));
            }
            if (entity.getTableName() != null && !entity.getTableName().isEmpty()) {
                predicates.add(cb.equal(root.get("tableName"), entity.getTableName()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}