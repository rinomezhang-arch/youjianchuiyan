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
@Table(name = "banquet_template_rel")
public class BanquetTemplateRel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "banquet_type_id", nullable = false)
    private Integer banquetTypeId;

    @Column(name = "template_id", nullable = false)
    private Integer templateId;

    @Column(name = "is_default")
    private Integer isDefault;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (isDefault == null) isDefault = 0;
    }
}
