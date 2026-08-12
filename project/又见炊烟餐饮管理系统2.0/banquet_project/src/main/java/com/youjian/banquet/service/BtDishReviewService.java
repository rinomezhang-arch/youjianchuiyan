package com.youjian.banquet.service;

import com.youjian.banquet.entity.BtDishReview;
import com.youjian.banquet.repository.BtDishReviewRepository;
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
 * 菜品评论 Service
 * 来源：点餐系统 discusscaipinxinxi Service
 */
@Service
public class BtDishReviewService {

    @Autowired
    private BtDishReviewRepository btDishReviewRepo;

    private Specification<BtDishReview> hasStoreId(Long storeId) {
        return (root, query, cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    public Page<BtDishReview> page(int page, int size, String sortField, String sortOrder, Long storeId) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return btDishReviewRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtDishReview> page(int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btDishReviewRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtDishReview> pageByDish(Long refid, int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("addtime").descending());
        return btDishReviewRepo.findByRefid(refid, pageable);
    }

    public List<BtDishReview> listByDish(Long refid) {
        return btDishReviewRepo.findByRefidOrderByAddtimeDesc(refid);
    }

    public Optional<BtDishReview> getById(Long id) {
        return btDishReviewRepo.findById(id);
    }

    @Transactional
    public BtDishReview save(BtDishReview entity) {
        return btDishReviewRepo.save(entity);
    }

    @Transactional
    public BtDishReview update(BtDishReview entity) {
        return btDishReviewRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        btDishReviewRepo.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        btDishReviewRepo.deleteAllById(ids);
    }

    public long count(Long storeId) {
        return btDishReviewRepo.count(hasStoreId(storeId));
    }
}