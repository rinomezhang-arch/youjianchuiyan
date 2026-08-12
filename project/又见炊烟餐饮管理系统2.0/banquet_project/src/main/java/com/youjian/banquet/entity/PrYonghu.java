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
@Table(name = "yonghu")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrYonghu implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "addtime")
    private LocalDateTime addtime;

    @Column(name = "yonghuzhanghao", length = 200)
    private String yonghuzhanghao;

    @Column(name = "mima", length = 200)
    private String mima;

    @Column(name = "yonghuxingming", length = 200)
    private String yonghuxingming;

    @Column(name = "touxiang", columnDefinition = "longtext")
    private String touxiang;

    @Column(name = "xingbie", length = 200)
    private String xingbie;

    @Column(name = "shoujihaoma", length = 200)
    private String shoujihaoma;

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