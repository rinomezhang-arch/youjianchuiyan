/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.PackageDTO
 *  com.youjian.banquet.entity.PackageMaster
 *  com.youjian.banquet.repository.PackageMasterRepository
 *  com.youjian.banquet.service.PackageService
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.youjian.banquet.service;

import com.youjian.banquet.dto.PackageDTO;
import com.youjian.banquet.entity.PackageDishDetail;
import com.youjian.banquet.entity.PackageMaster;
import com.youjian.banquet.repository.PackageDishDetailRepository;
import com.youjian.banquet.repository.PackageMasterRepository;
import com.youjian.banquet.util.UserContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PackageService {
    @Autowired
    private PackageMasterRepository packageMasterRepository;
    @Autowired
    private PackageDishDetailRepository packageDishDetailRepository;

    public List<PackageDTO> getAllPackages(String storeId) {
        return this.packageMasterRepository.findByStoreIdOrderBySortOrderAsc(Long.valueOf(storeId)).stream().map(arg_0 -> this.toDTO(arg_0)).collect(Collectors.toList());
    }

    public PackageDTO getPackage(String packageId, String storeId) {
        return this.packageMasterRepository.findByPackageIdAndStoreId(packageId, Long.valueOf(storeId)).map(arg_0 -> this.toDTO(arg_0)).orElseThrow(() -> new IllegalArgumentException("Package not found: " + packageId));
    }

    @Transactional
    public PackageDTO createPackage(PackageDTO dto) {
        PackageMaster pkg = new PackageMaster();
        // 生成套餐编号：TC+日期+套餐体系序列号（如 TC20260730001，无横杠）
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 统计整个套餐体系的总数，序列号作为 sortOrder
        long totalCount = this.packageMasterRepository.count();
        int seqInt = (int)(totalCount + 1);
        String seqNum = String.format("%03d", seqInt);
        String packageId = "TC" + dateStr + seqNum;
        pkg.setPackageId(packageId);
        pkg.setStoreId(dto.getStoreId() != null ? Long.valueOf(dto.getStoreId()) : null);
        pkg.setPackageName(dto.getPackageName());
        // 已移除: 字段对齐数据库
        // 已移除: 字段对齐数据库
        pkg.setCategory(dto.getCategory());
        pkg.setPrice(dto.getPrice());
        pkg.setPackageTotalPrice(dto.getPackageTotalPrice());
        pkg.setOriginalPrice(dto.getOriginalPrice());
        // 已移除: 字段对齐数据库
        pkg.setDescription(dto.getDescription());
        pkg.setImageUrl(dto.getImageUrl());
        pkg.setMinGuests(dto.getMinGuests());
        pkg.setMaxGuests(dto.getMaxGuests());
        pkg.setStatus(dto.getStatus() != null ? dto.getStatus() : "active");
        pkg.setTags(dto.getTags());
        // sortOrder 自生成：= 套餐体系序列号（整个套餐体系中排第几，不可手工编辑）
        pkg.setSortOrder(Integer.valueOf(seqInt));
        // 已移除: 字段对齐数据库
        // 设置录入时间：优先使用前端传入的值（预览生成），否则由 @PrePersist 自动生成
        if (dto.getCreatedAt() != null && !dto.getCreatedAt().isEmpty()) {
            try {
                pkg.setCreatedAt(LocalDateTime.parse(dto.getCreatedAt(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } catch (Exception e) {
                // 解析失败则忽略，由 @PrePersist 自动生成
            }
        }
        this.packageMasterRepository.save(pkg);
        return this.toDTO(pkg);
    }

    @Transactional
    public PackageDTO updatePackage(String packageId, String storeId, PackageDTO dto) {
        PackageMaster pkg = (PackageMaster)this.packageMasterRepository.findByPackageIdAndStoreId(packageId, Long.valueOf(storeId)).orElseThrow(() -> new IllegalArgumentException("Package not found: " + packageId));
        if (dto.getPackageName() != null) {
            pkg.setPackageName(dto.getPackageName());
        }
        // 已移除: 字段对齐数据库
        // 已移除: 字段对齐数据库
        if (dto.getCategory() != null) {
            pkg.setCategory(dto.getCategory());
        }
        if (dto.getPrice() != null) {
            pkg.setPrice(dto.getPrice());
        }
        if (dto.getOriginalPrice() != null) {
            pkg.setOriginalPrice(dto.getOriginalPrice());
        }
        if (dto.getPackageTotalPrice() != null) {
            pkg.setPackageTotalPrice(dto.getPackageTotalPrice());
        }
        // 已移除: 字段对齐数据库
        if (dto.getDescription() != null) {
            pkg.setDescription(dto.getDescription());
        }
        if (dto.getImageUrl() != null) {
            pkg.setImageUrl(dto.getImageUrl());
        }
        if (dto.getMinGuests() != null) {
            pkg.setMinGuests(dto.getMinGuests());
        }
        if (dto.getMaxGuests() != null) {
            pkg.setMaxGuests(dto.getMaxGuests());
        }
        if (dto.getStatus() != null) {
            pkg.setStatus(dto.getStatus());
        }
        if (dto.getTags() != null) {
            pkg.setTags(dto.getTags());
        }
        if (dto.getSortOrder() != null) {
            pkg.setSortOrder(dto.getSortOrder());
        }
        // 已移除: 字段对齐数据库
        // 更新录入时间：前端传了值才更新
        if (dto.getCreatedAt() != null && !dto.getCreatedAt().isEmpty()) {
            try {
                pkg.setCreatedAt(LocalDateTime.parse(dto.getCreatedAt(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } catch (Exception e) {
                // 解析失败则忽略
            }
        }
        this.packageMasterRepository.save(pkg);
        return this.toDTO(pkg);
    }

    @Transactional
    public void deletePackage(String packageId, String storeId) {
        this.packageMasterRepository.deleteByPackageIdAndStoreId(packageId, Long.valueOf(storeId));
    }

    // 获取套餐菜品明细
    public List<PackageDishDetail> getPackageDishes(String packageId, Long storeId) {
        return this.packageDishDetailRepository.findByPackageIdAndStoreIdOrderByDishOrderAsc(packageId, storeId);
    }

    // 添加菜品到套餐
    @Transactional
    public PackageDishDetail addDishToPackage(String packageId, Long storeId, String dishId, Integer quantity, Integer order, String customName, String note) {
        PackageDishDetail detail = new PackageDishDetail();
        detail.setPackageId(packageId);
        detail.setStoreId(storeId);
        detail.setDishId(dishId);
        detail.setDishQuantity(quantity != null ? quantity : 1);
        detail.setDishOrder(order != null ? order : 0);
        detail.setCustomName(customName);
        detail.setNote(note);
        return this.packageDishDetailRepository.save(detail);
    }

    // 删除套餐中的菜品
    @Transactional
    public void removeDishFromPackage(String packageId, Long storeId, String dishId) {
        this.packageDishDetailRepository.deleteByPackageIdAndStoreIdAndDishId(packageId, storeId, dishId);
    }

    // 批量添加菜品到套餐
    @Transactional
    public List<PackageDishDetail> addDishesToPackage(String packageId, Long storeId, List<PackageDishDetail> details) {
        for (PackageDishDetail detail : details) {
            detail.setPackageId(packageId);
            detail.setStoreId(storeId);
            if (detail.getDishQuantity() == null) detail.setDishQuantity(1);
            if (detail.getDishOrder() == null) detail.setDishOrder(0);
        }
        return this.packageDishDetailRepository.saveAll(details);
    }

    private PackageDTO toDTO(PackageMaster e) {
        PackageDTO dto = new PackageDTO();
        dto.setPackageId(e.getPackageId());
        dto.setStoreId(e.getStoreId() != null ? String.valueOf(e.getStoreId()) : null);
        dto.setPackageName(e.getPackageName());
        // 已移除: 字段对齐数据库
        // 已移除: 字段对齐数据库
        dto.setCategory(e.getCategory());
        dto.setPrice(e.getPrice());
        dto.setPackageTotalPrice(e.getPackageTotalPrice());
        dto.setOriginalPrice(e.getOriginalPrice());
        // 已移除: 字段对齐数据库
        dto.setDescription(e.getDescription());
        dto.setImageUrl(e.getImageUrl());
        dto.setMinGuests(e.getMinGuests());
        dto.setMaxGuests(e.getMaxGuests());
        dto.setStatus(e.getStatus());
        dto.setTags(e.getTags());
        dto.setSortOrder(e.getSortOrder());
        // 已移除: 字段对齐数据库
        if (e.getCreatedAt() != null) {
            dto.setCreatedAt(e.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        return dto;
    }
}

