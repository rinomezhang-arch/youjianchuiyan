package com.youjian.banquet.service;

import com.youjian.banquet.entity.PrAddress;
import com.youjian.banquet.repository.PrAddressRepository;
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
public class PrAddressService {

    @Autowired
    private PrAddressRepository repository;

    public Page<PrAddress> queryPage(Map<String, Object> params, PrAddress entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        String sortField = params.getOrDefault("sort", "id").toString();
        String order = params.getOrDefault("order", "desc").toString();

        Specification<PrAddress> spec = buildSpecification(entity);
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, sort));
    }

    public List<PrAddress> selectList(PrAddress entity) {
        Specification<PrAddress> spec = buildSpecification(entity);
        return repository.findAll(spec);
    }

    public Optional<PrAddress> selectById(Long id) {
        return repository.findById(id);
    }

    public PrAddress selectOne(Specification<PrAddress> spec) {
        return repository.findOne(spec).orElse(null);
    }

    @Transactional
    public PrAddress insert(PrAddress entity) {
        return repository.save(entity);
    }

    @Transactional
    public PrAddress updateById(PrAddress entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    @Transactional
    public void updateForSet(String setClause, Long userid) {
        // 将同一用户的所有地址设为非默认
        List<PrAddress> addresses = repository.findByUserid(userid);
        for (PrAddress addr : addresses) {
            addr.setIsdefault("否");
            repository.save(addr);
        }
    }

    public long selectCount(Specification<PrAddress> spec) {
        return repository.count(spec);
    }

    private Specification<PrAddress> buildSpecification(PrAddress entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entity.getStoreId() != null) {
                predicates.add(cb.equal(root.get("storeId"), entity.getStoreId()));
            }
            if (entity.getUserid() != null) {
                predicates.add(cb.equal(root.get("userid"), entity.getUserid()));
            }
            if (entity.getName() != null && !entity.getName().isEmpty()) {
                predicates.add(cb.like(root.get("name"), "%" + entity.getName() + "%"));
            }
            if (entity.getPhone() != null && !entity.getPhone().isEmpty()) {
                predicates.add(cb.like(root.get("phone"), "%" + entity.getPhone() + "%"));
            }
            if (entity.getIsdefault() != null && !entity.getIsdefault().isEmpty()) {
                predicates.add(cb.equal(root.get("isdefault"), entity.getIsdefault()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}