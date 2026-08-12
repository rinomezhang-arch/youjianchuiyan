package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "change_log")
public class ChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "operator_id")
    private Integer operatorId;

    @Column(name = "operator_name", length = 50)
    private String operatorName;

    @Column(name = "operation_type", nullable = false, length = 30)
    private String operationType;

    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType;

    @Column(name = "target_id", length = 50)
    private String targetId;

    @Column(name = "summary", nullable = false, length = 200)
    private String summary;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
