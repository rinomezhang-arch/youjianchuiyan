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
@Table(name = "discusscailiaoxinxi")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrMaterialReview implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "addtime")
    private LocalDateTime addtime;

    @Column(name = "refid")
    private Long refid;

    @Column(name = "userid")
    private Long userid;

    @Column(name = "avatarurl", columnDefinition = "longtext")
    private String avatarurl;

    @Column(name = "nickname", length = 200)
    private String nickname;

    @Column(name = "content", columnDefinition = "longtext")
    private String content;

    @Column(name = "reply", columnDefinition = "longtext")
    private String reply;

    @PrePersist
    protected void onCreate() {
        if (this.addtime == null) {
            this.addtime = LocalDateTime.now();
        }
    }
}