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
@Table(name = "cart")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrCart implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "addtime")
    private LocalDateTime addtime;

    @Column(name = "tablename", length = 200)
    private String tablename;

    @Column(name = "userid")
    private Long userid;

    @Column(name = "goodid")
    private Long goodid;

    @Column(name = "goodname", length = 200)
    private String goodname;

    @Column(name = "picture", columnDefinition = "longtext")
    private String picture;

    @Column(name = "buynumber")
    private Integer buynumber;

    @Column(name = "price")
    private Float price;

    @Column(name = "discountprice")
    private Float discountprice;

    @Column(name = "gongyingshangzhanghao", length = 200)
    private String gongyingshangzhanghao;

    @PrePersist
    protected void onCreate() {
        if (this.addtime == null) {
            this.addtime = LocalDateTime.now();
        }
        if (this.tablename == null) {
            this.tablename = "cailiaoxinxi";
        }
    }
}