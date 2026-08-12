/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.EmployeeLifecycle
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 */
package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="employee_lifecycle")
public class EmployeeLifecycle {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Integer id;
    @Column(name="emp_id")
    private String empId;
    @Column(name="emp_name")
    private String empName;
    @Column(name="event_type")
    private String eventType;
    @Column(name="event_date")
    private LocalDate eventDate;
    @Column(name="create_time")
    private LocalDateTime createdAt;

    public Integer getId() {
        return this.id;
    }

    public String getEmpId() {
        return this.empId;
    }

    public String getEmpName() {
        return this.empName;
    }

    public String getEventType() {
        return this.eventType;
    }

    public LocalDate getEventDate() {
        return this.eventDate;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EmployeeLifecycle)) {
            return false;
        }
        EmployeeLifecycle other = (EmployeeLifecycle)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$id = this.getId();
        Integer other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        String this$empId = this.getEmpId();
        String other$empId = other.getEmpId();
        if (this$empId == null ? other$empId != null : !this$empId.equals(other$empId)) {
            return false;
        }
        String this$empName = this.getEmpName();
        String other$empName = other.getEmpName();
        if (this$empName == null ? other$empName != null : !this$empName.equals(other$empName)) {
            return false;
        }
        String this$eventType = this.getEventType();
        String other$eventType = other.getEventType();
        if (this$eventType == null ? other$eventType != null : !this$eventType.equals(other$eventType)) {
            return false;
        }
        LocalDate this$eventDate = this.getEventDate();
        LocalDate other$eventDate = other.getEventDate();
        if (this$eventDate == null ? other$eventDate != null : !((Object)this$eventDate).equals(other$eventDate)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof EmployeeLifecycle;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        String $empId = this.getEmpId();
        result = result * 59 + ($empId == null ? 43 : $empId.hashCode());
        String $empName = this.getEmpName();
        result = result * 59 + ($empName == null ? 43 : $empName.hashCode());
        String $eventType = this.getEventType();
        result = result * 59 + ($eventType == null ? 43 : $eventType.hashCode());
        LocalDate $eventDate = this.getEventDate();
        result = result * 59 + ($eventDate == null ? 43 : ((Object)$eventDate).hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "EmployeeLifecycle(id=" + this.getId() + ", empId=" + this.getEmpId() + ", empName=" + this.getEmpName() + ", eventType=" + this.getEventType() + ", eventDate=" + String.valueOf(this.getEventDate()) + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ")";
    }

    public EmployeeLifecycle() {
    }

    public EmployeeLifecycle(Integer id, String empId, String empName, String eventType, LocalDate eventDate, LocalDateTime createdAt) {
        this.id = id;
        this.empId = empId;
        this.empName = empName;
        this.eventType = eventType;
        this.eventDate = eventDate;
        this.createdAt = createdAt;
    }
}

