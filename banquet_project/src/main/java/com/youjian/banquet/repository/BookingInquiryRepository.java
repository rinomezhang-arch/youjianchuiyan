package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BookingInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingInquiryRepository extends JpaRepository<BookingInquiry, Long> {
    List<BookingInquiry> findAllByOrderByCreatedAtDesc();
    List<BookingInquiry> findByStoreIdOrderByCreatedAtDesc(Long storeId);
}
