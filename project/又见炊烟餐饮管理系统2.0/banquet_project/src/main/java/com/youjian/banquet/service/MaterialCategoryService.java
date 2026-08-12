package com.youjian.banquet.service;

import com.youjian.banquet.entity.MaterialCategory;
import com.youjian.banquet.repository.MaterialCategoryRepository;
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
public class MaterialCategoryService {

    @Autowired
    private MaterialCategoryRepository repository;

    public Page<MaterialCategory> queryPage(Map<String, Object> params, MaterialCategory entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<MaterialCategory> spec = buildSpecification(entity);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public List<MaterialCategory> selectList(MaterialCategory entity) {
        Specification<MaterialCategory> spec = buildSpecification(entity);
        return repository.findAll(spec);
    }

    public Optional<MaterialCategory> selectById(Long id) {
        return repository.findById(id);
    }

    public MaterialCategory selectOne(Specification<MaterialCategory> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public MaterialCategory insert(MaterialCategory entity) {
        return repository.save(entity);
    }

    @Transactional
    public MaterialCategory updateById(MaterialCategory entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public long selectCount(Specification<MaterialCategory> spec) {
        return repository.count(spec);
    }

    private Specification<MaterialCategory> buildSpecification(MaterialCategory entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getStoreId() != null) {
                predicates.add(cb.equal(root.get("storeId"), entity.getStoreId()));
            }
            if (entity.getCategoryName() != null && !entity.getCategoryName().isEmpty()) {
                predicates.add(cb.like(root.get("categoryName"), "%" + entity.getCategoryName() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}