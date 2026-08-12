package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_cart")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseCart implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "addtime")
    private LocalDateTime addtime;

    @Column(name = "table_name", length = 200)
    private String tableName;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "good_id")
    private Long goodId;

    @Column(name = "good_name", length = 200)
    private String goodName;

    @Column(name = "picture", columnDefinition = "longtext")
    private String picture;

    @Column(name = "buy_number")
    private Integer buyNumber;

    @Column(name = "price")
    private Float price;

    @Column(name = "discount_price")
    private Float discountPrice;

    @Column(name = "supplier_account", length = 200)
    private String supplierAccount;

    @PrePersist
    protected void onCreate() {
        if (this.addtime == null) {
            this.addtime = LocalDateTime.now();
        }
        if (this.tableName == null) {
            this.tableName = "material_info";
        }
    }
}