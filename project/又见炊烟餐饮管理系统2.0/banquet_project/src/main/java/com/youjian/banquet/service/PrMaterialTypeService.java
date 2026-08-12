package com.youjian.banquet.service;

import com.youjian.banquet.entity.PrMaterialType;
import com.youjian.banquet.repository.PrMaterialTypeRepository;
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
public class PrMaterialTypeService {

    @Autowired
    private PrMaterialTypeRepository repository;

    public Page<PrMaterialType> queryPage(Map<String, Object> params, PrMaterialType entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<PrMaterialType> spec = buildSpecification(entity);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public List<PrMaterialType> selectList(PrMaterialType entity) {
        Specification<PrMaterialType> spec = buildSpecification(entity);
        return repository.findAll(spec);
    }

    public Optional<PrMaterialType> selectById(Long id) {
        return repository.findById(id);
    }

    public PrMaterialType selectOne(Specification<PrMaterialType> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public PrMaterialType insert(PrMaterialType entity) {
        if (entity.getId() == null) {
            entity.setId(System.currentTimeMillis() + (long) (Math.random() * 1000));
        }
        return repository.save(entity);
    }

    @Transactional
    public PrMaterialType updateById(PrMaterialType entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public long selectCount(Specification<PrMaterialType> spec) {
        return repository.count(spec);
    }

    private Specification<PrMaterialType> buildSpecification(PrMaterialType entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getStoreId() != null) {
                predicates.add(cb.equal(root.get("storeId"), entity.getStoreId()));
            }
            if (entity.getCailiaozhonglei() != null && !entity.getCailiaozhonglei().isEmpty()) {
                predicates.add(cb.like(root.get("cailiaozhonglei"), "%" + entity.getCailiaozhonglei() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}