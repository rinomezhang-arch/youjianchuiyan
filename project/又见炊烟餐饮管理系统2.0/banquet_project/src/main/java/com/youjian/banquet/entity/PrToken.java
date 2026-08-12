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
@Table(name = "token")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrToken implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "userid")
    private Long userid;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "tablename", length = 100)
    private String tablename;

    @Column(name = "role", length = 100)
    private String role;

    @Column(name = "token", length = 200)
    private String token;

    @Column(name = "addtime")
    private LocalDateTime addtime;

    @Column(name = "expiratedtime")
    private LocalDateTime expiratedtime;

    @PrePersist
    protected void onCreate() {
        if (this.addtime == null) {
            this.addtime = LocalDateTime.now();
        }
        if (this.expiratedtime == null) {
            this.expiratedtime = LocalDateTime.now();
        }
    }

    public PrToken(Long userid, String username, String tablename, String role, String token, LocalDateTime expiratedtime) {
        this.userid = userid;
        this.username = username;
        this.tablename = tablename;
        this.role = role;
        this.token = token;
        this.expiratedtime = expiratedtime;
        this.addtime = LocalDateTime.now();
    }
}