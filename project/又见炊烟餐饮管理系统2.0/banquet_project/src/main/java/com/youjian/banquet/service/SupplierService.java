package com.youjian.banquet.service;

import com.youjian.banquet.entity.Supplier;
import com.youjian.banquet.repository.SupplierRepository;
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
public class SupplierService {

    @Autowired
    private SupplierRepository repository;

    public Page<Supplier> queryPage(Map<String, Object> params, Supplier entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<Supplier> spec = buildSpecification(entity);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public List<Supplier> selectList(Supplier entity) {
        Specification<Supplier> spec = buildSpecification(entity);
        return repository.findAll(spec);
    }

    public Optional<Supplier> selectById(Long id) {
        return repository.findById(id);
    }

    public Supplier selectOne(Specification<Supplier> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public Supplier insert(Supplier entity) {
        return repository.save(entity);
    }

    @Transactional
    public Supplier updateById(Supplier entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public long selectCount(Specification<Supplier> spec) {
        return repository.count(spec);
    }

    private Specification<Supplier> buildSpecification(Supplier entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getStoreId() != null) {
                predicates.add(cb.equal(root.get("storeId"), entity.getStoreId()));
            }
            if (entity.getSupplierAccount() != null && !entity.getSupplierAccount().isEmpty()) {
                predicates.add(cb.like(root.get("supplierAccount"), "%" + entity.getSupplierAccount() + "%"));
            }
            if (entity.getSupplierName() != null && !entity.getSupplierName().isEmpty()) {
                predicates.add(cb.like(root.get("supplierName"), "%" + entity.getSupplierName() + "%"));
            }
            if (entity.getContactPerson() != null && !entity.getContactPerson().isEmpty()) {
                predicates.add(cb.like(root.get("contactPerson"), "%" + entity.getContactPerson() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}