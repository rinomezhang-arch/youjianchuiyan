/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.StaffDTO
 */
package com.youjian.banquet.dto;

import java.math.BigDecimal;

public class StaffDTO {
    private String staffId;
    private String storeId;
    private String staffName;
    private String phone;
    private String gender;
    private String role;
    private String position;
    private String hireDate;
    private BigDecimal salary;
    private String status;
    private String notes;

    public String getStaffId() {
        return this.staffId;
    }

    public String getStoreId() {
        return this.storeId;
    }

    public String getStaffName() {
        return this.staffName;
    }

    public String getPhone() {
        return this.phone;
    }

    public String getGender() {
        return this.gender;
    }

    public String getRole() {
        return this.role;
    }

    public String getPosition() {
        return this.position;
    }

    public String getHireDate() {
        return this.hireDate;
    }

    public BigDecimal getSalary() {
        return this.salary;
    }

    public String getStatus() {
        return this.status;
    }

    public String getNotes() {
        return this.notes;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public void setHireDate(String hireDate) {
        this.hireDate = hireDate;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StaffDTO)) {
            return false;
        }
        StaffDTO other = (StaffDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        String this$staffId = this.getStaffId();
        String other$staffId = other.getStaffId();
        if (this$staffId == null ? other$staffId != null : !this$staffId.equals(other$staffId)) {
            return false;
        }
        String this$storeId = this.getStoreId();
        String other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) {
            return false;
        }
        String this$staffName = this.getStaffName();
        String other$staffName = other.getStaffName();
        if (this$staffName == null ? other$staffName != null : !this$staffName.equals(other$staffName)) {
            return false;
        }
        String this$phone = this.getPhone();
        String other$phone = other.getPhone();
        if (this$phone == null ? other$phone != null : !this$phone.equals(other$phone)) {
            return false;
        }
        String this$gender = this.getGender();
        String other$gender = other.getGender();
        if (this$gender == null ? other$gender != null : !this$gender.equals(other$gender)) {
            return false;
        }
        String this$role = this.getRole();
        String other$role = other.getRole();
        if (this$role == null ? other$role != null : !this$role.equals(other$role)) {
            return false;
        }
        String this$position = this.getPosition();
        String other$position = other.getPosition();
        if (this$position == null ? other$position != null : !this$position.equals(other$position)) {
            return false;
        }
        String this$hireDate = this.getHireDate();
        String other$hireDate = other.getHireDate();
        if (this$hireDate == null ? other$hireDate != null : !this$hireDate.equals(other$hireDate)) {
            return false;
        }
        BigDecimal this$salary = this.getSalary();
        BigDecimal other$salary = other.getSalary();
        if (this$salary == null ? other$salary != null : !((Object)this$salary).equals(other$salary)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$notes = this.getNotes();
        String other$notes = other.getNotes();
        return !(this$notes == null ? other$notes != null : !this$notes.equals(other$notes));
    }

    protected boolean canEqual(Object other) {
        return other instanceof StaffDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $staffId = this.getStaffId();
        result = result * 59 + ($staffId == null ? 43 : $staffId.hashCode());
        String $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : $storeId.hashCode());
        String $staffName = this.getStaffName();
        result = result * 59 + ($staffName == null ? 43 : $staffName.hashCode());
        String $phone = this.getPhone();
        result = result * 59 + ($phone == null ? 43 : $phone.hashCode());
        String $gender = this.getGender();
        result = result * 59 + ($gender == null ? 43 : $gender.hashCode());
        String $role = this.getRole();
        result = result * 59 + ($role == null ? 43 : $role.hashCode());
        String $position = this.getPosition();
        result = result * 59 + ($position == null ? 43 : $position.hashCode());
        String $hireDate = this.getHireDate();
        result = result * 59 + ($hireDate == null ? 43 : $hireDate.hashCode());
        BigDecimal $salary = this.getSalary();
        result = result * 59 + ($salary == null ? 43 : ((Object)$salary).hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $notes = this.getNotes();
        result = result * 59 + ($notes == null ? 43 : $notes.hashCode());
        return result;
    }

    public String toString() {
        return "StaffDTO(staffId=" + this.getStaffId() + ", storeId=" + this.getStoreId() + ", staffName=" + this.getStaffName() + ", phone=" + this.getPhone() + ", gender=" + this.getGender() + ", role=" + this.getRole() + ", position=" + this.getPosition() + ", hireDate=" + this.getHireDate() + ", salary=" + String.valueOf(this.getSalary()) + ", status=" + this.getStatus() + ", notes=" + this.getNotes() + ")";
    }

    public StaffDTO() {
    }

    public StaffDTO(String staffId, String storeId, String staffName, String phone, String gender, String role, String position, String hireDate, BigDecimal salary, String status, String notes) {
        this.staffId = staffId;
        this.storeId = storeId;
        this.staffName = staffName;
        this.phone = phone;
        this.gender = gender;
        this.role = role;
        this.position = position;
        this.hireDate = hireDate;
        this.salary = salary;
        this.status = status;
        this.notes = notes;
    }
}

