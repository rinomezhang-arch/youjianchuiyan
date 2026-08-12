package com.youjian.banquet.service;

import com.youjian.banquet.entity.BtStoreup;
import com.youjian.banquet.repository.BtStoreupRepository;
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
 * 收藏 Service
 * 来源：点餐系统 storeup Service
 */
@Service
public class BtStoreupService {

    @Autowired
    private BtStoreupRepository btStoreupRepo;

    private Specification<BtStoreup> hasStoreId(Long storeId) {
        return (root, query, cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    public Page<BtStoreup> page(int page, int size, String sortField, String sortOrder, Long storeId) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return btStoreupRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtStoreup> page(int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btStoreupRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtStoreup> pageByUser(Long userid, int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btStoreupRepo.findByUserid(userid, pageable);
    }

    public List<BtStoreup> listByUser(Long userid) {
        return btStoreupRepo.findByUserid(userid);
    }

    public Optional<BtStoreup> getById(Long id) {
        return btStoreupRepo.findById(id);
    }

    public Optional<BtStoreup> getByUserAndRefidAndType(Long userid, Long refid, String type) {
        return btStoreupRepo.findByUseridAndRefidAndType(userid, refid, type);
    }

    @Transactional
    public BtStoreup save(BtStoreup entity) {
        return btStoreupRepo.save(entity);
    }

    @Transactional
    public BtStoreup update(BtStoreup entity) {
        return btStoreupRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        btStoreupRepo.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        btStoreupRepo.deleteAllById(ids);
    }

    public long count(Long storeId) {
        return btStoreupRepo.count(hasStoreId(storeId));
    }
}