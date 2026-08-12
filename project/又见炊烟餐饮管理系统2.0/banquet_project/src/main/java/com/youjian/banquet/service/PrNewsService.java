package com.youjian.banquet.service;

import com.youjian.banquet.entity.PrNews;
import com.youjian.banquet.repository.PrNewsRepository;
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
public class PrNewsService {

    @Autowired
    private PrNewsRepository repository;

    public Page<PrNews> queryPage(Map<String, Object> params, PrNews entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<PrNews> spec = buildSpecification(entity);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public List<PrNews> selectList(PrNews entity) {
        Specification<PrNews> spec = buildSpecification(entity);
        return repository.findAll(spec);
    }

    public Optional<PrNews> selectById(Long id) {
        return repository.findById(id);
    }

    public PrNews selectOne(Specification<PrNews> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public PrNews insert(PrNews entity) {
        return repository.save(entity);
    }

    @Transactional
    public PrNews updateById(PrNews entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public long selectCount(Specification<PrNews> spec) {
        return repository.count(spec);
    }

    private Specification<PrNews> buildSpecification(PrNews entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getStoreId() != null) {
                predicates.add(cb.equal(root.get("storeId"), entity.getStoreId()));
            }
            if (entity.getTitle() != null && !entity.getTitle().isEmpty()) {
                predicates.add(cb.like(root.get("title"), "%" + entity.getTitle() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}