package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "self_service_submission")
public class SelfServiceSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "submit_type")
    private String submitType;

    @Column(name = "job_posting_id")
    private Long jobPostingId;

    @Column(name = "name")
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "id_card")
    private String idCard;

    @Column(name = "department")
    private String department;

    @Column(name = "position")
    private String position;

    @Column(name = "gender")
    private String gender;

    @Column(name = "address")
    private String address;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    @Column(name = "emergency_phone")
    private String emergencyPhone;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "remark")
    private String remark;

    @Column(name = "status")
    private String status;

    @Column(name = "reject_note")
    private String rejectNote;

    @Column(name = "reviewer_id")
    private Integer reviewerId;

    @Column(name = "reviewer_name")
    private String reviewerName;

    @Column(name = "review_time")
    private LocalDateTime reviewTime;

    @Column(name = "converted_staff_id")
    private Integer convertedStaffId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
