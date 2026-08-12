package com.youjian.banquet.service;

import com.youjian.banquet.entity.TableUsage;
import com.youjian.banquet.repository.TableUsageRepository;
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
 * 餐桌使用记录 Service
 */
@Service
public class TableUsageService {

    @Autowired
    private TableUsageRepository tableUsageRepo;

    private Specification<TableUsage> hasStoreId(Long storeId) {
        return (root, query, cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    public Page<TableUsage> page(int page, int size, String sortField, String sortOrder, Long storeId) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return tableUsageRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<TableUsage> page(int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return tableUsageRepo.findAll(hasStoreId(storeId), pageable);
    }

    public List<TableUsage> listAll(Long storeId) {
        return tableUsageRepo.findAll(hasStoreId(storeId));
    }

    public Optional<TableUsage> getById(Long id) {
        return tableUsageRepo.findById(id);
    }

    @Transactional
    public TableUsage save(TableUsage entity) {
        return tableUsageRepo.save(entity);
    }

    @Transactional
    public TableUsage update(TableUsage entity) {
        return tableUsageRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        tableUsageRepo.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        tableUsageRepo.deleteAllById(ids);
    }

    public long count(Long storeId) {
        return tableUsageRepo.count(hasStoreId(storeId));
    }
}