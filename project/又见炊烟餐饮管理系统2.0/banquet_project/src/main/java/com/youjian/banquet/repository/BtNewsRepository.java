package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BtNews;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 餐厅资讯 Repository
 */
@Repository
public interface BtNewsRepository extends JpaRepository<BtNews, Long>, JpaSpecificationExecutor<BtNews> {

    Page<BtNews> findByTitleContaining(String title, Pageable pageable);
}