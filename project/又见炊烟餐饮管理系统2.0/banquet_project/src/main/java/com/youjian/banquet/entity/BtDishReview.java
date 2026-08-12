package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 菜品评论（discusscaipinxinxi）
 * 来源：点餐系统 discusscaipinxinxi / 表 bt_dish_review
 */
@Data
@Entity
@Table(name = "discusscaipinxinxi")
public class BtDishReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "refid", nullable = false)
    private Long refid;

    @Column(name = "userid", nullable = false)
    private Long userid;

    @Column(name = "avatarurl", columnDefinition = "LONGTEXT")
    private String avatarurl;

    @Column(name = "nickname", length = 200)
    private String nickname;

    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "reply", columnDefinition = "LONGTEXT")
    private String reply;

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