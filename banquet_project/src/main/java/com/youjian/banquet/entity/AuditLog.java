package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "action", length = 128)
    private String action;

    @Column(name = "target", length = 256)
    private String target;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @Column(name = "created_at")
    private Long createdAt;

    @Column(name = "store_id", nullable = false)
    private Long storeId;
}
