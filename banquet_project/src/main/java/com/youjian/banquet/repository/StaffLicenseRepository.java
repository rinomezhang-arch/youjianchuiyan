package com.youjian.banquet.repository;

import com.youjian.banquet.entity.StaffLicense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffLicenseRepository extends JpaRepository<StaffLicense, Long> {
}
