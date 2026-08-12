package com.youjian.banquet.service;

import com.youjian.banquet.entity.PrStoreup;
import com.youjian.banquet.repository.PrStoreupRepository;
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
public class PrStoreupService {

    @Autowired
    private PrStoreupRepository repository;

    public Page<PrStoreup> queryPage(Map<String, Object> params, PrStoreup entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<PrStoreup> spec = buildSpecification(entity);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public List<PrStoreup> selectList(PrStoreup entity) {
        Specification<PrStoreup> spec = buildSpecification(entity);
        return repository.findAll(spec);
    }

    public Optional<PrStoreup> selectById(Long id) {
        return repository.findById(id);
    }

    public PrStoreup selectOne(Specification<PrStoreup> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public PrStoreup insert(PrStoreup entity) {
        return repository.save(entity);
    }

    @Transactional
    public PrStoreup updateById(PrStoreup entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public long selectCount(Specification<PrStoreup> spec) {
        return repository.count(spec);
    }

    private Specification<PrStoreup> buildSpecification(PrStoreup entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getStoreId() != null) {
                predicates.add(cb.equal(root.get("storeId"), entity.getStoreId()));
            }
            if (entity.getUserid() != null) {
                predicates.add(cb.equal(root.get("userid"), entity.getUserid()));
            }
            if (entity.getRefid() != null) {
                predicates.add(cb.equal(root.get("refid"), entity.getRefid()));
            }
            if (entity.getName() != null && !entity.getName().isEmpty()) {
                predicates.add(cb.like(root.get("name"), "%" + entity.getName() + "%"));
            }
            if (entity.getType() != null && !entity.getType().isEmpty()) {
                predicates.add(cb.equal(root.get("type"), entity.getType()));
            }
            if (entity.getTablename() != null && !entity.getTablename().isEmpty()) {
                predicates.add(cb.equal(root.get("tablename"), entity.getTablename()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}