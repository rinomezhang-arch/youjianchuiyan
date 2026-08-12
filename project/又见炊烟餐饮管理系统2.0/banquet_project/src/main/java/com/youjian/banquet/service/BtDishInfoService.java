package com.youjian.banquet.service;

import com.youjian.banquet.entity.BtDishInfo;
import com.youjian.banquet.repository.BtDishInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 菜品信息 Service
 * 来源：点餐系统 caipinxinxi Service
 */
@Service
public class BtDishInfoService {

    @Autowired
    private BtDishInfoRepository btDishInfoRepo;

    private Specification<BtDishInfo> hasStoreId(Long storeId) {
        return (root, query, cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    public Page<BtDishInfo> page(int page, int size, String sortField, String sortOrder, Long storeId) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return btDishInfoRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtDishInfo> page(int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btDishInfoRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtDishInfo> pageByType(String caipinleixing, int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btDishInfoRepo.findByCaipinleixing(caipinleixing, pageable);
    }

    public Page<BtDishInfo> search(String caipinmingcheng, String caipinleixing, int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btDishInfoRepo.search(caipinmingcheng, caipinleixing, pageable);
    }

    public Page<BtDishInfo> searchWithPrice(String caipinmingcheng, String caipinleixing,
                                            Double pricestart, Double priceend,
                                            int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btDishInfoRepo.searchWithPrice(caipinmingcheng, caipinleixing, pricestart, priceend, pageable);
    }

    public List<BtDishInfo> listAll(Long storeId) {
        return btDishInfoRepo.findAll(hasStoreId(storeId));
    }

    public Optional<BtDishInfo> getById(Long id) {
        Optional<BtDishInfo> opt = btDishInfoRepo.findById(id);
        opt.ifPresent(dish -> {
            dish.setClicktime(LocalDateTime.now());
            btDishInfoRepo.save(dish);
        });
        return opt;
    }

    public List<BtDishInfo> getTopByClickTime(int limit) {
        return btDishInfoRepo.findByOrderByClicktimeDesc(PageRequest.of(0, limit));
    }

    public List<BtDishInfo> getByTypeOrderByClickTime(String caipinleixing, int limit) {
        return btDishInfoRepo.findByCaipinleixingOrderByClicktimeDesc(caipinleixing, PageRequest.of(0, limit));
    }

    @Transactional
    public BtDishInfo save(BtDishInfo entity) {
        return btDishInfoRepo.save(entity);
    }

    @Transactional
    public BtDishInfo update(BtDishInfo entity) {
        return btDishInfoRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        btDishInfoRepo.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        btDishInfoRepo.deleteAllById(ids);
    }

    public long count(Long storeId) {
        return btDishInfoRepo.count(hasStoreId(storeId));
    }
}