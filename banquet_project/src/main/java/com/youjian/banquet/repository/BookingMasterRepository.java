package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BookingMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingMasterRepository extends JpaRepository<BookingMaster, Long>, JpaSpecificationExecutor<BookingMaster> {

    void deleteByBookingIdAndStoreId(String bookingId, Long storeId);

    List<BookingMaster> findByStoreId(Long storeId);

    List<BookingMaster> findByStoreIdOrderByBookingDateDesc(Long storeId);

    List<BookingMaster> findByStoreIdAndBookingStatus(Long storeId, String bookingStatus);

    Optional<BookingMaster> findByBookingIdAndStoreId(String bookingId, Long storeId);

    List<BookingMaster> findByStoreIdAndBookingDate(Long storeId, LocalDate bookingDate);

    List<BookingMaster> findByStoreIdAndBookingDateBetween(Long storeId, LocalDate start, LocalDate end);

    List<BookingMaster> findByCustomerPhoneAndStoreId(String customerPhone, Long storeId);

    List<BookingMaster> findByStoreIdAndCustomerId(Long storeId, Integer customerId);

    List<BookingMaster> findByStoreIdAndBookingIdStartingWith(Long storeId, String prefix);

    @Query("SELECT b FROM BookingMaster b WHERE b.storeId = :storeId AND " +
           "(b.customerName LIKE %:keyword% OR b.customerPhone LIKE %:keyword% OR b.bookingId LIKE %:keyword%)")
    List<BookingMaster> searchByKeyword(@Param("storeId") Long storeId, @Param("keyword") String keyword);

    @Query("SELECT COUNT(b) FROM BookingMaster b WHERE b.storeId = :storeId AND b.bookingDate = :date AND b.bookingStatus = :status")
    long countByStoreIdAndBookingDateAndBookingStatus(@Param("storeId") Long storeId, @Param("date") LocalDate date, @Param("status") String status);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM BookingMaster b WHERE b.storeId = :storeId AND b.bookingDate = :date AND b.bookingStatus = :status")
    java.math.BigDecimal sumTotalAmountByStoreIdAndBookingDateAndBookingStatus(@Param("storeId") Long storeId, @Param("date") LocalDate date, @Param("status") String status);

    /** 全门店聚合查询（总经理视图） */
    List<BookingMaster> findByBookingDate(LocalDate bookingDate);

    List<BookingMaster> findByBookingDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT COUNT(b) FROM BookingMaster b WHERE b.bookingDate = :date AND b.bookingStatus = :status")
    long countByBookingDateAndBookingStatus(@Param("date") LocalDate date, @Param("status") String status);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM BookingMaster b WHERE b.bookingDate = :date AND b.bookingStatus = :status")
    java.math.BigDecimal sumTotalAmountByBookingDateAndBookingStatus(@Param("date") LocalDate date, @Param("status") String status);

    /** 全店聚合：按日期+状态求 guestCount 之和 */
    @Query("SELECT COALESCE(SUM(b.guestCount), 0) FROM BookingMaster b WHERE b.bookingDate = :date AND b.bookingStatus = :status")
    long sumGuestCountByBookingDateAndBookingStatus(@Param("date") LocalDate date, @Param("status") String status);

    /** 单店聚合：按门店+日期+状态求 guestCount 之和 */
    @Query("SELECT COALESCE(SUM(b.guestCount), 0) FROM BookingMaster b WHERE b.storeId = :storeId AND b.bookingDate = :date AND b.bookingStatus = :status")
    long sumGuestCountByStoreIdAndBookingDateAndBookingStatus(@Param("storeId") Long storeId, @Param("date") LocalDate date, @Param("status") String status);

    /** 全店聚合：按日期+状态返回全部订单 */
    @Query("SELECT b FROM BookingMaster b WHERE b.bookingDate = :date AND b.bookingStatus = :status")
    List<BookingMaster> findAllByBookingDateAndBookingStatus(@Param("date") LocalDate date, @Param("status") String status);

    /** 单店聚合：按门店+日期+状态返回全部订单 */
    @Query("SELECT b FROM BookingMaster b WHERE b.storeId = :storeId AND b.bookingDate = :date AND b.bookingStatus = :status")
    List<BookingMaster> findAllByStoreIdAndBookingDateAndBookingStatus(@Param("storeId") Long storeId, @Param("date") LocalDate date, @Param("status") String status);
}
