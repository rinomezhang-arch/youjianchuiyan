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
@Table(name = "caigouruku")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrPurchaseIn implements Serializable {

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

    @Column(name = "cailiaozhonglei", length = 200)
    private String cailiaozhonglei;

    @Column(name = "cailiaoguige", length = 200)
    private String cailiaoguige;

    @Column(name = "alllimittimes")
    private Integer alllimittimes;

    @Column(name = "rukushijian")
    private LocalDateTime rukushijian;

    @Column(name = "beizhu", length = 200)
    private String beizhu;

    @Column(name = "gongyingshangzhanghao", length = 200)
    private String gongyingshangzhanghao;

    @Column(name = "gongyingshangmingcheng", length = 200)
    private String gongyingshangmingcheng;

    @PrePersist
    protected void onCreate() {
        if (this.addtime == null) {
            this.addtime = LocalDateTime.now();
        }
    }
}