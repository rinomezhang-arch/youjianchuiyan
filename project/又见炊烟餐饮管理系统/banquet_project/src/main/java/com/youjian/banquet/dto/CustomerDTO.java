/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.CustomerDTO
 */
package com.youjian.banquet.dto;

import java.math.BigDecimal;

public class CustomerDTO {
    private String customerId;
    private String storeId;
    private String customerName;
    private String phone;
    private String gender;
    private String birthday;
    private String email;
    private String address;
    private String tags;
    private String notes;
    private Integer totalVisits;
    private BigDecimal totalSpent;
    private String status;

    public String getCustomerId() {
        return this.customerId;
    }

    public String getStoreId() {
        return this.storeId;
    }

    public String getCustomerName() {
        return this.customerName;
    }

    public String getPhone() {
        return this.phone;
    }

    public String getGender() {
        return this.gender;
    }

    public String getBirthday() {
        return this.birthday;
    }

    public String getEmail() {
        return this.email;
    }

    public String getAddress() {
        return this.address;
    }

    public String getTags() {
        return this.tags;
    }

    public String getNotes() {
        return this.notes;
    }

    public Integer getTotalVisits() {
        return this.totalVisits;
    }

    public BigDecimal getTotalSpent() {
        return this.totalSpent;
    }

    public String getStatus() {
        return this.status;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setTotalVisits(Integer totalVisits) {
        this.totalVisits = totalVisits;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CustomerDTO)) {
            return false;
        }
        CustomerDTO other = (CustomerDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$totalVisits = this.getTotalVisits();
        Integer other$totalVisits = other.getTotalVisits();
        if (this$totalVisits == null ? other$totalVisits != null : !((Object)this$totalVisits).equals(other$totalVisits)) {
            return false;
        }
        String this$customerId = this.getCustomerId();
        String other$customerId = other.getCustomerId();
        if (this$customerId == null ? other$customerId != null : !this$customerId.equals(other$customerId)) {
            return false;
        }
        String this$storeId = this.getStoreId();
        String other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) {
            return false;
        }
        String this$customerName = this.getCustomerName();
        String other$customerName = other.getCustomerName();
        if (this$customerName == null ? other$customerName != null : !this$customerName.equals(other$customerName)) {
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
        String this$birthday = this.getBirthday();
        String other$birthday = other.getBirthday();
        if (this$birthday == null ? other$birthday != null : !this$birthday.equals(other$birthday)) {
            return false;
        }
        String this$email = this.getEmail();
        String other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) {
            return false;
        }
        String this$address = this.getAddress();
        String other$address = other.getAddress();
        if (this$address == null ? other$address != null : !this$address.equals(other$address)) {
            return false;
        }
        String this$tags = this.getTags();
        String other$tags = other.getTags();
        if (this$tags == null ? other$tags != null : !this$tags.equals(other$tags)) {
            return false;
        }
        String this$notes = this.getNotes();
        String other$notes = other.getNotes();
        if (this$notes == null ? other$notes != null : !this$notes.equals(other$notes)) {
            return false;
        }
        BigDecimal this$totalSpent = this.getTotalSpent();
        BigDecimal other$totalSpent = other.getTotalSpent();
        if (this$totalSpent == null ? other$totalSpent != null : !((Object)this$totalSpent).equals(other$totalSpent)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        return !(this$status == null ? other$status != null : !this$status.equals(other$status));
    }

    protected boolean canEqual(Object other) {
        return other instanceof CustomerDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $totalVisits = this.getTotalVisits();
        result = result * 59 + ($totalVisits == null ? 43 : ((Object)$totalVisits).hashCode());
        String $customerId = this.getCustomerId();
        result = result * 59 + ($customerId == null ? 43 : $customerId.hashCode());
        String $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : $storeId.hashCode());
        String $customerName = this.getCustomerName();
        result = result * 59 + ($customerName == null ? 43 : $customerName.hashCode());
        String $phone = this.getPhone();
        result = result * 59 + ($phone == null ? 43 : $phone.hashCode());
        String $gender = this.getGender();
        result = result * 59 + ($gender == null ? 43 : $gender.hashCode());
        String $birthday = this.getBirthday();
        result = result * 59 + ($birthday == null ? 43 : $birthday.hashCode());
        String $email = this.getEmail();
        result = result * 59 + ($email == null ? 43 : $email.hashCode());
        String $address = this.getAddress();
        result = result * 59 + ($address == null ? 43 : $address.hashCode());
        String $tags = this.getTags();
        result = result * 59 + ($tags == null ? 43 : $tags.hashCode());
        String $notes = this.getNotes();
        result = result * 59 + ($notes == null ? 43 : $notes.hashCode());
        BigDecimal $totalSpent = this.getTotalSpent();
        result = result * 59 + ($totalSpent == null ? 43 : ((Object)$totalSpent).hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        return result;
    }

    public String toString() {
        return "CustomerDTO(customerId=" + this.getCustomerId() + ", storeId=" + this.getStoreId() + ", customerName=" + this.getCustomerName() + ", phone=" + this.getPhone() + ", gender=" + this.getGender() + ", birthday=" + this.getBirthday() + ", email=" + this.getEmail() + ", address=" + this.getAddress() + ", tags=" + this.getTags() + ", notes=" + this.getNotes() + ", totalVisits=" + this.getTotalVisits() + ", totalSpent=" + String.valueOf(this.getTotalSpent()) + ", status=" + this.getStatus() + ")";
    }

    public CustomerDTO() {
    }

    public CustomerDTO(String customerId, String storeId, String customerName, String phone, String gender, String birthday, String email, String address, String tags, String notes, Integer totalVisits, BigDecimal totalSpent, String status) {
        this.customerId = customerId;
        this.storeId = storeId;
        this.customerName = customerName;
        this.phone = phone;
        this.gender = gender;
        this.birthday = birthday;
        this.email = email;
        this.address = address;
        this.tags = tags;
        this.notes = notes;
        this.totalVisits = totalVisits;
        this.totalSpent = totalSpent;
        this.status = status;
    }
}

