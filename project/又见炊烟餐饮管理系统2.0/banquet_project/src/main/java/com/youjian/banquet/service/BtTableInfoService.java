package com.youjian.banquet.service;

import com.youjian.banquet.entity.BtTableInfo;
import com.youjian.banquet.repository.BtTableInfoRepository;
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
 * 餐桌信息 Service
 * 来源：点餐系统 canzhuoxinxi Service
 */
@Service
public class BtTableInfoService {

    @Autowired
    private BtTableInfoRepository btTableInfoRepo;

    private Specification<BtTableInfo> hasStoreId(Long storeId) {
        return (root, query, cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    public Page<BtTableInfo> page(int page, int size, String sortField, String sortOrder, Long storeId) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return btTableInfoRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtTableInfo> page(int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btTableInfoRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtTableInfo> pageByStatus(String status, int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btTableInfoRepo.findByCanzhuozhuangtai(status, pageable);
    }

    public List<BtTableInfo> listAll(Long storeId) {
        return btTableInfoRepo.findAll(hasStoreId(storeId));
    }

    public Optional<BtTableInfo> getById(Long id) {
        return btTableInfoRepo.findById(id);
    }

    public BtTableInfo getByNumber(String number) {
        return btTableInfoRepo.findByCanzhuohaoma(number);
    }

    @Transactional
    public BtTableInfo save(BtTableInfo entity) {
        return btTableInfoRepo.save(entity);
    }

    @Transactional
    public BtTableInfo update(BtTableInfo entity) {
        return btTableInfoRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        btTableInfoRepo.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        btTableInfoRepo.deleteAllById(ids);
    }

    public long count(Long storeId) {
        return btTableInfoRepo.count(hasStoreId(storeId));
    }
}