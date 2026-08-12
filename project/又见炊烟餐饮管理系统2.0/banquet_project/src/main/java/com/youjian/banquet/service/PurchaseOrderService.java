package com.youjian.banquet.service;

import com.youjian.banquet.entity.PurchaseOrder;
import com.youjian.banquet.repository.PurchaseOrderRepository;
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
public class PurchaseOrderService {

    @Autowired
    private PurchaseOrderRepository repository;

    public Page<PurchaseOrder> queryPage(Map<String, Object> params, PurchaseOrder entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<PurchaseOrder> spec = buildSpecification(entity);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public List<PurchaseOrder> selectList(PurchaseOrder entity) {
        Specification<PurchaseOrder> spec = buildSpecification(entity);
        return repository.findAll(spec);
    }

    public Optional<PurchaseOrder> selectById(Long id) {
        return repository.findById(id);
    }

    public PurchaseOrder selectOne(Specification<PurchaseOrder> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public PurchaseOrder insert(PurchaseOrder entity) {
        return repository.save(entity);
    }

    @Transactional
    public PurchaseOrder updateById(PurchaseOrder entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public long selectCount(Specification<PurchaseOrder> spec) {
        return repository.count(spec);
    }

    private Specification<PurchaseOrder> buildSpecification(PurchaseOrder entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getStoreId() != null) {
                predicates.add(cb.equal(root.get("storeId"), entity.getStoreId()));
            }
            if (entity.getOrderId() != null && !entity.getOrderId().isEmpty()) {
                predicates.add(cb.like(root.get("orderId"), "%" + entity.getOrderId() + "%"));
            }
            if (entity.getUserId() != null) {
                predicates.add(cb.equal(root.get("userId"), entity.getUserId()));
            }
            if (entity.getGoodName() != null && !entity.getGoodName().isEmpty()) {
                predicates.add(cb.like(root.get("goodName"), "%" + entity.getGoodName() + "%"));
            }
            if (entity.getStatus() != null && !entity.getStatus().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), entity.getStatus()));
            }
            if (entity.getTableName() != null && !entity.getTableName().isEmpty()) {
                predicates.add(cb.equal(root.get("tableName"), entity.getTableName()));
            }
            if (entity.getSupplierAccount() != null && !entity.getSupplierAccount().isEmpty()) {
                predicates.add(cb.like(root.get("supplierAccount"), "%" + entity.getSupplierAccount() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}