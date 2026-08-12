package com.youjian.banquet.repository;

import com.youjian.banquet.entity.RestaurantNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 餐厅资讯 Repository
 */
@Repository
public interface RestaurantNewsRepository extends JpaRepository<RestaurantNews, Long>, JpaSpecificationExecutor<RestaurantNews> {
}