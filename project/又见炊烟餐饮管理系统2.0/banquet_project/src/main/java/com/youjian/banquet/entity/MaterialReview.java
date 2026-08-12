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
@Table(name = "material_review")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaterialReview implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "addtime")
    private LocalDateTime addtime;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "avatar_url", columnDefinition = "longtext")
    private String avatarUrl;

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