package com.youjian.banquet.service;

import com.youjian.banquet.entity.UserAddress;
import com.youjian.banquet.repository.UserAddressRepository;
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
 * 用户地址 Service
 */
@Service
public class UserAddressService {

    @Autowired
    private UserAddressRepository userAddressRepo;

    private Specification<UserAddress> hasStoreId(Long storeId) {
        return (root, query, cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    public Page<UserAddress> page(int page, int size, String sortField, String sortOrder, Long storeId) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return userAddressRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<UserAddress> page(int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return userAddressRepo.findAll(hasStoreId(storeId), pageable);
    }

    public List<UserAddress> listAll(Long storeId) {
        return userAddressRepo.findAll(hasStoreId(storeId));
    }

    public Optional<UserAddress> getById(Long id) {
        return userAddressRepo.findById(id);
    }

    @Transactional
    public UserAddress save(UserAddress entity) {
        return userAddressRepo.save(entity);
    }

    @Transactional
    public UserAddress update(UserAddress entity) {
        return userAddressRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        userAddressRepo.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        userAddressRepo.deleteAllById(ids);
    }

    public long count(Long storeId) {
        return userAddressRepo.count(hasStoreId(storeId));
    }
}