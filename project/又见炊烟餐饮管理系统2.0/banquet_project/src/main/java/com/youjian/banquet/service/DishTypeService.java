package com.youjian.banquet.service;

import com.youjian.banquet.entity.DishType;
import com.youjian.banquet.repository.DishTypeRepository;
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
 * 菜品类型 Service
 */
@Service
public class DishTypeService {

    @Autowired
    private DishTypeRepository dishTypeRepo;

    private Specification<DishType> hasStoreId(Long storeId) {
        return (root, query, cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    public Page<DishType> page(int page, int size, String sortField, String sortOrder, Long storeId) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return dishTypeRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<DishType> page(int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return dishTypeRepo.findAll(hasStoreId(storeId), pageable);
    }

    public List<DishType> listAll(Long storeId) {
        return dishTypeRepo.findAll(hasStoreId(storeId));
    }

    public Optional<DishType> getById(Long id) {
        return dishTypeRepo.findById(id);
    }

    @Transactional
    public DishType save(DishType entity) {
        return dishTypeRepo.save(entity);
    }

    @Transactional
    public DishType update(DishType entity) {
        return dishTypeRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        dishTypeRepo.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        dishTypeRepo.deleteAllById(ids);
    }

    public long count(Long storeId) {
        return dishTypeRepo.count(hasStoreId(storeId));
    }
}