package com.youjian.banquet.service;

import com.youjian.banquet.entity.BtOrder;
import com.youjian.banquet.repository.BtOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 订单 Service
 * 来源：点餐系统 orders Service
 */
@Service
public class BtOrderService {

    @Autowired
    private BtOrderRepository btOrderRepo;

    private Specification<BtOrder> hasStoreId(Long storeId) {
        return (root, query, cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    public Page<BtOrder> page(int page, int size, String sortField, String sortOrder, Long storeId) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return btOrderRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtOrder> page(int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btOrderRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtOrder> pageByUser(Long userid, int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btOrderRepo.findByUserid(userid, pageable);
    }

    public Page<BtOrder> pageByStatus(String status, int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btOrderRepo.findByStatus(status, pageable);
    }

    public List<BtOrder> listByUser(Long userid) {
        return btOrderRepo.findByUseridAndTablenameOrderByAddtimeDesc(userid, "caipinxinxi");
    }

    public List<BtOrder> listByUserAndStatuses(Long userid, List<String> statuses) {
        return btOrderRepo.findByUseridAndStatusIn(userid, statuses);
    }

    public Optional<BtOrder> getById(Long id) {
        return btOrderRepo.findById(id);
    }

    public BtOrder getByOrderid(String orderid) {
        return btOrderRepo.findByOrderid(orderid);
    }

    @Transactional
    public BtOrder save(BtOrder entity) {
        if (entity.getOrderid() == null || entity.getOrderid().isEmpty()) {
            entity.setOrderid(String.valueOf(System.currentTimeMillis()));
        }
        if (entity.getTotal() == null) {
            entity.setTotal(0f);
        }
        return btOrderRepo.save(entity);
    }

    @Transactional
    public BtOrder update(BtOrder entity) {
        return btOrderRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        btOrderRepo.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        btOrderRepo.deleteAllById(ids);
    }

    public long count(Long storeId) {
        return btOrderRepo.count(hasStoreId(storeId));
    }

    public List<Map<String, Object>> selectValue(String xColumn, String yColumn) {
        if ("total".equalsIgnoreCase(yColumn)) {
            return btOrderRepo.selectDailyTotalValue();
        }
        return btOrderRepo.selectDailyTotalValue();
    }

    public List<Map<String, Object>> selectTimeStatValue(String xColumn, String yColumn, String timeStatType) {
        return btOrderRepo.selectDailyTotalValue();
    }

    public List<Map<String, Object>> selectGroup(String columnName) {
        if ("goodtype".equalsIgnoreCase(columnName)) {
            return btOrderRepo.selectGroupByGoodtype();
        }
        if ("status".equalsIgnoreCase(columnName)) {
            return btOrderRepo.selectGroupByStatus();
        }
        return btOrderRepo.selectGroupByGoodtype();
    }
}