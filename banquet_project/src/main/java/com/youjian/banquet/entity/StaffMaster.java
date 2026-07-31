/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.StaffMaster
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.PrePersist
 *  jakarta.persistence.PreUpdate
 *  jakarta.persistence.Table
 */
package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="staff_master")
public class StaffMaster {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="staff_id")
    private Integer staffId;
    @Column(name="store_id")
    private Long storeId;
    @Column(name="staff_name")
    private String staffName;
    @Column(name="staff_account")
    private String staffAccount;
    // 安全修复 N1：密码字段仅写不读，序列化输出时不包含，反序列化仍可接收用于登录/创建
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name="staff_password")
    private String staffPassword;
    @Column(name="staff_gender")
    private String staffGender;
    @Column(name="staff_age")
    private Integer staffAge;
    @Column(name="staff_phone")
    private String staffPhone;
    @Column(name="staff_position")
    private String staffPosition;
    @Column(name="department")
    private String department;
    @Column(name="hire_date")
    private LocalDate hireDate;
    @Column(name="monthly_salary", precision=10, scale=2)
    private BigDecimal monthlySalary;
    @Column(name="id_card")
    private String idCard;
    @Column(name="home_address")
    private String homeAddress;
    @Column(name="emergency_contact")
    private String emergencyContact;
    @Column(name="emergency_phone")
    private String emergencyPhone;
    @Column(name="employment_status")
    private String employmentStatus;
    @Column(name="resign_reason", columnDefinition="TEXT")
    private String resignReason;
    @Column(name="resign_date")
    private LocalDate resignDate;
    @Column(name="role")
    private String role;
    @Column(name="remark", columnDefinition="TEXT")
    private String remark;
    @Column(name="create_time")
    private LocalDateTime createdAt;
    @Column(name="update_time")
    private LocalDateTime updatedAt;
    @Column(name="permission_level")
    private Integer permissionLevel;
    @Column(name="dept_id")
    private Integer deptId;
    @Column(name="can_manage_kitchen")
    private Integer canManageKitchen;
    @Column(name="can_manage_sales")
    private Integer canManageSales;
    @Column(name="can_manage_finance")
    private Integer canManageFinance;
    @Column(name="can_manage_hr")
    private Integer canManageHr;
    @Column(name="can_view_all_stores")
    private Integer canViewAllStores;
    @Column(name="can_edit_system")
    private Integer canEditSystem;

    // ===== HR 扩展字段（对应规划手册 5.txt 阶段1.1） =====
    @Column(name="staff_code", length=20)
    private String staffCode;
    @Column(name="gender")
    private Integer gender;
    @Column(name="birth_date")
    private LocalDate birthDate;
    @Column(name="email", length=100)
    private String email;
    @Column(name="wechat", length=50)
    private String wechat;
    @Column(name="native_place", length=100)
    private String nativePlace;
    @Column(name="nation", length=20)
    private String nation;
    @Column(name="marital_status")
    private Integer maritalStatus;
    @Column(name="education", length=20)
    private String education;
    @Column(name="major", length=50)
    private String major;
    @Column(name="graduate_school", length=100)
    private String graduateSchool;
    @Column(name="graduate_date")
    private LocalDate graduateDate;
    @Column(name="probation_months")
    private Integer probationMonths;
    @Column(name="regular_date")
    private LocalDate regularDate;
    @Column(name="leader_id")
    private Long leaderId;
    @Column(name="employment_type")
    private Integer employmentType;
    @Column(name="avatar_url", length=500)
    private String avatarUrl;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getStaffId() {
        return this.staffId;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getStaffName() {
        return this.staffName;
    }

    public String getStaffAccount() {
        return this.staffAccount;
    }

    public String getStaffPassword() {
        return this.staffPassword;
    }

    public String getStaffGender() {
        return this.staffGender;
    }

    public Integer getStaffAge() {
        return this.staffAge;
    }

    public String getStaffPhone() {
        return this.staffPhone;
    }

    public String getStaffPosition() {
        return this.staffPosition;
    }

    public String getDepartment() {
        return this.department;
    }

    public LocalDate getHireDate() {
        return this.hireDate;
    }

    public BigDecimal getMonthlySalary() {
        return this.monthlySalary;
    }

    public String getIdCard() {
        return this.idCard;
    }

    public String getHomeAddress() {
        return this.homeAddress;
    }

    public String getEmergencyContact() {
        return this.emergencyContact;
    }

    public String getEmergencyPhone() {
        return this.emergencyPhone;
    }

    public String getEmploymentStatus() {
        return this.employmentStatus;
    }

    public String getResignReason() {
        return this.resignReason;
    }

    public LocalDate getResignDate() {
        return this.resignDate;
    }

    public String getRole() {
        return this.role;
    }

    public String getRemark() {
        return this.remark;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public Integer getPermissionLevel() {
        return this.permissionLevel;
    }

    public Integer getDeptId() {
        return this.deptId;
    }

    public Integer getCanManageKitchen() {
        return this.canManageKitchen;
    }

    public Integer getCanManageSales() {
        return this.canManageSales;
    }

    public Integer getCanManageFinance() {
        return this.canManageFinance;
    }

    public Integer getCanManageHr() {
        return this.canManageHr;
    }

    public Integer getCanViewAllStores() {
        return this.canViewAllStores;
    }

    public Integer getCanEditSystem() {
        return this.canEditSystem;
    }

    public void setStaffId(Integer staffId) {
        this.staffId = staffId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public void setStaffAccount(String staffAccount) {
        this.staffAccount = staffAccount;
    }

    public void setStaffPassword(String staffPassword) {
        this.staffPassword = staffPassword;
    }

    public void setStaffGender(String staffGender) {
        this.staffGender = staffGender;
    }

    public void setStaffAge(Integer staffAge) {
        this.staffAge = staffAge;
    }

    public void setStaffPhone(String staffPhone) {
        this.staffPhone = staffPhone;
    }

    public void setStaffPosition(String staffPosition) {
        this.staffPosition = staffPosition;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public void setMonthlySalary(BigDecimal monthlySalary) {
        this.monthlySalary = monthlySalary;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public void setHomeAddress(String homeAddress) {
        this.homeAddress = homeAddress;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public void setEmergencyPhone(String emergencyPhone) {
        this.emergencyPhone = emergencyPhone;
    }

    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public void setResignReason(String resignReason) {
        this.resignReason = resignReason;
    }

    public void setResignDate(LocalDate resignDate) {
        this.resignDate = resignDate;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setPermissionLevel(Integer permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public void setDeptId(Integer deptId) {
        this.deptId = deptId;
    }

    public void setCanManageKitchen(Integer canManageKitchen) {
        this.canManageKitchen = canManageKitchen;
    }

    public void setCanManageSales(Integer canManageSales) {
        this.canManageSales = canManageSales;
    }

    public void setCanManageFinance(Integer canManageFinance) {
        this.canManageFinance = canManageFinance;
    }

    public void setCanManageHr(Integer canManageHr) {
        this.canManageHr = canManageHr;
    }

    public void setCanViewAllStores(Integer canViewAllStores) {
        this.canViewAllStores = canViewAllStores;
    }

    public void setCanEditSystem(Integer canEditSystem) {
        this.canEditSystem = canEditSystem;
    }

    // ===== HR 扩展字段 getter/setter =====
    public String getStaffCode() { return this.staffCode; }
    public void setStaffCode(String staffCode) { this.staffCode = staffCode; }
    public Integer getGender() { return this.gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public LocalDate getBirthDate() { return this.birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getEmail() { return this.email; }
    public void setEmail(String email) { this.email = email; }
    public String getWechat() { return this.wechat; }
    public void setWechat(String wechat) { this.wechat = wechat; }
    public String getNativePlace() { return this.nativePlace; }
    public void setNativePlace(String nativePlace) { this.nativePlace = nativePlace; }
    public String getNation() { return this.nation; }
    public void setNation(String nation) { this.nation = nation; }
    public Integer getMaritalStatus() { return this.maritalStatus; }
    public void setMaritalStatus(Integer maritalStatus) { this.maritalStatus = maritalStatus; }
    public String getEducation() { return this.education; }
    public void setEducation(String education) { this.education = education; }
    public String getMajor() { return this.major; }
    public void setMajor(String major) { this.major = major; }
    public String getGraduateSchool() { return this.graduateSchool; }
    public void setGraduateSchool(String graduateSchool) { this.graduateSchool = graduateSchool; }
    public LocalDate getGraduateDate() { return this.graduateDate; }
    public void setGraduateDate(LocalDate graduateDate) { this.graduateDate = graduateDate; }
    public Integer getProbationMonths() { return this.probationMonths; }
    public void setProbationMonths(Integer probationMonths) { this.probationMonths = probationMonths; }
    public LocalDate getRegularDate() { return this.regularDate; }
    public void setRegularDate(LocalDate regularDate) { this.regularDate = regularDate; }
    public Long getLeaderId() { return this.leaderId; }
    public void setLeaderId(Long leaderId) { this.leaderId = leaderId; }
    public Integer getEmploymentType() { return this.employmentType; }
    public void setEmploymentType(Integer employmentType) { this.employmentType = employmentType; }
    public String getAvatarUrl() { return this.avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StaffMaster)) {
            return false;
        }
        StaffMaster other = (StaffMaster)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$staffId = this.getStaffId();
        Integer other$staffId = other.getStaffId();
        if (this$staffId == null ? other$staffId != null : !((Object)this$staffId).equals(other$staffId)) {
            return false;
        }
        Long this$storeId = this.getStoreId();
        Long other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !((Object)this$storeId).equals(other$storeId)) {
            return false;
        }
        Integer this$staffAge = this.getStaffAge();
        Integer other$staffAge = other.getStaffAge();
        if (this$staffAge == null ? other$staffAge != null : !((Object)this$staffAge).equals(other$staffAge)) {
            return false;
        }
        Integer this$permissionLevel = this.getPermissionLevel();
        Integer other$permissionLevel = other.getPermissionLevel();
        if (this$permissionLevel == null ? other$permissionLevel != null : !((Object)this$permissionLevel).equals(other$permissionLevel)) {
            return false;
        }
        Integer this$deptId = this.getDeptId();
        Integer other$deptId = other.getDeptId();
        if (this$deptId == null ? other$deptId != null : !((Object)this$deptId).equals(other$deptId)) {
            return false;
        }
        Integer this$canManageKitchen = this.getCanManageKitchen();
        Integer other$canManageKitchen = other.getCanManageKitchen();
        if (this$canManageKitchen == null ? other$canManageKitchen != null : !((Object)this$canManageKitchen).equals(other$canManageKitchen)) {
            return false;
        }
        Integer this$canManageSales = this.getCanManageSales();
        Integer other$canManageSales = other.getCanManageSales();
        if (this$canManageSales == null ? other$canManageSales != null : !((Object)this$canManageSales).equals(other$canManageSales)) {
            return false;
        }
        Integer this$canManageFinance = this.getCanManageFinance();
        Integer other$canManageFinance = other.getCanManageFinance();
        if (this$canManageFinance == null ? other$canManageFinance != null : !((Object)this$canManageFinance).equals(other$canManageFinance)) {
            return false;
        }
        Integer this$canManageHr = this.getCanManageHr();
        Integer other$canManageHr = other.getCanManageHr();
        if (this$canManageHr == null ? other$canManageHr != null : !((Object)this$canManageHr).equals(other$canManageHr)) {
            return false;
        }
        Integer this$canViewAllStores = this.getCanViewAllStores();
        Integer other$canViewAllStores = other.getCanViewAllStores();
        if (this$canViewAllStores == null ? other$canViewAllStores != null : !((Object)this$canViewAllStores).equals(other$canViewAllStores)) {
            return false;
        }
        Integer this$canEditSystem = this.getCanEditSystem();
        Integer other$canEditSystem = other.getCanEditSystem();
        if (this$canEditSystem == null ? other$canEditSystem != null : !((Object)this$canEditSystem).equals(other$canEditSystem)) {
            return false;
        }
        String this$staffName = this.getStaffName();
        String other$staffName = other.getStaffName();
        if (this$staffName == null ? other$staffName != null : !this$staffName.equals(other$staffName)) {
            return false;
        }
        String this$staffAccount = this.getStaffAccount();
        String other$staffAccount = other.getStaffAccount();
        if (this$staffAccount == null ? other$staffAccount != null : !this$staffAccount.equals(other$staffAccount)) {
            return false;
        }
        String this$staffPassword = this.getStaffPassword();
        String other$staffPassword = other.getStaffPassword();
        if (this$staffPassword == null ? other$staffPassword != null : !this$staffPassword.equals(other$staffPassword)) {
            return false;
        }
        String this$staffGender = this.getStaffGender();
        String other$staffGender = other.getStaffGender();
        if (this$staffGender == null ? other$staffGender != null : !this$staffGender.equals(other$staffGender)) {
            return false;
        }
        String this$staffPhone = this.getStaffPhone();
        String other$staffPhone = other.getStaffPhone();
        if (this$staffPhone == null ? other$staffPhone != null : !this$staffPhone.equals(other$staffPhone)) {
            return false;
        }
        String this$staffPosition = this.getStaffPosition();
        String other$staffPosition = other.getStaffPosition();
        if (this$staffPosition == null ? other$staffPosition != null : !this$staffPosition.equals(other$staffPosition)) {
            return false;
        }
        String this$department = this.getDepartment();
        String other$department = other.getDepartment();
        if (this$department == null ? other$department != null : !this$department.equals(other$department)) {
            return false;
        }
        LocalDate this$hireDate = this.getHireDate();
        LocalDate other$hireDate = other.getHireDate();
        if (this$hireDate == null ? other$hireDate != null : !((Object)this$hireDate).equals(other$hireDate)) {
            return false;
        }
        BigDecimal this$monthlySalary = this.getMonthlySalary();
        BigDecimal other$monthlySalary = other.getMonthlySalary();
        if (this$monthlySalary == null ? other$monthlySalary != null : !((Object)this$monthlySalary).equals(other$monthlySalary)) {
            return false;
        }
        String this$idCard = this.getIdCard();
        String other$idCard = other.getIdCard();
        if (this$idCard == null ? other$idCard != null : !this$idCard.equals(other$idCard)) {
            return false;
        }
        String this$homeAddress = this.getHomeAddress();
        String other$homeAddress = other.getHomeAddress();
        if (this$homeAddress == null ? other$homeAddress != null : !this$homeAddress.equals(other$homeAddress)) {
            return false;
        }
        String this$emergencyContact = this.getEmergencyContact();
        String other$emergencyContact = other.getEmergencyContact();
        if (this$emergencyContact == null ? other$emergencyContact != null : !this$emergencyContact.equals(other$emergencyContact)) {
            return false;
        }
        String this$emergencyPhone = this.getEmergencyPhone();
        String other$emergencyPhone = other.getEmergencyPhone();
        if (this$emergencyPhone == null ? other$emergencyPhone != null : !this$emergencyPhone.equals(other$emergencyPhone)) {
            return false;
        }
        String this$employmentStatus = this.getEmploymentStatus();
        String other$employmentStatus = other.getEmploymentStatus();
        if (this$employmentStatus == null ? other$employmentStatus != null : !this$employmentStatus.equals(other$employmentStatus)) {
            return false;
        }
        String this$resignReason = this.getResignReason();
        String other$resignReason = other.getResignReason();
        if (this$resignReason == null ? other$resignReason != null : !this$resignReason.equals(other$resignReason)) {
            return false;
        }
        LocalDate this$resignDate = this.getResignDate();
        LocalDate other$resignDate = other.getResignDate();
        if (this$resignDate == null ? other$resignDate != null : !((Object)this$resignDate).equals(other$resignDate)) {
            return false;
        }
        String this$role = this.getRole();
        String other$role = other.getRole();
        if (this$role == null ? other$role != null : !this$role.equals(other$role)) {
            return false;
        }
        String this$remark = this.getRemark();
        String other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt)) {
            return false;
        }
        LocalDateTime this$updatedAt = this.getUpdatedAt();
        LocalDateTime other$updatedAt = other.getUpdatedAt();
        return !(this$updatedAt == null ? other$updatedAt != null : !((Object)this$updatedAt).equals(other$updatedAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof StaffMaster;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $staffId = this.getStaffId();
        result = result * 59 + ($staffId == null ? 43 : ((Object)$staffId).hashCode());
        Long $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : ((Object)$storeId).hashCode());
        Integer $staffAge = this.getStaffAge();
        result = result * 59 + ($staffAge == null ? 43 : ((Object)$staffAge).hashCode());
        Integer $permissionLevel = this.getPermissionLevel();
        result = result * 59 + ($permissionLevel == null ? 43 : ((Object)$permissionLevel).hashCode());
        Integer $deptId = this.getDeptId();
        result = result * 59 + ($deptId == null ? 43 : ((Object)$deptId).hashCode());
        Integer $canManageKitchen = this.getCanManageKitchen();
        result = result * 59 + ($canManageKitchen == null ? 43 : ((Object)$canManageKitchen).hashCode());
        Integer $canManageSales = this.getCanManageSales();
        result = result * 59 + ($canManageSales == null ? 43 : ((Object)$canManageSales).hashCode());
        Integer $canManageFinance = this.getCanManageFinance();
        result = result * 59 + ($canManageFinance == null ? 43 : ((Object)$canManageFinance).hashCode());
        Integer $canManageHr = this.getCanManageHr();
        result = result * 59 + ($canManageHr == null ? 43 : ((Object)$canManageHr).hashCode());
        Integer $canViewAllStores = this.getCanViewAllStores();
        result = result * 59 + ($canViewAllStores == null ? 43 : ((Object)$canViewAllStores).hashCode());
        Integer $canEditSystem = this.getCanEditSystem();
        result = result * 59 + ($canEditSystem == null ? 43 : ((Object)$canEditSystem).hashCode());
        String $staffName = this.getStaffName();
        result = result * 59 + ($staffName == null ? 43 : $staffName.hashCode());
        String $staffAccount = this.getStaffAccount();
        result = result * 59 + ($staffAccount == null ? 43 : $staffAccount.hashCode());
        String $staffPassword = this.getStaffPassword();
        result = result * 59 + ($staffPassword == null ? 43 : $staffPassword.hashCode());
        String $staffGender = this.getStaffGender();
        result = result * 59 + ($staffGender == null ? 43 : $staffGender.hashCode());
        String $staffPhone = this.getStaffPhone();
        result = result * 59 + ($staffPhone == null ? 43 : $staffPhone.hashCode());
        String $staffPosition = this.getStaffPosition();
        result = result * 59 + ($staffPosition == null ? 43 : $staffPosition.hashCode());
        String $department = this.getDepartment();
        result = result * 59 + ($department == null ? 43 : $department.hashCode());
        LocalDate $hireDate = this.getHireDate();
        result = result * 59 + ($hireDate == null ? 43 : ((Object)$hireDate).hashCode());
        BigDecimal $monthlySalary = this.getMonthlySalary();
        result = result * 59 + ($monthlySalary == null ? 43 : ((Object)$monthlySalary).hashCode());
        String $idCard = this.getIdCard();
        result = result * 59 + ($idCard == null ? 43 : $idCard.hashCode());
        String $homeAddress = this.getHomeAddress();
        result = result * 59 + ($homeAddress == null ? 43 : $homeAddress.hashCode());
        String $emergencyContact = this.getEmergencyContact();
        result = result * 59 + ($emergencyContact == null ? 43 : $emergencyContact.hashCode());
        String $emergencyPhone = this.getEmergencyPhone();
        result = result * 59 + ($emergencyPhone == null ? 43 : $emergencyPhone.hashCode());
        String $employmentStatus = this.getEmploymentStatus();
        result = result * 59 + ($employmentStatus == null ? 43 : $employmentStatus.hashCode());
        String $resignReason = this.getResignReason();
        result = result * 59 + ($resignReason == null ? 43 : $resignReason.hashCode());
        LocalDate $resignDate = this.getResignDate();
        result = result * 59 + ($resignDate == null ? 43 : ((Object)$resignDate).hashCode());
        String $role = this.getRole();
        result = result * 59 + ($role == null ? 43 : $role.hashCode());
        String $remark = this.getRemark();
        result = result * 59 + ($remark == null ? 43 : $remark.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        return result;
    }

    public String toString() {
        return "StaffMaster(staffId=" + this.getStaffId() + ", storeId=" + this.getStoreId() + ", staffName=" + this.getStaffName() + ", staffAccount=" + this.getStaffAccount() + ", staffPassword=" + this.getStaffPassword() + ", staffGender=" + this.getStaffGender() + ", staffAge=" + this.getStaffAge() + ", staffPhone=" + this.getStaffPhone() + ", staffPosition=" + this.getStaffPosition() + ", department=" + this.getDepartment() + ", hireDate=" + String.valueOf(this.getHireDate()) + ", monthlySalary=" + String.valueOf(this.getMonthlySalary()) + ", idCard=" + this.getIdCard() + ", homeAddress=" + this.getHomeAddress() + ", emergencyContact=" + this.getEmergencyContact() + ", emergencyPhone=" + this.getEmergencyPhone() + ", employmentStatus=" + this.getEmploymentStatus() + ", resignReason=" + this.getResignReason() + ", resignDate=" + String.valueOf(this.getResignDate()) + ", role=" + this.getRole() + ", remark=" + this.getRemark() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ", updatedAt=" + String.valueOf(this.getUpdatedAt()) + ", permissionLevel=" + this.getPermissionLevel() + ", deptId=" + this.getDeptId() + ", canManageKitchen=" + this.getCanManageKitchen() + ", canManageSales=" + this.getCanManageSales() + ", canManageFinance=" + this.getCanManageFinance() + ", canManageHr=" + this.getCanManageHr() + ", canViewAllStores=" + this.getCanViewAllStores() + ", canEditSystem=" + this.getCanEditSystem() + ")";
    }

    public StaffMaster() {
    }

    public StaffMaster(Integer staffId, Long storeId, String staffName, String staffAccount, String staffPassword, String staffGender, Integer staffAge, String staffPhone, String staffPosition, String department, LocalDate hireDate, BigDecimal monthlySalary, String idCard, String homeAddress, String emergencyContact, String emergencyPhone, String employmentStatus, String resignReason, LocalDate resignDate, String role, String remark, LocalDateTime createdAt, LocalDateTime updatedAt, Integer permissionLevel, Integer deptId, Integer canManageKitchen, Integer canManageSales, Integer canManageFinance, Integer canManageHr, Integer canViewAllStores, Integer canEditSystem) {
        this.staffId = staffId;
        this.storeId = storeId;
        this.staffName = staffName;
        this.staffAccount = staffAccount;
        this.staffPassword = staffPassword;
        this.staffGender = staffGender;
        this.staffAge = staffAge;
        this.staffPhone = staffPhone;
        this.staffPosition = staffPosition;
        this.department = department;
        this.hireDate = hireDate;
        this.monthlySalary = monthlySalary;
        this.idCard = idCard;
        this.homeAddress = homeAddress;
        this.emergencyContact = emergencyContact;
        this.emergencyPhone = emergencyPhone;
        this.employmentStatus = employmentStatus;
        this.resignReason = resignReason;
        this.resignDate = resignDate;
        this.role = role;
        this.remark = remark;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.permissionLevel = permissionLevel;
        this.deptId = deptId;
        this.canManageKitchen = canManageKitchen;
        this.canManageSales = canManageSales;
        this.canManageFinance = canManageFinance;
        this.canManageHr = canManageHr;
        this.canViewAllStores = canViewAllStores;
        this.canEditSystem = canEditSystem;
    }
}

