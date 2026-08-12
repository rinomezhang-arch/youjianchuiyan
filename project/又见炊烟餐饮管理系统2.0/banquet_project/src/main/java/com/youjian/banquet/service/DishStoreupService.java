package com.youjian.banquet.service;

import com.youjian.banquet.entity.DishStoreup;
import com.youjian.banquet.repository.DishStoreupRepository;
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
 * 收藏 Service
 */
@Service
public class DishStoreupService {

    @Autowired
    private DishStoreupRepository dishStoreupRepo;

    private Specification<DishStoreup> hasStoreId(Long storeId) {
        return (root, query, cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    public Page<DishStoreup> page(int page, int size, String sortField, String sortOrder, Long storeId) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return dishStoreupRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<DishStoreup> page(int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return dishStoreupRepo.findAll(hasStoreId(storeId), pageable);
    }

    public List<DishStoreup> listAll(Long storeId) {
        return dishStoreupRepo.findAll(hasStoreId(storeId));
    }

    public Optional<DishStoreup> getById(Long id) {
        return dishStoreupRepo.findById(id);
    }

    @Transactional
    public DishStoreup save(DishStoreup entity) {
        return dishStoreupRepo.save(entity);
    }

    @Transactional
    public DishStoreup update(DishStoreup entity) {
        return dishStoreupRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        dishStoreupRepo.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        dishStoreupRepo.deleteAllById(ids);
    }

    public long count(Long storeId) {
        return dishStoreupRepo.count(hasStoreId(storeId));
    }
}