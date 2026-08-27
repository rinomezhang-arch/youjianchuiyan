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
}
