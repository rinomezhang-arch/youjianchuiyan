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
@Table(name = "gongyingshang")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrSupplier implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "addtime")
    private LocalDateTime addtime;

    @Column(name = "gongyingshangzhanghao", length = 200)
    private String gongyingshangzhanghao;

    @Column(name = "mima", length = 200)
    private String mima;

    @Column(name = "gongyingshangmingcheng", length = 200)
    private String gongyingshangmingcheng;

    @Column(name = "tupian", columnDefinition = "longtext")
    private String tupian;

    @Column(name = "lianxiren", length = 200)
    private String lianxiren;

    @Column(name = "lianxidianhua", length = 200)
    private String lianxidianhua;

    @Column(name = "gongyingshangdizhi", length = 200)
    private String gongyingshangdizhi;

    @Column(name = "money")
    private Float money;

    @PrePersist
    protected void onCreate() {
        if (this.addtime == null) {
            this.addtime = LocalDateTime.now();
        }
        if (this.money == null) {
            this.money = 0f;
        }
    }
}