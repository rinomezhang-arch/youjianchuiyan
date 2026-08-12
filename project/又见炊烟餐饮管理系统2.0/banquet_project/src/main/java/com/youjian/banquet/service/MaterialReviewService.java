package com.youjian.banquet.service;

import com.youjian.banquet.entity.MaterialReview;
import com.youjian.banquet.repository.MaterialReviewRepository;
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
public class MaterialReviewService {

    @Autowired
    private MaterialReviewRepository repository;

    public Page<MaterialReview> queryPage(Map<String, Object> params, MaterialReview entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<MaterialReview> spec = buildSpecification(entity);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public List<MaterialReview> selectList(MaterialReview entity) {
        Specification<MaterialReview> spec = buildSpecification(entity);
        return repository.findAll(spec);
    }

    public Optional<MaterialReview> selectById(Long id) {
        return repository.findById(id);
    }

    public MaterialReview selectOne(Specification<MaterialReview> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public MaterialReview insert(MaterialReview entity) {
        return repository.save(entity);
    }

    @Transactional
    public MaterialReview updateById(MaterialReview entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public long selectCount(Specification<MaterialReview> spec) {
        return repository.count(spec);
    }

    private Specification<MaterialReview> buildSpecification(MaterialReview entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getStoreId() != null) {
                predicates.add(cb.equal(root.get("storeId"), entity.getStoreId()));
            }
            if (entity.getRefId() != null) {
                predicates.add(cb.equal(root.get("refId"), entity.getRefId()));
            }
            if (entity.getUserId() != null) {
                predicates.add(cb.equal(root.get("userId"), entity.getUserId()));
            }
            if (entity.getNickname() != null && !entity.getNickname().isEmpty()) {
                predicates.add(cb.like(root.get("nickname"), "%" + entity.getNickname() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}