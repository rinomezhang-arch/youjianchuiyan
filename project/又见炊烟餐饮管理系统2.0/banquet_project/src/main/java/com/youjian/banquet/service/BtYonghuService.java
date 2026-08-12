package com.youjian.banquet.service;

import com.youjian.banquet.entity.BtYonghu;
import com.youjian.banquet.repository.BtYonghuRepository;
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
 * 普通用户 Service
 * 来源：点餐系统 yonghu Service
 */
@Service
public class BtYonghuService {

    @Autowired
    private BtYonghuRepository btYonghuRepo;

    private Specification<BtYonghu> hasStoreId(Long storeId) {
        return (root, query, cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    public Page<BtYonghu> page(int page, int size, String sortField, String sortOrder, Long storeId) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return btYonghuRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtYonghu> page(int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btYonghuRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtYonghu> search(String yonghuming, int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btYonghuRepo.findByYonghumingContaining(yonghuming, pageable);
    }

    public List<BtYonghu> listAll(Long storeId) {
        return btYonghuRepo.findAll(hasStoreId(storeId));
    }

    public Optional<BtYonghu> getById(Long id) {
        return btYonghuRepo.findById(id);
    }

    public Optional<BtYonghu> getByUsername(String yonghuming) {
        return btYonghuRepo.findByYonghuming(yonghuming);
    }

    public boolean existsByUsername(String yonghuming) {
        return btYonghuRepo.existsByYonghuming(yonghuming);
    }

    @Transactional
    public BtYonghu save(BtYonghu entity) {
        return btYonghuRepo.save(entity);
    }

    @Transactional
    public BtYonghu update(BtYonghu entity) {
        return btYonghuRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        btYonghuRepo.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        btYonghuRepo.deleteAllById(ids);
    }

    /**
     * 登录验证
     */
    public BtYonghu login(String yonghuming, String mima) {
        Optional<BtYonghu> userOpt = btYonghuRepo.findByYonghuming(yonghuming);
        if (userOpt.isPresent() && userOpt.get().getMima().equals(mima)) {
            return userOpt.get();
        }
        return null;
    }

    /**
     * 注册
     */
    @Transactional
    public BtYonghu register(BtYonghu entity) {
        if (btYonghuRepo.existsByYonghuming(entity.getYonghuming())) {
            return null;
        }
        return btYonghuRepo.save(entity);
    }

    /**
     * 密码重置
     */
    @Transactional
    public boolean resetPassword(String yonghuming) {
        Optional<BtYonghu> userOpt = btYonghuRepo.findByYonghuming(yonghuming);
        if (userOpt.isPresent()) {
            BtYonghu user = userOpt.get();
            user.setMima("123456");
            btYonghuRepo.save(user);
            return true;
        }
        return false;
    }
}