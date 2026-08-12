package com.youjian.banquet.service;

import com.youjian.banquet.entity.Contract;
import com.youjian.banquet.repository.ContractRepository;
import com.youjian.banquet.repository.StaffMasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 劳动合同服务
 * 对应规划手册 5.txt - 合同模块
 */
@Service
public class ContractService {

    @Autowired private ContractRepository contractRepo;
    @Autowired private StaffMasterRepository staffRepo;

    /** 列表查询 */
    public List<Contract> list(Long storeId, Integer status) {
        if (status != null) {
            return contractRepo.findByStoreIdAndStatus(storeId, status);
        }
        return contractRepo.findByStoreId(storeId);
    }

    /** 详情 */
    public Contract get(Long contractId) {
        return contractRepo.findById(contractId).orElse(null);
    }

    /** 新增 */
    @Transactional
    public Contract create(Contract contract) {
        return contractRepo.save(contract);
    }

    /** 更新 */
    @Transactional
    public Contract update(Long contractId, Contract dto) {
        Contract existing = contractRepo.findById(contractId).orElse(null);
        if (existing == null) {
            throw new RuntimeException("合同不存在: " + contractId);
        }
        dto.setContractId(contractId);
        return contractRepo.save(dto);
    }

    /** 删除 */
    @Transactional
    public void delete(Long contractId) {
        contractRepo.deleteById(contractId);
    }

    /**
     * 合同到期预警
     * @param days 多少天内到期
     */
    public List<Contract> getExpiringContracts(Long storeId, int days) {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(days);
        // status=1 表示有效
        return contractRepo.findByEndDateBetweenAndStatus(today, deadline, 1);
    }
}
