package com.youjian.banquet.service;

import com.youjian.banquet.entity.BtAddress;
import com.youjian.banquet.repository.BtAddressRepository;
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
 * 地址 Service
 * 来源：点餐系统 address Service
 */
@Service
public class BtAddressService {

    @Autowired
    private BtAddressRepository btAddressRepo;

    private Specification<BtAddress> hasStoreId(Long storeId) {
        return (root, query, cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    public Page<BtAddress> page(int page, int size, String sortField, String sortOrder, Long storeId) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return btAddressRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtAddress> page(int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btAddressRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtAddress> pageByUser(Long userid, int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btAddressRepo.findByUserid(userid, pageable);
    }

    public Optional<BtAddress> getById(Long id) {
        return btAddressRepo.findById(id);
    }

    public Optional<BtAddress> getDefaultAddress(Long userid) {
        return btAddressRepo.findByUseridAndIsdefault(userid, "是");
    }

    @Transactional
    public BtAddress save(BtAddress entity) {
        if ("是".equals(entity.getIsdefault())) {
            btAddressRepo.clearDefaultByUserid(entity.getUserid());
        }
        return btAddressRepo.save(entity);
    }

    @Transactional
    public BtAddress update(BtAddress entity) {
        if ("是".equals(entity.getIsdefault())) {
            btAddressRepo.clearDefaultByUserid(entity.getUserid());
        }
        return btAddressRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        btAddressRepo.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        btAddressRepo.deleteAllById(ids);
    }

    public long count(Long storeId) {
        return btAddressRepo.count(hasStoreId(storeId));
    }
}