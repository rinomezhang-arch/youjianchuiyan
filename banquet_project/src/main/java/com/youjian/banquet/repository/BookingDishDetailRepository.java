/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.BookingDishDetail
 *  com.youjian.banquet.repository.BookingDishDetailRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BookingDishDetail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingDishDetailRepository
extends JpaRepository<BookingDishDetail, Long>,
JpaSpecificationExecutor<BookingDishDetail> {
    public List<BookingDishDetail> findByStoreId(Long var1);

    public List<BookingDishDetail> findByBookingIdAndStoreId(String var1, Long var2);

    public List<BookingDishDetail> findByBookingId(String var1);

    public void deleteByBookingIdAndStoreId(String var1, Long var2);

    public void deleteByBookingId(String var1);
}

