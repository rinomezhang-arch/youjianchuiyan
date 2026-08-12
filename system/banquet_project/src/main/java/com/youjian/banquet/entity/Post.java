package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Integer postId;

    @Column(name = "dept_id")
    private Integer deptId;

    @Column(name = "post_name")
    private String postName;

    @Column(name = "post_code")
    private String postCode;

    @Column(name = "headcount")
    private Integer headcount;

    @Column(name = "on_duty_count")
    private Integer onDutyCount;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "remark")
    private String remark;
}
