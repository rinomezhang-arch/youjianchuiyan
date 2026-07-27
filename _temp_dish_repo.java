package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BookingDishDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingDishDetailRepository extends JpaRepository<BookingDishDetail, Integer> {
    List<BookingDishDetail> findByBookingIdAndStoreId(String bookingId, Long storeId);
    void deleteByBookingIdAndStoreId(String bookingId, Long storeId);
    List<BookingDishDetail> findByBookingId(String bookingId);
    void deleteByBookingId(String bookingId);
}
