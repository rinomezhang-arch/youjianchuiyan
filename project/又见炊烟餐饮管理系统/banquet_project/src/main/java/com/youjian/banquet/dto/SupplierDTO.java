/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.SupplierDTO
 */
package com.youjian.banquet.dto;

public class SupplierDTO {
    private String supplierId;
    private String storeId;
    private String supplierName;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;
    private String category;
    private String paymentTerms;
    private String status;
    private String notes;

    public String getSupplierId() {
        return this.supplierId;
    }

    public String getStoreId() {
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

    public String getNotes() {
        return this.notes;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public void setStoreId(String storeId) {
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

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SupplierDTO)) {
            return false;
        }
        SupplierDTO other = (SupplierDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        String this$supplierId = this.getSupplierId();
        String other$supplierId = other.getSupplierId();
        if (this$supplierId == null ? other$supplierId != null : !this$supplierId.equals(other$supplierId)) {
            return false;
        }
        String this$storeId = this.getStoreId();
        String other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) {
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
        return !(this$notes == null ? other$notes != null : !this$notes.equals(other$notes));
    }

    protected boolean canEqual(Object other) {
        return other instanceof SupplierDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $supplierId = this.getSupplierId();
        result = result * 59 + ($supplierId == null ? 43 : $supplierId.hashCode());
        String $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : $storeId.hashCode());
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
        return result;
    }

    public String toString() {
        return "SupplierDTO(supplierId=" + this.getSupplierId() + ", storeId=" + this.getStoreId() + ", supplierName=" + this.getSupplierName() + ", contactPerson=" + this.getContactPerson() + ", phone=" + this.getPhone() + ", email=" + this.getEmail() + ", address=" + this.getAddress() + ", category=" + this.getCategory() + ", paymentTerms=" + this.getPaymentTerms() + ", status=" + this.getStatus() + ", notes=" + this.getNotes() + ")";
    }

    public SupplierDTO() {
    }

    public SupplierDTO(String supplierId, String storeId, String supplierName, String contactPerson, String phone, String email, String address, String category, String paymentTerms, String status, String notes) {
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
    }
}

