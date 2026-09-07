package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "wx_customer")
@Data
public class WxCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "open_id", unique = true, nullable = false)
    private String openId;

    @Column(name = "union_id")
    private String unionId;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "phone")
    private String phone;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
