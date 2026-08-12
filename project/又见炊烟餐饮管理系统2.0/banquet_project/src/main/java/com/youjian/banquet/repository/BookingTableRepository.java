/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.BookingTable
 *  com.youjian.banquet.repository.BookingTableRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BookingTable;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingTableRepository
extends JpaRepository<BookingTable, Long>,
JpaSpecificationExecutor<BookingTable> {
    public List<BookingTable> findByStoreId(Long var1);

    public List<BookingTable> findByBookingId(String var1);

    public List<BookingTable> findByBookingIdAndStoreId(String var1, Long var2);

    public List<BookingTable> findByBookingMasterId(Long var1);

    public List<BookingTable> findByBookingMasterIdAndStoreId(Long var1, Long var2);

    public List<BookingTable> findByTableIdAndStoreId(Integer var1, Long var2);

    public void deleteByBookingIdAndStoreId(String var1, Long var2);

    public void deleteByBookingId(String var1);

    public void deleteByBookingMasterIdAndStoreId(Long var1, Long var2);

    public boolean existsByTableIdAndStoreIdAndBookingId(Integer var1, Long var2, String var3);

    /** 按门店+日期查全部桌台预订 */
    public List<BookingTable> findByStoreIdAndBookingDate(Long storeId, LocalDate bookingDate);

    /** 全店按日期查全部桌台预订 */
    public List<BookingTable> findByBookingDate(LocalDate bookingDate);

    /** 按门店+日期段查 */
    @Query("SELECT t FROM BookingTable t WHERE t.storeId = :storeId AND t.bookingDate BETWEEN :start AND :end")
    public List<BookingTable> findByStoreIdAndBookingDateBetween(@Param("storeId") Long storeId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 全店按日期段查 */
    @Query("SELECT t FROM BookingTable t WHERE t.bookingDate BETWEEN :start AND :end")
    public List<BookingTable> findByBookingDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 按多个 bookingId 批量查 */
    @Query("SELECT t FROM BookingTable t WHERE t.bookingId IN :bookingIds AND (:storeId IS NULL OR t.storeId = :storeId)")
    public List<BookingTable> findByBookingIdInAndStoreId(@Param("bookingIds") java.util.Collection<String> bookingIds, @Param("storeId") Long storeId);
}

