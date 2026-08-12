package com.youjian.banquet.service;

import com.youjian.banquet.entity.PrSupplier;
import com.youjian.banquet.entity.PrToken;
import com.youjian.banquet.repository.PrSupplierRepository;
import com.youjian.banquet.repository.PrTokenRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PrSupplierService {

    @Autowired
    private PrSupplierRepository repository;

    public Page<PrSupplier> queryPage(Map<String, Object> params, PrSupplier entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<PrSupplier> spec = buildSpecification(entity);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public List<PrSupplier> selectList(PrSupplier entity) {
        Specification<PrSupplier> spec = buildSpecification(entity);
        return repository.findAll(spec);
    }

    public Optional<PrSupplier> selectById(Long id) {
        return repository.findById(id);
    }

    public PrSupplier selectOne(Specification<PrSupplier> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public PrSupplier insert(PrSupplier entity) {
        return repository.save(entity);
    }

    @Transactional
    public PrSupplier updateById(PrSupplier entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public long selectCount(Specification<PrSupplier> spec) {
        return repository.count(spec);
    }

    private Specification<PrSupplier> buildSpecification(PrSupplier entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getStoreId() != null) {
                predicates.add(cb.equal(root.get("storeId"), entity.getStoreId()));
            }
            if (entity.getGongyingshangzhanghao() != null && !entity.getGongyingshangzhanghao().isEmpty()) {
                predicates.add(cb.like(root.get("gongyingshangzhanghao"), "%" + entity.getGongyingshangzhanghao() + "%"));
            }
            if (entity.getGongyingshangmingcheng() != null && !entity.getGongyingshangmingcheng().isEmpty()) {
                predicates.add(cb.like(root.get("gongyingshangmingcheng"), "%" + entity.getGongyingshangmingcheng() + "%"));
            }
            if (entity.getLianxiren() != null && !entity.getLianxiren().isEmpty()) {
                predicates.add(cb.like(root.get("lianxiren"), "%" + entity.getLianxiren() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}