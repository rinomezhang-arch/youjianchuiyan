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
@Table(name = "cailiaoxinxi")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrMaterialInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "addtime")
    private LocalDateTime addtime;

    @Column(name = "cailiaomingcheng", length = 200)
    private String cailiaomingcheng;

    @Column(name = "tupian", columnDefinition = "longtext")
    private String tupian;

    @Column(name = "cailiaozhonglei", length = 200)
    private String cailiaozhonglei;

    @Column(name = "cailiaoguige", length = 200)
    private String cailiaoguige;

    @Column(name = "cailiaoxiangqing", columnDefinition = "longtext")
    private String cailiaoxiangqing;

    @Column(name = "gongyingshangzhanghao", length = 200)
    private String gongyingshangzhanghao;

    @Column(name = "gongyingshangmingcheng", length = 200)
    private String gongyingshangmingcheng;

    @Column(name = "onelimittimes")
    private Integer onelimittimes;

    @Column(name = "alllimittimes")
    private Integer alllimittimes;

    @Column(name = "clicktime")
    private LocalDateTime clicktime;

    @Column(name = "price")
    private Float price;

    @PrePersist
    protected void onCreate() {
        if (this.addtime == null) {
            this.addtime = LocalDateTime.now();
        }
    }
}