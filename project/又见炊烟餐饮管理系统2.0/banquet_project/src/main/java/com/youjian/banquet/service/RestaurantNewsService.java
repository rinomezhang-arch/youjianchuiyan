package com.youjian.banquet.service;

import com.youjian.banquet.entity.RestaurantNews;
import com.youjian.banquet.repository.RestaurantNewsRepository;
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
 */
@Service
public class RestaurantNewsService {

    @Autowired
    private RestaurantNewsRepository restaurantNewsRepo;

    private Specification<RestaurantNews> hasStoreId(Long storeId) {
        return (root, query, cb) -> {
            if (storeId != null) {
                return cb.equal(root.get("storeId"), storeId);
            }
            return null;
        };
    }

    public Page<RestaurantNews> page(int page, int size, String sortField, String sortOrder, Long storeId) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return restaurantNewsRepo.findAll(hasStoreId(storeId), pageable);
    }

    public Page<RestaurantNews> page(int page, int size, Long storeId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return restaurantNewsRepo.findAll(hasStoreId(storeId), pageable);
    }

    public List<RestaurantNews> listAll(Long storeId) {
        return restaurantNewsRepo.findAll(hasStoreId(storeId));
    }

    public Optional<RestaurantNews> getById(Long id) {
        return restaurantNewsRepo.findById(id);
    }

    @Transactional
    public RestaurantNews save(RestaurantNews entity) {
        return restaurantNewsRepo.save(entity);
    }

    @Transactional
    public RestaurantNews update(RestaurantNews entity) {
        return restaurantNewsRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        restaurantNewsRepo.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        restaurantNewsRepo.deleteAllById(ids);
    }

    public long count(Long storeId) {
        return restaurantNewsRepo.count(hasStoreId(storeId));
    }
}