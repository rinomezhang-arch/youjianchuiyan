package com.youjian.banquet.service;

import com.youjian.banquet.entity.PurchaseIn;
import com.youjian.banquet.repository.PurchaseInRepository;
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
public class PurchaseInService {

    @Autowired
    private PurchaseInRepository repository;

    public Page<PurchaseIn> queryPage(Map<String, Object> params, PurchaseIn entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<PurchaseIn> spec = buildSpecification(entity);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public List<PurchaseIn> selectList(PurchaseIn entity) {
        Specification<PurchaseIn> spec = buildSpecification(entity);
        return repository.findAll(spec);
    }

    public Optional<PurchaseIn> selectById(Long id) {
        return repository.findById(id);
    }

    public PurchaseIn selectOne(Specification<PurchaseIn> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public PurchaseIn insert(PurchaseIn entity) {
        return repository.save(entity);
    }

    @Transactional
    public PurchaseIn updateById(PurchaseIn entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public long selectCount(Specification<PurchaseIn> spec) {
        return repository.count(spec);
    }

    private Specification<PurchaseIn> buildSpecification(PurchaseIn entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getStoreId() != null) {
                predicates.add(cb.equal(root.get("storeId"), entity.getStoreId()));
            }
            if (entity.getMaterialName() != null && !entity.getMaterialName().isEmpty()) {
                predicates.add(cb.like(root.get("materialName"), "%" + entity.getMaterialName() + "%"));
            }
            if (entity.getCategory() != null && !entity.getCategory().isEmpty()) {
                predicates.add(cb.like(root.get("category"), "%" + entity.getCategory() + "%"));
            }
            if (entity.getSupplierAccount() != null && !entity.getSupplierAccount().isEmpty()) {
                predicates.add(cb.like(root.get("supplierAccount"), "%" + entity.getSupplierAccount() + "%"));
            }
            if (entity.getSupplierName() != null && !entity.getSupplierName().isEmpty()) {
                predicates.add(cb.like(root.get("supplierName"), "%" + entity.getSupplierName() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}