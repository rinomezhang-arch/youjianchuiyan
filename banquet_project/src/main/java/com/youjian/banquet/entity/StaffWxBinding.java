package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "staff_wx_binding")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffWxBinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_id", nullable = false)
    private Integer staffId;

    @Column(name = "open_id", nullable = false, unique = true, length = 64)
    private String openId;

    @Column(name = "bound_at")
    private LocalDateTime boundAt;
}
