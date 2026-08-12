package com.youjian.banquet.service;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrSocCity;
import com.youjian.banquet.entity.HrSocInsurance;
import com.youjian.banquet.entity.StaffMaster;
import com.youjian.banquet.repository.HrSocCityRepository;
import com.youjian.banquet.repository.HrSocInsuranceRepository;
import com.youjian.banquet.repository.StaffMasterRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 员工五险一金服务 (HR系统复刻)
 * 来源: HR系统 InsuranceService
 * 功能: 五险一金计算（养老/医疗/失业/工伤/生育+公积金）、CRUD、分页查询
 *
 * 计算逻辑:
 * 1. 根据参保城市配置的缴费比例，分别计算个人和企业应缴金额
 * 2. 社保基数 = 员工工资，限制在 [soc_lower_limit, soc_upper_limit] 范围内
 * 3. 公积金基数 = 员工工资，限制在 [hou_lower_limit, hou_upper_limit] 范围内
 * 4. 个人社保 = 养老(per_pension) + 医疗(per_medical) + 失业(per_unemployment)
 * 5. 企业社保 = 养老(com_pension) + 医疗(com_medical) + 失业(com_unemployment) + 生育(com_maternity) + 工伤(com_injury)
 * 6. 公积金 = 基数 * 比例
 */
@Service
public class HrSocInsuranceService {

    @Autowired
    private HrSocInsuranceRepository insuranceRepository;

    @Autowired
    private HrSocCityRepository cityRepository;

    @Autowired
    private StaffMasterRepository staffMasterRepository;

    // ==================== 基础CRUD ====================

    /**
     * 新增五险一金记录
     */
    @Transactional
    public Result<HrSocInsurance> add(HrSocInsurance insurance) {
        HrSocInsurance saved = insuranceRepository.save(insurance);
        return Result.success(saved);
    }

    /**
     * 根据ID逻辑删除（软删除）
     */
    @Transactional
    public Result<String> deleteById(Integer id) {
        Optional<HrSocInsurance> opt = insuranceRepository.findById(id);
        if (opt.isPresent()) {
            HrSocInsurance insurance = opt.get();
            insurance.setIsDeleted(1);
            insuranceRepository.save(insurance);
            return Result.success("删除成功");
        }
        return Result.error(404, "记录不存在");
    }

