package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BookingTable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface BookingTableRepository extends JpaRepository<BookingTable, Long> {
    List<BookingTable> findByBookingId(String bookingId);
    void deleteByBookingId(String bookingId);
    List<BookingTable> findByBookingIdAndStoreId(String bookingId, Long storeId);
}
