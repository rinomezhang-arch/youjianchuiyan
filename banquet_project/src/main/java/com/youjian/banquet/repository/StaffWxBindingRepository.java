package com.youjian.banquet.repository;

import com.youjian.banquet.entity.StaffWxBinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffWxBindingRepository extends JpaRepository<StaffWxBinding, Long> {
    Optional<StaffWxBinding> findByOpenId(String openId);
}
