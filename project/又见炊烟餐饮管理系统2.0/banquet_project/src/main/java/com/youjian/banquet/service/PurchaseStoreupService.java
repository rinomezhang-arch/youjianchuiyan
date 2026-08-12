package com.youjian.banquet.service;

import com.youjian.banquet.entity.PurchaseStoreup;
import com.youjian.banquet.repository.PurchaseStoreupRepository;
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
public class PurchaseStoreupService {

    @Autowired
    private PurchaseStoreupRepository repository;

    public Page<PurchaseStoreup> queryPage(Map<String, Object> params, PurchaseStoreup entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<PurchaseStoreup> spec = buildSpecification(entity);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public List<PurchaseStoreup> selectList(PurchaseStoreup entity) {
        Specification<PurchaseStoreup> spec = buildSpecification(entity);
        return repository.findAll(spec);
    }

    public Optional<PurchaseStoreup> selectById(Long id) {
        return repository.findById(id);
    }

    public PurchaseStoreup selectOne(Specification<PurchaseStoreup> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public PurchaseStoreup insert(PurchaseStoreup entity) {
        return repository.save(entity);
    }

    @Transactional
    public PurchaseStoreup updateById(PurchaseStoreup entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public long selectCount(Specification<PurchaseStoreup> spec) {
        return repository.count(spec);
    }

    private Specification<PurchaseStoreup> buildSpecification(PurchaseStoreup entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getStoreId() != null) {
                predicates.add(cb.equal(root.get("storeId"), entity.getStoreId()));
            }
            if (entity.getUserId() != null) {
                predicates.add(cb.equal(root.get("userId"), entity.getUserId()));
            }
            if (entity.getRefId() != null) {
                predicates.add(cb.equal(root.get("refId"), entity.getRefId()));
            }
            if (entity.getTableName() != null && !entity.getTableName().isEmpty()) {
                predicates.add(cb.equal(root.get("tableName"), entity.getTableName()));
            }
            if (entity.getName() != null && !entity.getName().isEmpty()) {
                predicates.add(cb.like(root.get("name"), "%" + entity.getName() + "%"));
            }
            if (entity.getType() != null && !entity.getType().isEmpty()) {
                predicates.add(cb.equal(root.get("type"), entity.getType()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}