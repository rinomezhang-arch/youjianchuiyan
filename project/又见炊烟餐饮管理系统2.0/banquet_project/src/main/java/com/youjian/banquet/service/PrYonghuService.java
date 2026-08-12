package com.youjian.banquet.service;

import com.youjian.banquet.entity.PrYonghu;
import com.youjian.banquet.repository.PrYonghuRepository;
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
public class PrYonghuService {

    @Autowired
    private PrYonghuRepository repository;

    public Page<PrYonghu> queryPage(Map<String, Object> params, PrYonghu entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<PrYonghu> spec = buildSpecification(entity);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public List<PrYonghu> selectList(PrYonghu entity) {
        Specification<PrYonghu> spec = buildSpecification(entity);
        return repository.findAll(spec);
    }

    public Optional<PrYonghu> selectById(Long id) {
        return repository.findById(id);
    }

    public PrYonghu selectOne(Specification<PrYonghu> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public PrYonghu insert(PrYonghu entity) {
        return repository.save(entity);
    }

    @Transactional
    public PrYonghu updateById(PrYonghu entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public long selectCount(Specification<PrYonghu> spec) {
        return repository.count(spec);
    }

    private Specification<PrYonghu> buildSpecification(PrYonghu entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getStoreId() != null) {
                predicates.add(cb.equal(root.get("storeId"), entity.getStoreId()));
            }
            if (entity.getYonghuzhanghao() != null && !entity.getYonghuzhanghao().isEmpty()) {
                predicates.add(cb.like(root.get("yonghuzhanghao"), "%" + entity.getYonghuzhanghao() + "%"));
            }
            if (entity.getYonghuxingming() != null && !entity.getYonghuxingming().isEmpty()) {
                predicates.add(cb.like(root.get("yonghuxingming"), "%" + entity.getYonghuxingming() + "%"));
            }
            if (entity.getXingbie() != null && !entity.getXingbie().isEmpty()) {
                predicates.add(cb.equal(root.get("xingbie"), entity.getXingbie()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}