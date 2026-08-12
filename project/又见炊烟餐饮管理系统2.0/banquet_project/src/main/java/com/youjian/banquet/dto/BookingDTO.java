package com.youjian.banquet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDTO {
    private String bookingId;
    private String bookingNo;
    private String storeId;
    private String customerId;
    private String customerName;
    private String customerPhone;
    private LocalDate bookingDate;
    private LocalTime bookingTime;
    private String bookingType;
    private String pendingName;
    private String pendingPhone;
    private String sourceType;
    private String referrerName;
    private String referrerPhone;
    private Integer staffId;
    private String staffName;
    private String staffDept;
    private String coordinatorName;
    private String coordinatorPhone;
    private Integer guestCount;
    private Integer tableCount;
    private Integer spareTables;
    private Integer guestPerTable;
    private String occasionType;
    private String occasion;
    private String banquetName;
    private String packageId;
    private String packageName;
    private BigDecimal totalAmount;
    private BigDecimal finalAmount;
    @JsonProperty("deposit")
    private BigDecimal depositAmount;
    private String bookingStatus;
    private String status;
    private String paymentStatus;
    private String remark;
    private String notes;
    private String specialRequest;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private List<String> tableIds;
    private List<String> tableNames;
    private List<BookingDishDTO> dishes;
}
