package com.youjian.banquet.service;

import com.youjian.banquet.entity.SysConfig;
import com.youjian.banquet.repository.SysConfigRepository;
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
public class SysConfigService {

    @Autowired
    private SysConfigRepository repository;

    public Page<SysConfig> queryPage(Map<String, Object> params, SysConfig entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<SysConfig> spec = buildSpecification(entity);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public List<SysConfig> selectList(SysConfig entity) {
        Specification<SysConfig> spec = buildSpecification(entity);
        return repository.findAll(spec);
    }

    public Optional<SysConfig> selectById(Long id) {
        return repository.findById(id);
    }

    public SysConfig selectOne(Specification<SysConfig> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public SysConfig insert(SysConfig entity) {
        return repository.save(entity);
    }

    @Transactional
    public SysConfig updateById(SysConfig entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public long selectCount(Specification<SysConfig> spec) {
        return repository.count(spec);
    }

    private Specification<SysConfig> buildSpecification(SysConfig entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getStoreId() != null) {
                predicates.add(cb.equal(root.get("storeId"), entity.getStoreId()));
            }
            if (entity.getName() != null && !entity.getName().isEmpty()) {
                predicates.add(cb.like(root.get("name"), "%" + entity.getName() + "%"));
            }
            if (entity.getValue() != null && !entity.getValue().isEmpty()) {
                predicates.add(cb.like(root.get("value"), "%" + entity.getValue() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}