/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.StaffDTO
 *  com.youjian.banquet.entity.StaffMaster
 *  com.youjian.banquet.repository.StaffMasterRepository
 *  com.youjian.banquet.service.StaffService
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.youjian.banquet.service;

import com.youjian.banquet.dto.StaffDTO;
import com.youjian.banquet.entity.StaffMaster;
import com.youjian.banquet.repository.StaffMasterRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffService {
    @Autowired
    private StaffMasterRepository staffMasterRepository;

    public List<StaffDTO> getAllStaff(String storeId) {
        return this.staffMasterRepository.findByStoreId(Long.valueOf(Long.parseLong(storeId))).stream().map(arg_0 -> this.toDTO(arg_0)).collect(Collectors.toList());
    }

    public StaffDTO getStaff(String staffId, String storeId) {
        return this.staffMasterRepository.findByStaffIdAndStoreId(Integer.valueOf(Integer.parseInt(staffId)), Long.valueOf(Long.parseLong(storeId))).map(arg_0 -> this.toDTO(arg_0)).orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));
    }

    @Transactional
    public StaffDTO createStaff(StaffDTO dto) {
        StaffMaster staff = new StaffMaster();
        staff.setStoreId(Long.valueOf(Long.parseLong(dto.getStoreId())));
        staff.setStaffName(dto.getStaffName());
        staff.setStaffPhone(dto.getPhone());
        staff.setStaffGender(dto.getGender());
        staff.setRole(dto.getRole());
        staff.setStaffPosition(dto.getPosition());
        if (dto.getHireDate() != null) {
            staff.setHireDate(LocalDate.parse(dto.getHireDate()));
        }
        staff.setMonthlySalary(dto.getSalary());
        staff.setEmploymentStatus(dto.getStatus() != null ? dto.getStatus() : "active");
        staff.setRemark(dto.getNotes());
        this.staffMasterRepository.save(staff);
        return this.toDTO(staff);
    }

    @Transactional
    public StaffDTO updateStaff(String staffId, String storeId, StaffDTO dto) {
        StaffMaster staff = (StaffMaster)this.staffMasterRepository.findByStaffIdAndStoreId(Integer.valueOf(Integer.parseInt(staffId)), Long.valueOf(Long.parseLong(storeId))).orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));
        if (dto.getStaffName() != null) {
            staff.setStaffName(dto.getStaffName());
        }
        if (dto.getPhone() != null) {
            staff.setStaffPhone(dto.getPhone());
        }
        if (dto.getGender() != null) {
            staff.setStaffGender(dto.getGender());
        }
        if (dto.getRole() != null) {
            staff.setRole(dto.getRole());
        }
        if (dto.getPosition() != null) {
            staff.setStaffPosition(dto.getPosition());
        }
        if (dto.getHireDate() != null) {
            staff.setHireDate(LocalDate.parse(dto.getHireDate()));
        }
        if (dto.getSalary() != null) {
            staff.setMonthlySalary(dto.getSalary());
        }
        if (dto.getStatus() != null) {
            staff.setEmploymentStatus(dto.getStatus());
        }
        if (dto.getNotes() != null) {
            staff.setRemark(dto.getNotes());
        }
        this.staffMasterRepository.save(staff);
        return this.toDTO(staff);
    }

    @Transactional
    public void deleteStaff(String staffId, String storeId) {
        this.staffMasterRepository.findByStaffIdAndStoreId(Integer.valueOf(Integer.parseInt(staffId)), Long.valueOf(Long.parseLong(storeId))).ifPresent(arg_0 -> ((StaffMasterRepository)this.staffMasterRepository).delete(arg_0));
    }

    private StaffDTO toDTO(StaffMaster e) {
        StaffDTO dto = new StaffDTO();
        dto.setStaffId(String.valueOf(e.getStaffId()));
        dto.setStoreId(String.valueOf(e.getStoreId()));
        dto.setStaffName(e.getStaffName());
        dto.setPhone(e.getStaffPhone());
        dto.setGender(e.getStaffGender());
        dto.setRole(e.getRole());
        dto.setPosition(e.getStaffPosition());
        dto.setHireDate(e.getHireDate() != null ? e.getHireDate().toString() : null);
        dto.setSalary(e.getMonthlySalary());
        dto.setStatus(e.getEmploymentStatus());
        dto.setNotes(e.getRemark());
        return dto;
    }
}

