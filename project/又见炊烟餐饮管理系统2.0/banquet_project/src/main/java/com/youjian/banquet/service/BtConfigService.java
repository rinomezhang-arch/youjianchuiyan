package com.youjian.banquet.service;

import com.youjian.banquet.entity.BtConfig;
import com.youjian.banquet.repository.BtConfigRepository;
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
 * 配置 Service
 * 来源：点餐系统 config Service
 */
@Service
public class BtConfigService {

    @Autowired
    private BtConfigRepository btConfigRepo;

    private Specification<BtConfig> hasStoreId(Long storeId) {
        return (root, query, cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    public Page<BtConfig> page(int page, int size, String sortField, String sortOrder, Long storeId) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return btConfigRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtConfig> page(int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btConfigRepo.findAll(hasStoreId(storeId), pageable);
    }

    public List<BtConfig> listAll(Long storeId) {
        return btConfigRepo.findAll(hasStoreId(storeId));
    }

    public Optional<BtConfig> getById(Long id) {
        return btConfigRepo.findById(id);
    }

    public Optional<BtConfig> getByName(String name) {
        return btConfigRepo.findByName(name);
    }

    @Transactional
    public BtConfig save(BtConfig entity) {
        return btConfigRepo.save(entity);
    }

    @Transactional
    public BtConfig update(BtConfig entity) {
        return btConfigRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        btConfigRepo.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        btConfigRepo.deleteAllById(ids);
    }
}