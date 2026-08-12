package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Token
 * 来源：点餐系统 token / 表 bt_token
 */
@Data
@Entity
@Table(name = "token")
public class BtToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "userid", nullable = false)
    private Long userid;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "tablename", length = 100)
    private String tablename;

    @Column(name = "role", length = 100)
    private String role;

    @Column(name = "token", nullable = false, length = 200)
    private String token;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "addtime")
    private LocalDateTime addtime;

    @Column(name = "expiratedtime")
    private LocalDateTime expiratedtime;

    @PrePersist
    protected void onCreate() {
        this.addtime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
    }

    public BtToken() {
    }

    public BtToken(Long userid, String username, String tablename, String role, String token, LocalDateTime expiratedtime) {
        this.userid = userid;
        this.username = username;
        this.tablename = tablename;
        this.role = role;
        this.token = token;
        this.expiratedtime = expiratedtime;
    }
}