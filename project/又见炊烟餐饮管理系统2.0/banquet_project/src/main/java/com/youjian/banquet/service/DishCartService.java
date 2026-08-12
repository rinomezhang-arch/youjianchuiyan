package com.youjian.banquet.service;

import com.youjian.banquet.entity.DishCart;
import com.youjian.banquet.repository.DishCartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 购物车 Service
 */
@Service
public class DishCartService {

    @Autowired
    private DishCartRepository dishCartRepo;

    private Specification<DishCart> hasStoreId(Long storeId) {
        return (root, query, cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    public Page<DishCart> page(int page, int size, String sortField, String sortOrder, Long storeId) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return dishCartRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<DishCart> page(int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return dishCartRepo.findAll(hasStoreId(storeId), pageable);
    }

    public List<DishCart> listAll(Long storeId) {
        return dishCartRepo.findAll(hasStoreId(storeId));
    }

    public Optional<DishCart> getById(Long id) {
        return dishCartRepo.findById(id);
    }

    @Transactional
    public DishCart save(DishCart entity) {
        return dishCartRepo.save(entity);
    }

    @Transactional
    public DishCart update(DishCart entity) {
        return dishCartRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        dishCartRepo.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        dishCartRepo.deleteAllById(ids);
    }

    public long count(Long storeId) {
        return dishCartRepo.count(hasStoreId(storeId));
    }
}