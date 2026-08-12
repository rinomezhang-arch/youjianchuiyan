package com.youjian.banquet.service;

import com.youjian.banquet.entity.DishOrder;
import com.youjian.banquet.repository.DishOrderRepository;
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
 * 订单 Service
 */
@Service
public class DishOrderService {

    @Autowired
    private DishOrderRepository dishOrderRepo;

    private Specification<DishOrder> hasStoreId(Long storeId) {
        return (root, query, cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    public Page<DishOrder> page(int page, int size, String sortField, String sortOrder, Long storeId) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return dishOrderRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<DishOrder> page(int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return dishOrderRepo.findAll(hasStoreId(storeId), pageable);
    }

    public List<DishOrder> listAll(Long storeId) {
        return dishOrderRepo.findAll(hasStoreId(storeId));
    }

    public Optional<DishOrder> getById(Long id) {
        return dishOrderRepo.findById(id);
    }

    @Transactional
    public DishOrder save(DishOrder entity) {
        return dishOrderRepo.save(entity);
    }

    @Transactional
    public DishOrder update(DishOrder entity) {
        return dishOrderRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        dishOrderRepo.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        dishOrderRepo.deleteAllById(ids);
    }

    public long count(Long storeId) {
        return dishOrderRepo.count(hasStoreId(storeId));
    }
}