package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tool_damage")
public class ToolDamage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "damage_id")
    private Long damageId;

    @Column(name = "damage_no", nullable = false, unique = true, length = 32)
    private String damageNo;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "tool_id", nullable = false)
    private Long toolId;

    @Column(name = "staff_id")
    private Integer staffId;

    @Column(name = "damage_date", nullable = false)
    private LocalDate damageDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "damage_type", nullable = false)
    private DamageType damageType;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "compensation_amount", precision = 10, scale = 2)
    private BigDecimal compensationAmount;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "handler_id")
    private Long handlerId;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "creator", length = 64)
    private String creator;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum DamageType {
        轻微, 严重, 报废, 丢失
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (status == null) status = "待处理";
        if (damageType == null) damageType = DamageType.轻微;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
