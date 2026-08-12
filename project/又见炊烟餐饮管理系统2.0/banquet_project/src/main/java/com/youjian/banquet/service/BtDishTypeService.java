package com.youjian.banquet.service;

import com.youjian.banquet.entity.BtDishType;
import com.youjian.banquet.repository.BtDishTypeRepository;
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
 * 菜品类型 Service
 * 来源：点餐系统 caipinleixing Service
 */
@Service
public class BtDishTypeService {

    @Autowired
    private BtDishTypeRepository btDishTypeRepo;

    private Specification<BtDishType> hasStoreId(Long storeId) {
        return (root, query, cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    public Page<BtDishType> page(int page, int size, String sortField, String sortOrder, Long storeId) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return btDishTypeRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtDishType> page(int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btDishTypeRepo.findAll(hasStoreId(storeId), pageable);
    }

    public List<BtDishType> listAll(Long storeId) {
        return btDishTypeRepo.findAll(hasStoreId(storeId));
    }

    public Optional<BtDishType> getById(Long id) {
        return btDishTypeRepo.findById(id);
    }

    @Transactional
    public BtDishType save(BtDishType entity) {
        return btDishTypeRepo.save(entity);
    }

    @Transactional
    public BtDishType update(BtDishType entity) {
        return btDishTypeRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        btDishTypeRepo.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        btDishTypeRepo.deleteAllById(ids);
    }

    public long count(Long storeId) {
        return btDishTypeRepo.count(hasStoreId(storeId));
    }
}