package com.youjian.banquet.service;

import com.youjian.banquet.entity.PrUser;
import com.youjian.banquet.repository.PrUserRepository;
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
public class PrUserService {

    @Autowired
    private PrUserRepository repository;

    public Page<PrUser> queryPage(Map<String, Object> params, PrUser entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<PrUser> spec = buildSpecification(entity);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public List<PrUser> selectList(PrUser entity) {
        Specification<PrUser> spec = buildSpecification(entity);
        return repository.findAll(spec);
    }

    public Optional<PrUser> selectById(Long id) {
        return repository.findById(id);
    }

    public PrUser selectOne(Specification<PrUser> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public PrUser insert(PrUser entity) {
        return repository.save(entity);
    }

    @Transactional
    public PrUser updateById(PrUser entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public long selectCount(Specification<PrUser> spec) {
        return repository.count(spec);
    }

    private Specification<PrUser> buildSpecification(PrUser entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getStoreId() != null) {
                predicates.add(cb.equal(root.get("storeId"), entity.getStoreId()));
            }
            if (entity.getUsername() != null && !entity.getUsername().isEmpty()) {
                predicates.add(cb.like(root.get("username"), "%" + entity.getUsername() + "%"));
            }
            if (entity.getRole() != null && !entity.getRole().isEmpty()) {
                predicates.add(cb.equal(root.get("role"), entity.getRole()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}