package com.youjian.banquet.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.youjian.banquet.config.BankAccountConverter;
import com.youjian.banquet.config.IdCardSerializer;
import com.youjian.banquet.config.SensitiveDataSerializer;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_master")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_id")
    private Integer staffId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "staff_name", length = 20)
    private String staffName;

    @Column(name = "staff_account", length = 20)
    private String staffAccount;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "staff_password", length = 100)
    private String staffPassword;

    @Column(name = "staff_gender", length = 2)
    private String staffGender;

    @Column(name = "staff_age")
    private Integer staffAge;

    @Column(name = "staff_phone", length = 20)
    private String staffPhone;

    @Column(name = "staff_position", length = 50)
    private String staffPosition;

    @Column(name = "department", length = 50)
    private String department;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "monthly_salary", precision = 10, scale = 2)
    private BigDecimal monthlySalary;

    @Column(name = "id_card", length = 20)
    @Convert(converter = BankAccountConverter.class)
    @JsonSerialize(using = IdCardSerializer.class)
    private String idCard;

    @Column(name = "home_address", length = 100)
    private String homeAddress;

    @Column(name = "emergency_contact", length = 20)
    private String emergencyContact;

    @Column(name = "emergency_phone", length = 20)
    private String emergencyPhone;

    @Column(name = "employment_status", length = 10)
    private String employmentStatus;

    @Column(name = "resign_reason", columnDefinition = "TEXT")
    private String resignReason;

    @Column(name = "resign_date")
    private LocalDate resignDate;

    @Column(name = "role", length = 30)
    private String role;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "permission_level")
    private Integer permissionLevel;

    @Column(name = "dept_id")
    private Integer deptId;

    @Column(name = "can_manage_kitchen")
    private Integer canManageKitchen;

    @Column(name = "can_manage_sales")
    private Integer canManageSales;

    @Column(name = "can_manage_finance")
    private Integer canManageFinance;

    @Column(name = "can_manage_hr")
    private Integer canManageHr;

    @Column(name = "can_view_all_stores")
    private Integer canViewAllStores;

    @Column(name = "can_edit_system")
    private Integer canEditSystem;

    @Column(name = "staff_no", length = 20)
    private String staffNo;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "nation", length = 20)
    private String nation;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "native_place", length = 100)
    private String nativePlace;

    @Column(name = "marital_status", length = 10)
    private String maritalStatus;

    @Column(name = "political_status", length = 20)
    private String politicalStatus;

    @Column(name = "education", length = 20)
    private String education;

    @Column(name = "major", length = 50)
    private String major;

    @Column(name = "graduate_school", length = 100)
    private String graduateSchool;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "wechat", length = 50)
    private String wechat;

    @Column(name = "staff_rank", length = 20)
    private String staffRank;

    @Column(name = "employment_type", length = 20)
    private String employmentType;

    @Column(name = "hire_channel", length = 30)
    private String hireChannel;

    @Column(name = "probation_months", precision = 3, scale = 1)
    private BigDecimal probationMonths;

    @Column(name = "probation_start_date")
    private LocalDate probationStartDate;

    @Column(name = "probation_end_date")
    private LocalDate probationEndDate;

    @Column(name = "regular_date")
    private LocalDate regularDate;

    @Column(name = "leader_id")
    private Integer leaderId;

    @Column(name = "work_location", length = 100)
    private String workLocation;

    @Column(name = "basic_salary", precision = 12, scale = 2)
    private BigDecimal basicSalary;

    @Column(name = "performance_salary", precision = 12, scale = 2)
    private BigDecimal performanceSalary;

    @Column(name = "subsidy", precision = 12, scale = 2)
    private BigDecimal subsidy;

    @Column(name = "bonus", precision = 12, scale = 2)
    private BigDecimal bonus;

    @Column(name = "social_insurance", precision = 12, scale = 2)
    private BigDecimal socialInsurance;

    @Column(name = "housing_fund", precision = 12, scale = 2)
    private BigDecimal housingFund;

    @Column(name = "bank_name", length = 50)
    private String bankName;

    @Column(name = "bank_account", length = 30)
    @Convert(converter = BankAccountConverter.class)
    @JsonSerialize(using = SensitiveDataSerializer.class)
    private String bankAccount;

    @Column(name = "account_holder", length = 20)
    private String accountHolder;

    @Column(name = "entry_age")
    private Integer entryAge;

    @Column(name = "work_years", precision = 5, scale = 2)
    private BigDecimal workYears;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
