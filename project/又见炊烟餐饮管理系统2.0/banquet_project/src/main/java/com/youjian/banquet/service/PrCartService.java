package com.youjian.banquet.service;

import com.youjian.banquet.entity.PrCart;
import com.youjian.banquet.repository.PrCartRepository;
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
public class PrCartService {

    @Autowired
    private PrCartRepository repository;

    public Page<PrCart> queryPage(Map<String, Object> params, PrCart entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<PrCart> spec = buildSpecification(entity);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public List<PrCart> selectList(PrCart entity) {
        Specification<PrCart> spec = buildSpecification(entity);
        return repository.findAll(spec);
    }

    public Optional<PrCart> selectById(Long id) {
        return repository.findById(id);
    }

    public PrCart selectOne(Specification<PrCart> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public PrCart insert(PrCart entity) {
        return repository.save(entity);
    }

    @Transactional
    public PrCart updateById(PrCart entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public long selectCount(Specification<PrCart> spec) {
        return repository.count(spec);
    }

    private Specification<PrCart> buildSpecification(PrCart entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getStoreId() != null) {
                predicates.add(cb.equal(root.get("storeId"), entity.getStoreId()));
            }
            if (entity.getUserid() != null) {
                predicates.add(cb.equal(root.get("userid"), entity.getUserid()));
            }
            if (entity.getGoodid() != null) {
                predicates.add(cb.equal(root.get("goodid"), entity.getGoodid()));
            }
            if (entity.getGoodname() != null && !entity.getGoodname().isEmpty()) {
                predicates.add(cb.like(root.get("goodname"), "%" + entity.getGoodname() + "%"));
            }
            if (entity.getGongyingshangzhanghao() != null && !entity.getGongyingshangzhanghao().isEmpty()) {
                predicates.add(cb.equal(root.get("gongyingshangzhanghao"), entity.getGongyingshangzhanghao()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}