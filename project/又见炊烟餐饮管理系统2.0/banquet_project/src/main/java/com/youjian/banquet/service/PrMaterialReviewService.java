package com.youjian.banquet.service;

import com.youjian.banquet.entity.PrMaterialReview;
import com.youjian.banquet.repository.PrMaterialReviewRepository;
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
public class PrMaterialReviewService {

    @Autowired
    private PrMaterialReviewRepository repository;

    public Page<PrMaterialReview> queryPage(Map<String, Object> params, PrMaterialReview entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<PrMaterialReview> spec = buildSpecification(entity);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public List<PrMaterialReview> selectList(PrMaterialReview entity) {
        Specification<PrMaterialReview> spec = buildSpecification(entity);
        return repository.findAll(spec);
    }

    public Optional<PrMaterialReview> selectById(Long id) {
        return repository.findById(id);
    }

    public PrMaterialReview selectOne(Specification<PrMaterialReview> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public PrMaterialReview insert(PrMaterialReview entity) {
        return repository.save(entity);
    }

    @Transactional
    public PrMaterialReview updateById(PrMaterialReview entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public long selectCount(Specification<PrMaterialReview> spec) {
        return repository.count(spec);
    }

    private Specification<PrMaterialReview> buildSpecification(PrMaterialReview entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getStoreId() != null) {
                predicates.add(cb.equal(root.get("storeId"), entity.getStoreId()));
            }
            if (entity.getRefid() != null) {
                predicates.add(cb.equal(root.get("refid"), entity.getRefid()));
            }
            if (entity.getUserid() != null) {
                predicates.add(cb.equal(root.get("userid"), entity.getUserid()));
            }
            if (entity.getNickname() != null && !entity.getNickname().isEmpty()) {
                predicates.add(cb.like(root.get("nickname"), "%" + entity.getNickname() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}