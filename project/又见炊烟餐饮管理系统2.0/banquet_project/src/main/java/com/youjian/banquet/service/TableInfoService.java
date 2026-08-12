package com.youjian.banquet.service;

import com.youjian.banquet.entity.TableInfo;
import com.youjian.banquet.repository.TableInfoRepository;
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
 * 餐桌信息 Service
 */
@Service
public class TableInfoService {

    @Autowired
    private TableInfoRepository tableInfoRepo;

    private Specification<TableInfo> hasStoreId(Long storeId) {
        return (root, query, cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    public Page<TableInfo> page(int page, int size, String sortField, String sortOrder, Long storeId) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return tableInfoRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<TableInfo> page(int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return tableInfoRepo.findAll(hasStoreId(storeId), pageable);
    }

    public List<TableInfo> listAll(Long storeId) {
        return tableInfoRepo.findAll(hasStoreId(storeId));
    }

    public Optional<TableInfo> getById(Long id) {
        return tableInfoRepo.findById(id);
    }

    @Transactional
    public TableInfo save(TableInfo entity) {
        return tableInfoRepo.save(entity);
    }

    @Transactional
    public TableInfo update(TableInfo entity) {
        return tableInfoRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        tableInfoRepo.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        tableInfoRepo.deleteAllById(ids);
    }

    public long count(Long storeId) {
        return tableInfoRepo.count(hasStoreId(storeId));
    }
}