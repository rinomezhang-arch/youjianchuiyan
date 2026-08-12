package com.youjian.banquet.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.entity.BanquetNotice;
import com.youjian.banquet.repository.BanquetNoticeRepository;
import com.youjian.banquet.util.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BanquetNoticeService {
    private static final Set<String> STATUSES = Set.of("draft", "published", "confirmed", "returned", "archived");
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "draft", Set.of("published"),
            "published", Set.of("confirmed"),
            "confirmed", Set.of("returned"),
            "returned", Set.of("archived"),
            "archived", Set.of()
    );

    private final BanquetNoticeRepository repository;
    private final ObjectMapper objectMapper;

    public BanquetNoticeService(BanquetNoticeRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public List<BanquetNotice> search(Long requestedStoreId, String keyword, String status,
                                      LocalDate startDate, LocalDate endDate) {
        Long storeId = resolveStore(requestedStoreId);
        return repository.search(storeId, keyword, status, startDate, endDate);
    }

    public BanquetNotice get(Long id, Long requestedStoreId) {
        Long storeId = resolveStore(requestedStoreId);
        return repository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new IllegalArgumentException("宴会通知单不存在或无权访问"));
    }

    @Transactional
    public BanquetNotice create(BanquetNotice input) {
        Long storeId = resolveStore(input.getStoreId());
        validate(input);
        input.setId(null);
        input.setStoreId(storeId);
        input.setNoticeNo(generateNoticeNo(storeId));
        input.setStatus("draft");
        input.setScanUrl(null);
        input.setScanName(null);
        input.setCreatedBy(UserContext.getStaffId());
        input.setUpdatedBy(UserContext.getStaffId());
        return repository.save(input);
    }

    @Transactional
    public BanquetNotice update(Long id, BanquetNotice input) {
        BanquetNotice existing = get(id, input.getStoreId());
        if (!"draft".equals(existing.getStatus())) {
            throw new IllegalStateException("仅草稿状态允许编辑；请复制后重新发布");
        }
        validate(input);
        existing.setBookingId(input.getBookingId());
        existing.setBanquetDate(input.getBanquetDate());
        existing.setBanquetTime(input.getBanquetTime());
        existing.setLocation(input.getLocation());
        existing.setReservedQuantity(input.getReservedQuantity());
        existing.setBanquetType(input.getBanquetType());
        existing.setCustomerName(input.getCustomerName());
        existing.setCustomerPhone(input.getCustomerPhone());
        existing.setMenuContent(input.getMenuContent());
        existing.setDepartmentItems(input.getDepartmentItems());
        existing.setUpdatedBy(UserContext.getStaffId());
        return repository.save(existing);
    }

    @Transactional
    public BanquetNotice transition(Long id, Long requestedStoreId, String targetStatus) {
        if (!STATUSES.contains(targetStatus)) throw new IllegalArgumentException("非法状态");
        BanquetNotice notice = get(id, requestedStoreId);
        if (!TRANSITIONS.getOrDefault(notice.getStatus(), Set.of()).contains(targetStatus)) {
            throw new IllegalStateException("当前状态不能流转为" + targetStatus);
        }
        if ("archived".equals(targetStatus) && (notice.getScanUrl() == null || notice.getScanUrl().isBlank())) {
            throw new IllegalStateException("归档前必须上传纸质签字扫描件");
        }
        notice.setStatus(targetStatus);
        if ("returned".equals(targetStatus)) notice.setReturnedAt(LocalDateTime.now());
        if ("archived".equals(targetStatus)) notice.setArchivedAt(LocalDateTime.now());
        notice.setUpdatedBy(UserContext.getStaffId());
        return repository.save(notice);
    }

    @Transactional
    public BanquetNotice attachScan(Long id, Long requestedStoreId, String url, String name) {
        if (url == null || url.isBlank()) throw new IllegalArgumentException("扫描件地址不能为空");
        BanquetNotice notice = get(id, requestedStoreId);
        if ("draft".equals(notice.getStatus())) throw new IllegalStateException("草稿不能上传归档扫描件");
        notice.setScanUrl(url.trim());
        notice.setScanName(name == null ? "签字扫描件" : name.trim());
        notice.setUpdatedBy(UserContext.getStaffId());
        return repository.save(notice);
    }

    @Transactional
    public BanquetNotice copy(Long id, Long requestedStoreId) {
        BanquetNotice source = get(id, requestedStoreId);
        BanquetNotice copy = new BanquetNotice();
        copy.setStoreId(source.getStoreId());
        copy.setBookingId(source.getBookingId());
        copy.setBanquetDate(source.getBanquetDate());
        copy.setBanquetTime(source.getBanquetTime());
        copy.setLocation(source.getLocation());
        copy.setReservedQuantity(source.getReservedQuantity());
        copy.setBanquetType(source.getBanquetType());
        copy.setCustomerName(source.getCustomerName());
        copy.setCustomerPhone(source.getCustomerPhone());
        copy.setMenuContent(source.getMenuContent());
        copy.setDepartmentItems(source.getDepartmentItems());
        return create(copy);
    }

    private Long resolveStore(Long requestedStoreId) {
        UserContext.ensureDataScopeFromStoreId();
        if (UserContext.isGeneralManager()) {
            if (requestedStoreId == null || requestedStoreId <= 0) throw new IllegalArgumentException("请选择门店");
            return requestedStoreId;
        }
        Long current = UserContext.getStoreId();
        if (current == null || current <= 0) throw new SecurityException("未识别当前用户门店");
        return current;
    }

    private void validate(BanquetNotice notice) {
        if (notice.getBanquetDate() == null) throw new IllegalArgumentException("宴会日期不能为空");
        if (notice.getLocation() == null || notice.getLocation().isBlank()) throw new IllegalArgumentException("宴会地点不能为空");
        if (notice.getReservedQuantity() == null || notice.getReservedQuantity() <= 0) throw new IllegalArgumentException("预定数量必须大于0");
        if (notice.getBanquetType() == null || notice.getBanquetType().isBlank()) throw new IllegalArgumentException("宴会性质不能为空");
        try {
            List<Map<String, Object>> items = objectMapper.readValue(notice.getDepartmentItems(), new TypeReference<>() {});
            if (items.isEmpty()) throw new IllegalArgumentException("至少需要一个部门事项");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("部门事项格式错误");
        }
    }

    private String generateNoticeNo(Long storeId) {
        return "YN" + storeId + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(10, 100);
    }
}
