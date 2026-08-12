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
@Table(name = "storeup")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrStoreup implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "addtime")
    private LocalDateTime addtime;

    @Column(name = "userid")
    private Long userid;

    @Column(name = "refid")
    private Long refid;

    @Column(name = "tablename", length = 200)
    private String tablename;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "picture", columnDefinition = "longtext")
    private String picture;

    @Column(name = "type", length = 200)
    private String type;

    @Column(name = "inteltype", length = 200)
    private String inteltype;

    @Column(name = "remark", length = 200)
    private String remark;

    @PrePersist
    protected void onCreate() {
        if (this.addtime == null) {
            this.addtime = LocalDateTime.now();
        }
        if (this.type == null) {
            this.type = "1";
        }
    }
}