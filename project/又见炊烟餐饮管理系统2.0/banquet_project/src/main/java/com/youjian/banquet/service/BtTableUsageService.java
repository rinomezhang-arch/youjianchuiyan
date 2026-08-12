package com.youjian.banquet.service;

import com.youjian.banquet.entity.BtTableUsage;
import com.youjian.banquet.repository.BtTableUsageRepository;
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
 * 餐桌使用 Service
 * 来源：点餐系统 canzhuoshiyong Service
 */
@Service
public class BtTableUsageService {

    @Autowired
    private BtTableUsageRepository btTableUsageRepo;

    private Specification<BtTableUsage> hasStoreId(Long storeId) {
        return (root, query, cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    public Page<BtTableUsage> page(int page, int size, String sortField, String sortOrder, Long storeId) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return btTableUsageRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtTableUsage> page(int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btTableUsageRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtTableUsage> pageByUser(String yonghuming, int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btTableUsageRepo.findByYonghuming(yonghuming, pageable);
    }

    public List<BtTableUsage> listAll(Long storeId) {
        return btTableUsageRepo.findAll(hasStoreId(storeId));
    }

    public List<BtTableUsage> listByUser(String yonghuming) {
        return btTableUsageRepo.findByYonghuming(yonghuming);
    }

    public Optional<BtTableUsage> getById(Long id) {
        return btTableUsageRepo.findById(id);
    }

    @Transactional
    public BtTableUsage save(BtTableUsage entity) {
        return btTableUsageRepo.save(entity);
    }

    @Transactional
    public BtTableUsage update(BtTableUsage entity) {
        return btTableUsageRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        btTableUsageRepo.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        btTableUsageRepo.deleteAllById(ids);
    }

    public long count(Long storeId) {
        return btTableUsageRepo.count(hasStoreId(storeId));
    }
}