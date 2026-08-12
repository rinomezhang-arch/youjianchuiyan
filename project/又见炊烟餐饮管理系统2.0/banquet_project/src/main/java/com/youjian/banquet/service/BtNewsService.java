package com.youjian.banquet.service;

import com.youjian.banquet.entity.BtNews;
import com.youjian.banquet.repository.BtNewsRepository;
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
 * 餐厅资讯 Service
 * 来源：点餐系统 news Service
 */
@Service
public class BtNewsService {

    @Autowired
    private BtNewsRepository btNewsRepo;

    private Specification<BtNews> hasStoreId(Long storeId) {
        return (root, query, cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    public Page<BtNews> page(int page, int size, String sortField, String sortOrder, Long storeId) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return btNewsRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtNews> page(int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btNewsRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<BtNews> search(String title, int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return btNewsRepo.findByTitleContaining(title, pageable);
    }

    public List<BtNews> listAll(Long storeId) {
        return btNewsRepo.findAll(hasStoreId(storeId));
    }

    public Optional<BtNews> getById(Long id) {
        return btNewsRepo.findById(id);
    }

    @Transactional
    public BtNews save(BtNews entity) {
        return btNewsRepo.save(entity);
    }

    @Transactional
    public BtNews update(BtNews entity) {
        return btNewsRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        btNewsRepo.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        btNewsRepo.deleteAllById(ids);
    }

    public long count(Long storeId) {
        return btNewsRepo.count(hasStoreId(storeId));
    }
}