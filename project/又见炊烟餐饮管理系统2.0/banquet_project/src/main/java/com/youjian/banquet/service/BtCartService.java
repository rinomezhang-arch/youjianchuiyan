package com.youjian.banquet.service;

import com.youjian.banquet.entity.BtCart;
import com.youjian.banquet.repository.BtCartRepository;
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
 * 来源：点餐系统 cart Service
 */
@Service
public class BtCartService {

    @Autowired
    private BtCartRepository btCartRepo;

    private Specification<BtCart> hasStoreId(Long storeId) {
        return (root, query, cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    public Page<BtCart> page(int page, int size, String sortField, String sortOrder, Long storeId) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return btCartRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtCart> page(int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btCartRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtCart> pageByUser(Long userid, int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btCartRepo.findByUserid(userid, pageable);
    }

    public List<BtCart> listByUser(Long userid) {
        return btCartRepo.findByUserid(userid);
    }

    public List<BtCart> listByUserAndGood(Long userid, Long goodid) {
        return btCartRepo.findByUseridAndGoodid(userid, goodid);
    }

    public Optional<BtCart> getById(Long id) {
        return btCartRepo.findById(id);
    }

    @Transactional
    public BtCart save(BtCart entity) {
        return btCartRepo.save(entity);
    }

    @Transactional
    public BtCart update(BtCart entity) {
        return btCartRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        btCartRepo.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        btCartRepo.deleteAllById(ids);
    }

    public long count(Long storeId) {
        return btCartRepo.count(hasStoreId(storeId));
    }
}