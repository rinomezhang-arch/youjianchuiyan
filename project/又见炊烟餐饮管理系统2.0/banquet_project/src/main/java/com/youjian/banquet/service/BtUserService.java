package com.youjian.banquet.service;

import com.youjian.banquet.entity.BtUser;
import com.youjian.banquet.repository.BtUserRepository;
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
 * 管理员用户 Service
 * 来源：点餐系统 users Service
 */
@Service
public class BtUserService {

    @Autowired
    private BtUserRepository btUserRepo;

    private Specification<BtUser> hasStoreId(Long storeId) {
        return (root, query, cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    public Page<BtUser> page(int page, int size, String sortField, String sortOrder, Long storeId) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return btUserRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtUser> page(int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btUserRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Optional<BtUser> getById(Long id) {
        return btUserRepo.findById(id);
    }

    public Optional<BtUser> getByUsername(String username) {
        return btUserRepo.findByUsername(username);
    }

    public boolean existsByUsername(String username) {
        return btUserRepo.existsByUsername(username);
    }

    @Transactional
    public BtUser save(BtUser entity) {
        return btUserRepo.save(entity);
    }

    @Transactional
    public BtUser update(BtUser entity) {
        return btUserRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        btUserRepo.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        btUserRepo.deleteAllById(ids);
    }

    /**
     * 登录验证
     */
    public BtUser login(String username, String password) {
        Optional<BtUser> userOpt = btUserRepo.findByUsername(username);
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            return userOpt.get();
        }
        return null;
    }

    /**
     * 密码重置
     */
    @Transactional
    public boolean resetPassword(String username) {
        Optional<BtUser> userOpt = btUserRepo.findByUsername(username);
        if (userOpt.isPresent()) {
            BtUser user = userOpt.get();
            user.setPassword("123456");
            btUserRepo.save(user);
            return true;
        }
        return false;
    }
}