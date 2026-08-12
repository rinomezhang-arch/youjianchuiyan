package com.youjian.banquet.service;

import com.youjian.banquet.entity.PrOrder;
import com.youjian.banquet.repository.PrOrderRepository;
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
public class PrOrderService {

    @Autowired
    private PrOrderRepository repository;

    public Page<PrOrder> queryPage(Map<String, Object> params, PrOrder entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<PrOrder> spec = buildSpecification(entity);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public List<PrOrder> selectList(PrOrder entity) {
        Specification<PrOrder> spec = buildSpecification(entity);
        return repository.findAll(spec);
    }

    public Optional<PrOrder> selectById(Long id) {
        return repository.findById(id);
    }

    public PrOrder selectOne(Specification<PrOrder> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public PrOrder insert(PrOrder entity) {
        return repository.save(entity);
    }

    @Transactional
    public PrOrder updateById(PrOrder entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public long selectCount(Specification<PrOrder> spec) {
        return repository.count(spec);
    }

    private Specification<PrOrder> buildSpecification(PrOrder entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getStoreId() != null) {
                predicates.add(cb.equal(root.get("storeId"), entity.getStoreId()));
            }
            if (entity.getOrderid() != null && !entity.getOrderid().isEmpty()) {
                predicates.add(cb.like(root.get("orderid"), "%" + entity.getOrderid() + "%"));
            }
            if (entity.getUserid() != null) {
                predicates.add(cb.equal(root.get("userid"), entity.getUserid()));
            }
            if (entity.getGongyingshangzhanghao() != null && !entity.getGongyingshangzhanghao().isEmpty()) {
                predicates.add(cb.equal(root.get("gongyingshangzhanghao"), entity.getGongyingshangzhanghao()));
            }
            if (entity.getStatus() != null && !entity.getStatus().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), entity.getStatus()));
            }
            if (entity.getGoodname() != null && !entity.getGoodname().isEmpty()) {
                predicates.add(cb.like(root.get("goodname"), "%" + entity.getGoodname() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}