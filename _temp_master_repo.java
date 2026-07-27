package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BookingMaster;
import com.youjian.banquet.entity.BookingMasterId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface BookingMasterRepository extends JpaRepository<BookingMaster, BookingMasterId> {
    List<BookingMaster> findByBookingDateAndStoreIdOrderByBookingTime(LocalDate date, Long storeId);
    List<BookingMaster> findByStoreIdOrderByBookingDateDesc(Long storeId);
    BookingMaster findByBookingNo(String bookingNo);
    List<BookingMaster> findByCustomerPhoneAndStoreId(String customerPhone, Long storeId);
    int countByCustomerPhoneAndStoreId(String customerPhone, Long storeId);
    
    @Query("SELECT b FROM BookingMaster b WHERE b.storeId = ?1 AND (b.customerName LIKE %?2% OR b.customerPhone LIKE %?2%)")
    List<BookingMaster> search(Long storeId, String keyword);
    
    List<BookingMaster> findByStoreIdAndBookingDate(Long storeId, LocalDate date);
    
    @Query("SELECT b FROM BookingMaster b WHERE b.storeId = ?1 AND b.bookingDate BETWEEN ?2 AND ?3")
    List<BookingMaster> findByDateRange(Long storeId, LocalDate start, LocalDate end);
    
    List<BookingMaster> findByStoreIdAndBookingStatus(Long storeId, String status);
}
