package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "booking_inquiry")
public class BookingInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "preferred_date")
    private LocalDate preferredDate;

    @Column(name = "preferred_time")
    private String preferredTime;

    @Column(name = "guest_count")
    private Integer guestCount;

    @Column(name = "selected_dishes", columnDefinition = "TEXT")
    private String selectedDishes;

    @Column(name = "remark")
    private String remark;

    /**
     * 转成正式预订后回填的 booking_id；未转换时为 NULL。
     * <p>
     * 可空 + 唯一：可空是为了兼容全部历史咨询（它们本来就没转过），
     * 唯一是为了让"同一张正式预订被两条咨询认领"在数据库层就不可能发生。
     * MySQL 的唯一索引允许多个 NULL，所以不影响历史行。
     */
    @Column(name = "booking_id", length = 20, unique = true)
    private String bookingId;

    @Column(name = "status")
    private String status;

    @Column(name = "staff_note")
    private String staffNote;

    @Column(name = "handled_by")
    private String handledBy;

    @Column(name = "handled_time")
    private LocalDateTime handledTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
}
