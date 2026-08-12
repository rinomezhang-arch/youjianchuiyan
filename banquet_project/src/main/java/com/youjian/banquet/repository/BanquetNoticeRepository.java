package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BanquetNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BanquetNoticeRepository extends JpaRepository<BanquetNotice, Long> {
    Optional<BanquetNotice> findByIdAndStoreId(Long id, Long storeId);

    @Query("select n from BanquetNotice n where n.storeId = :storeId " +
            "and (:keyword is null or :keyword = '' or lower(n.noticeNo) like lower(concat('%', :keyword, '%')) " +
            "or lower(coalesce(n.customerName, '')) like lower(concat('%', :keyword, '%')) " +
            "or lower(n.location) like lower(concat('%', :keyword, '%'))) " +
            "and (:status is null or :status = '' or n.status = :status) " +
            "and (:startDate is null or n.banquetDate >= :startDate) " +
            "and (:endDate is null or n.banquetDate <= :endDate) order by n.banquetDate desc, n.id desc")
    List<BanquetNotice> search(@Param("storeId") Long storeId,
                               @Param("keyword") String keyword,
                               @Param("status") String status,
                               @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate);
}