    /**
     * 批量逻辑删除
     */
    @Transactional
    public Result<String> deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            Optional<HrSocInsurance> opt = insuranceRepository.findById(id);
            if (opt.isPresent()) {
                HrSocInsurance insurance = opt.get();
                insurance.setIsDeleted(1);
                insuranceRepository.save(insurance);
            }
        }
        return Result.success("批量删除成功");
    }

    /**
     * 编辑更新
     */
    @Transactional
    public Result<HrSocInsurance> edit(HrSocInsurance insurance) {
        if (insurance.getId() == null || !insuranceRepository.existsById(insurance.getId())) {
            return Result.error(404, "记录不存在");
        }
        HrSocInsurance updated = insuranceRepository.save(insurance);
        return Result.success(updated);
    }

    /**
     * 根据ID查询
     */
    public Result<HrSocInsurance> findById(Integer id) {
        return insuranceRepository.findById(id)
                .map(Result::success)
                .orElse(Result.error(404, "记录不存在"));
    }

    /**
     * 根据员工ID查询五险一金记录（返回最近一条，未删除）
     */
    public Result<HrSocInsurance> findByStaffId(Long storeId, Integer staffId) {
        List<HrSocInsurance> list = insuranceRepository.findByStoreIdAndStaffIdAndIsDeleted(storeId, staffId, 0);
        if (list != null && !list.isEmpty()) {
            return Result.success(list.get(0));
        }
        return Result.error(404, "该员工暂无五险一金记录");
    }

    /**
     * 查询员工所有五险一金记录（按缴纳月份，未删除）
     */
    public Result<List<HrSocInsurance>> listByStaff(Long storeId, Integer staffId) {
        return Result.success(insuranceRepository.findByStoreIdAndStaffIdAndIsDeleted(storeId, staffId, 0));
    }

    /**
     * 按月份查询五险一金记录（未删除）
     */
    public Result<List<HrSocInsurance>> listByMonth(Long storeId, String payMonth) {
        return Result.success(insuranceRepository.findByStoreIdAndPayMonthAndIsDeleted(storeId, payMonth, 0));
    }

    // ==================== 五险一金计算核心逻辑 ====================

    /**
     * 为员工设置/计算五险一金（核心方法）
     * 如果该员工已存在记录则更新，否则新增
     */
    @Transactional
    public Result<HrSocInsurance> setInsurance(HrSocInsurance insurance) {
        // 检查是否存在该员工的记录（未删除）
        Optional<HrSocInsurance> existing = insuranceRepository
                .findByStoreIdAndStaffIdAndCityIdAndIsDeleted(insurance.getStoreId(), insurance.getStaffId(), insurance.getCityId(), 0);

        if (existing.isPresent()) {
            // 更新已有记录
            HrSocInsurance exist = existing.get();
            insurance.setId(exist.getId());
        }

        // 执行五险一金计算
        HrSocInsurance calculated = calculateInsurance(insurance);
        HrSocInsurance saved = insuranceRepository.save(calculated);
        return Result.success(saved);
    }

    /**
     * 根据城市配置和员工基数计算五险一金
     * 完整保留HR系统的计算逻辑:
     * - 养老: 个人 + 企业
     * - 医疗: 个人 + 企业
     * - 失业: 个人 + 企业
     * - 工伤: 仅企业
     * - 生育: 仅企业
     * - 公积金: 个人 + 企业
     */
    @Transactional
    public Result<HrSocInsurance> calculateAndSave(HrSocInsurance insurance) {
        HrSocInsurance calculated = calculateInsurance(insurance);
        HrSocInsurance saved = insuranceRepository.save(calculated);
        return Result.success(saved);
    }

    /**
     * 五险一金计算核心算法
     * 来自HR系统 InsuranceService.setInsurance 的完整逻辑
     */
    private HrSocInsurance calculateInsurance(HrSocInsurance insurance) {
        HrSocCity city = cityRepository.findById(insurance.getCityId())
                .orElseThrow(() -> new RuntimeException("参保城市不存在，cityId=" + insurance.getCityId()));

        // ---------- 社保基数计算 ----------
        BigDecimal socialBase = insurance.getSocialBase();
        if (socialBase == null) {
            // 如果未指定基数，使用城市平均工资
            socialBase = city.getAverageSalary() != null ? city.getAverageSalary() : BigDecimal.ZERO;
        }
        // 限制在社保上下限范围内
        if (city.getSocLowerLimit() != null && socialBase.compareTo(city.getSocLowerLimit()) < 0) {
            socialBase = city.getSocLowerLimit();
        }
        if (city.getSocUpperLimit() != null && socialBase.compareTo(city.getSocUpperLimit()) > 0) {
            socialBase = city.getSocUpperLimit();
        }
        insurance.setSocialBase(socialBase);

        // ---------- 公积金基数计算 ----------
        BigDecimal houseBase = insurance.getHouseBase();
        if (houseBase == null) {
            houseBase = socialBase;
        }
        // 限制在公积金上下限范围内
        if (city.getHouLowerLimit() != null && houseBase.compareTo(city.getHouLowerLimit()) < 0) {
            houseBase = city.getHouLowerLimit();
        }
        if (city.getHouUpperLimit() != null && houseBase.compareTo(city.getHouUpperLimit()) > 0) {
            houseBase = city.getHouUpperLimit();
        }
        insurance.setHouseBase(houseBase);

        // ---------- 社保个人部分计算 ----------
        BigDecimal perPension = safeMultiply(socialBase, city.getPerPensionRate());
        BigDecimal perMedical = safeMultiply(socialBase, city.getPerMedicalRate());
        BigDecimal perUnemployment = safeMultiply(socialBase, city.getPerUnemploymentRate());
        BigDecimal perSocialPay = perPension.add(perMedical).add(perUnemployment);
        insurance.setPerSocialPay(perSocialPay);

        // ---------- 社保企业部分计算 ----------
        BigDecimal comPension = safeMultiply(socialBase, city.getComPensionRate());
        BigDecimal comMedical = safeMultiply(socialBase, city.getComMedicalRate());
        BigDecimal comUnemployment = safeMultiply(socialBase, city.getComUnemploymentRate());
        BigDecimal comMaternity = safeMultiply(socialBase, city.getComMaternityRate());
        BigDecimal comInjury = safeMultiply(socialBase, city.getComInjuryRate());
        insurance.setComInjuryRate(city.getComInjuryRate());
        BigDecimal comSocialPay = comPension.add(comMedical).add(comUnemployment).add(comMaternity).add(comInjury);
        insurance.setComSocialPay(comSocialPay);

        // ---------- 公积金计算 ----------
        BigDecimal perHouseRate = insurance.getPerHouseRate() != null ? insurance.getPerHouseRate() : BigDecimal.valueOf(0.07);
        BigDecimal comHouseRate = insurance.getComHouseRate() != null ? insurance.getComHouseRate() : BigDecimal.valueOf(0.07);
        insurance.setPerHouseRate(perHouseRate);
        insurance.setComHouseRate(comHouseRate);
        insurance.setPerHousePay(safeMultiply(houseBase, perHouseRate));
        insurance.setComHousePay(safeMultiply(houseBase, comHouseRate));

        if (insurance.getStatus() == null) {
            insurance.setStatus(0);
        }

        return insurance;
    }

    /**
     * 更新支付状态
     */
    @Transactional
    public Result<String> updateStatus(Integer id, Integer status) {
        return insuranceRepository.findById(id).map(i -> {
            i.setStatus(status);
            insuranceRepository.save(i);
            return Result.success("状态更新成功");
        }).orElse(Result.error(404, "记录不存在"));
    }

    // ==================== 分页查询（关联员工和城市信息） ====================

    /**
     * 多条件分页查询（过滤已删除）
     */
    public Result<Map<String, Object>> list(Integer current, Integer size, Long storeId,
                                            Integer deptId, String staffName) {
        PageRequest pageRequest = PageRequest.of(current - 1, size, Sort.by(Sort.Direction.DESC, "id"));

        Specification<HrSocInsurance> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("storeId"), storeId));
            predicates.add(cb.equal(root.get("isDeleted"), 0));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<HrSocInsurance> page = insuranceRepository.findAll(spec, pageRequest);

        Map<String, Object> map = new HashMap<>();
        map.put("pages", page.getTotalPages());
        map.put("total", page.getTotalElements());
        map.put("list", page.getContent());
        return Result.success(map);
    }

    /**
     * 分页查询（按storeId），支持按员工姓名模糊搜索（过滤已删除）
     */
    public Result<Map<String, Object>> listByStore(Integer current, Integer size, Long storeId, String staffName) {
        PageRequest pageRequest = PageRequest.of(current - 1, size, Sort.by(Sort.Direction.DESC, "id"));

        Page<HrSocInsurance> page;
        if (StringUtils.hasText(staffName)) {
            List<StaffMaster> staffList = staffMasterRepository.searchByKeyword(storeId, staffName);
            List<Integer> staffIds = staffList.stream().map(StaffMaster::getStaffId).toList();

            if (staffIds.isEmpty()) {
                Map<String, Object> emptyMap = new HashMap<>();
                emptyMap.put("pages", 0);
                emptyMap.put("total", 0L);
                emptyMap.put("list", List.of());
                return Result.success(emptyMap);
            }

            Specification<HrSocInsurance> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("storeId"), storeId));
                predicates.add(cb.equal(root.get("isDeleted"), 0));
                predicates.add(root.get("staffId").in(staffIds));
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            page = insuranceRepository.findAll(spec, pageRequest);
        } else {
            Specification<HrSocInsurance> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("storeId"), storeId));
                predicates.add(cb.equal(root.get("isDeleted"), 0));
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            page = insuranceRepository.findAll(spec, pageRequest);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("pages", page.getTotalPages());
        map.put("total", page.getTotalElements());
        map.put("list", page.getContent());
        return Result.success(map);
    }

    // ==================== 工具方法 ====================

    /**
     * BigDecimal 安全乘法，保留2位小数，四舍五入
     */
    private BigDecimal safeMultiply(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return BigDecimal.ZERO;
        return a.multiply(b).setScale(2, RoundingMode.HALF_UP);
    }
}