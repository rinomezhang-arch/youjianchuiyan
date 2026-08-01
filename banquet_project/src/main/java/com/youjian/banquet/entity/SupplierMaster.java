/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.SupplierMaster
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
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.youjian.banquet.config.BankAccountConverter;
import java.time.LocalDateTime;

@Entity
@Table(name="supplier_master")
public class SupplierMaster {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="supplier_id")
    private Long supplierId;
    @Column(name="store_id")
    private Long storeId;
    @Column(name="supplier_name")
    private String supplierName;
    @Column(name="contact_person")
    private String contactPerson;
    @Column(name="phone")
    private String phone;
    @Column(name="email")
    private String email;
    @Column(name="address", columnDefinition="TEXT")
    private String address;
    @Column(name="category")
    private String category;
    @Column(name="payment_terms")
    private String paymentTerms;
    @Column(name="status")
    private String status;
    @Column(name="bank_account")
    @Convert(converter = BankAccountConverter.class)
    @JsonSerialize(using = com.youjian.banquet.config.SensitiveDataSerializer.class)
    private String bankAccount;
    @Column(name="notes", columnDefinition="TEXT")
    private String notes;
    @Column(name="created_at")
    private LocalDateTime createdAt;
    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getSupplierId() {
        return this.supplierId;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getSupplierName() {
        return this.supplierName;
    }

    public String getContactPerson() {
        return this.contactPerson;
    }

    public String getPhone() {
        return this.phone;
    }

    public String getEmail() {
        return this.email;
    }

    public String getAddress() {
        return this.address;
    }

    public String getCategory() {
        return this.category;
    }

    public String getPaymentTerms() {
        return this.paymentTerms;
    }

    public String getStatus() {
        return this.status;
    }

    public String getBankAccount() {
        return this.bankAccount;
    }

    public String getNotes() {
        return this.notes;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SupplierMaster)) {
            return false;
        }
        SupplierMaster other = (SupplierMaster)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$supplierId = this.getSupplierId();
        Long other$supplierId = other.getSupplierId();
        if (this$supplierId == null ? other$supplierId != null : !((Object)this$supplierId).equals(other$supplierId)) {
            return false;
        }
        Long this$storeId = this.getStoreId();
        Long other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !((Object)this$storeId).equals(other$storeId)) {
            return false;
        }
        String this$supplierName = this.getSupplierName();
        String other$supplierName = other.getSupplierName();
        if (this$supplierName == null ? other$supplierName != null : !this$supplierName.equals(other$supplierName)) {
            return false;
        }
        String this$contactPerson = this.getContactPerson();
        String other$contactPerson = other.getContactPerson();
        if (this$contactPerson == null ? other$contactPerson != null : !this$contactPerson.equals(other$contactPerson)) {
            return false;
        }
        String this$phone = this.getPhone();
        String other$phone = other.getPhone();
        if (this$phone == null ? other$phone != null : !this$phone.equals(other$phone)) {
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
        String this$category = this.getCategory();
        String other$category = other.getCategory();
        if (this$category == null ? other$category != null : !this$category.equals(other$category)) {
            return false;
        }
        String this$paymentTerms = this.getPaymentTerms();
        String other$paymentTerms = other.getPaymentTerms();
        if (this$paymentTerms == null ? other$paymentTerms != null : !this$paymentTerms.equals(other$paymentTerms)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$notes = this.getNotes();
        String other$notes = other.getNotes();
        if (this$notes == null ? other$notes != null : !this$notes.equals(other$notes)) {
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
        return other instanceof SupplierMaster;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $supplierId = this.getSupplierId();
        result = result * 59 + ($supplierId == null ? 43 : ((Object)$supplierId).hashCode());
        Long $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : ((Object)$storeId).hashCode());
        String $supplierName = this.getSupplierName();
        result = result * 59 + ($supplierName == null ? 43 : $supplierName.hashCode());
        String $contactPerson = this.getContactPerson();
        result = result * 59 + ($contactPerson == null ? 43 : $contactPerson.hashCode());
        String $phone = this.getPhone();
        result = result * 59 + ($phone == null ? 43 : $phone.hashCode());
        String $email = this.getEmail();
        result = result * 59 + ($email == null ? 43 : $email.hashCode());
        String $address = this.getAddress();
        result = result * 59 + ($address == null ? 43 : $address.hashCode());
        String $category = this.getCategory();
        result = result * 59 + ($category == null ? 43 : $category.hashCode());
        String $paymentTerms = this.getPaymentTerms();
        result = result * 59 + ($paymentTerms == null ? 43 : $paymentTerms.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $notes = this.getNotes();
        result = result * 59 + ($notes == null ? 43 : $notes.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        return result;
    }

    public String toString() {
        return "SupplierMaster(supplierId=" + this.getSupplierId() + ", storeId=" + this.getStoreId() + ", supplierName=" + this.getSupplierName() + ", contactPerson=" + this.getContactPerson() + ", phone=" + this.getPhone() + ", email=" + this.getEmail() + ", address=" + this.getAddress() + ", category=" + this.getCategory() + ", paymentTerms=" + this.getPaymentTerms() + ", status=" + this.getStatus() + ", notes=" + this.getNotes() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ", updatedAt=" + String.valueOf(this.getUpdatedAt()) + ")";
    }

    public SupplierMaster() {
    }

    public SupplierMaster(Long supplierId, Long storeId, String supplierName, String contactPerson, String phone, String email, String address, String category, String paymentTerms, String status, String notes, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.supplierId = supplierId;
        this.storeId = storeId;
        this.supplierName = supplierName;
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.category = category;
        this.paymentTerms = paymentTerms;
        this.status = status;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

