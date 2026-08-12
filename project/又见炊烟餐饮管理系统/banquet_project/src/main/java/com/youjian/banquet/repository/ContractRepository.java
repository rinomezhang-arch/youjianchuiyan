package com.youjian.banquet.repository;

import com.youjian.banquet.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    List<Contract> findByStoreId(Long storeId);

    List<Contract> findByStaffId(Long staffId);

    List<Contract> findByStaffIdAndStatus(Long staffId, Integer status);

    List<Contract> findByStoreIdAndStatus(Long storeId, Integer status);

    /**
     * 合同到期预警：查询指定时间段内到期、且状态为有效的合同
     */
    List<Contract> findByEndDateBetweenAndStatus(LocalDate start, LocalDate end, Integer status);
}
