package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 餐厅资讯
 * 来源：点餐系统 news / 表 bt_news
 */
@Data
@Entity
@Table(name = "news")
public class BtNews {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "introduction", columnDefinition = "LONGTEXT")
    private String introduction;

    @Column(name = "picture", nullable = false, columnDefinition = "LONGTEXT")
    private String picture;

    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "addtime")
    private LocalDateTime addtime;

    @PrePersist
    protected void onCreate() {
        this.addtime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
    }
}