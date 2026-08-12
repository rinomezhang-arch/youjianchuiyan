package com.youjian.banquet.service;

import com.youjian.banquet.entity.MaterialInfo;
import com.youjian.banquet.repository.MaterialInfoRepository;
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
public class MaterialInfoService {

    @Autowired
    private MaterialInfoRepository repository;

    public Page<MaterialInfo> queryPage(Map<String, Object> params, MaterialInfo entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<MaterialInfo> spec = buildSpecification(entity);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public List<MaterialInfo> selectList(MaterialInfo entity) {
        Specification<MaterialInfo> spec = buildSpecification(entity);
        return repository.findAll(spec);
    }

    public Optional<MaterialInfo> selectById(Long id) {
        return repository.findById(id);
    }

    public MaterialInfo selectOne(Specification<MaterialInfo> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public MaterialInfo insert(MaterialInfo entity) {
        return repository.save(entity);
    }

    @Transactional
    public MaterialInfo updateById(MaterialInfo entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public long selectCount(Specification<MaterialInfo> spec) {
        return repository.count(spec);
    }

    private Specification<MaterialInfo> buildSpecification(MaterialInfo entity) {
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