package com.youjian.banquet.repository;

import com.youjian.banquet.entity.SalaryTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryTemplateRepository extends JpaRepository<SalaryTemplate, Long> {

    List<SalaryTemplate> findByStoreId(Long storeId);

    List<SalaryTemplate> findByStoreIdAndIsActive(Long storeId, Integer isActive);

    Optional<SalaryTemplate> findByStoreIdAndPostNameAndIsActive(Long storeId, String postName, Integer isActive);
}
