package com.youjian.banquet.service;

import com.youjian.banquet.entity.PrConfig;
import com.youjian.banquet.repository.PrConfigRepository;
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
public class PrConfigService {

    @Autowired
    private PrConfigRepository repository;

    public Page<PrConfig> queryPage(Map<String, Object> params, PrConfig entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<PrConfig> spec = buildSpecification(entity);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public Optional<PrConfig> selectById(Long id) {
        return repository.findById(id);
    }

    public PrConfig selectOne(Specification<PrConfig> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public PrConfig insert(PrConfig entity) {
        return repository.save(entity);
    }

    @Transactional
    public PrConfig updateById(PrConfig entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    private Specification<PrConfig> buildSpecification(PrConfig entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getStoreId() != null) {
                predicates.add(cb.equal(root.get("storeId"), entity.getStoreId()));
            }
            if (entity.getName() != null && !entity.getName().isEmpty()) {
                predicates.add(cb.like(root.get("name"), "%" + entity.getName() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}