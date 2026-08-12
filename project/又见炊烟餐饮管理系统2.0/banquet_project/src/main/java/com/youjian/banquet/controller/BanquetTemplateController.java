package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BanquetTemplate;
import com.youjian.banquet.entity.BanquetTemplateRel;
import com.youjian.banquet.entity.BanquetType;
import com.youjian.banquet.repository.BanquetTemplateRelRepository;
import com.youjian.banquet.repository.BanquetTemplateRepository;
import com.youjian.banquet.repository.BanquetTypeRepository;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 宴会模板/类型 Controller
 * 表: banquet_template / banquet_template_rel / banquet_type
 * 路径:
 *   /api/banquet-templates     宴会模板
 *   /api/banquet-types         宴会类型
 *   /api/banquet-template-rels 模板类型关联
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BanquetTemplateController {

    @Autowired private BanquetTemplateRepository templateRepo;
    @Autowired private BanquetTemplateRelRepository relRepo;
    @Autowired private BanquetTypeRepository typeRepo;

    private Long resolveQueryStoreId(Long requestStoreId) {
        Long currentStoreId = UserContext.getCurrentStoreId();
        if (!UserContext.isDataScopeAll() && currentStoreId != null) {
            return currentStoreId;
        }
        return requestStoreId;
    }

    // ============ 宴会模板 ============

    @GetMapping("/banquet-templates")
    public Result<List<BanquetTemplate>> listTemplates(@RequestParam(defaultValue = "1") Long storeId,
                                                         @RequestParam(required = false) String templateType,
                                                         @RequestParam(required = false) Integer isActive) {
        try {
            storeId = resolveQueryStoreId(storeId);
            List<BanquetTemplate> list;
            if (templateType != null && !templateType.isEmpty()) {
                list = templateRepo.findByStoreIdAndTemplateType(storeId, templateType);
            } else if (isActive != null) {
                list = templateRepo.findByStoreIdAndIsActive(storeId, isActive);
            } else {
                list = templateRepo.findByStoreId(storeId);
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "查询宴会模板失败: " + e.getMessage());
        }
    }

    @GetMapping("/banquet-templates/{id}")
    public Result<BanquetTemplate> getTemplate(@PathVariable Integer id) {
        try {
            BanquetTemplate t = templateRepo.findById(id).orElse(null);
            if (t == null) return Result.error(404, "模板不存在");
            if (t.getStoreId() != null) {
                try { UserContext.assertStoreAccess(t.getStoreId()); }
                catch (IllegalArgumentException e) { return Result.error(403, "无权限"); }
            }
            return Result.success(t);
        } catch (Exception e) {
            return Result.error(500, "获取模板失败: " + e.getMessage());
        }
    }

    @PostMapping("/banquet-templates")
    @Transactional
    public Result<BanquetTemplate> createTemplate(@RequestBody BanquetTemplate template) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            if (!UserContext.isDataScopeAll()) {
                template.setStoreId(UserContext.currentStoreId());
            }
            template.setId(null);
            BanquetTemplate saved = templateRepo.save(template);
            return Result.success(saved);
        } catch (Exception e) {
            try { TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); } catch (Exception ignore) {}
            return Result.error(500, "创建模板失败: " + e.getMessage());
        }
    }

    @PutMapping("/banquet-templates/{id}")
    @Transactional
    public Result<BanquetTemplate> updateTemplate(@PathVariable Integer id, @RequestBody BanquetTemplate template) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            BanquetTemplate existing = templateRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "模板不存在");
            if (!UserContext.isDataScopeAll()) {
                try { UserContext.assertStoreAccess(existing.getStoreId()); }
                catch (IllegalArgumentException e) { return Result.error(403, "无权限"); }
            }
            if (template.getTemplateName() != null) existing.setTemplateName(template.getTemplateName());
            if (template.getTemplateType() != null) existing.setTemplateType(template.getTemplateType());
            if (template.getDescription() != null) existing.setDescription(template.getDescription());
            if (template.getBasePrice() != null) existing.setBasePrice(template.getBasePrice());
            if (template.getIsActive() != null) existing.setIsActive(template.getIsActive());
            existing.setUpdatedAt(LocalDateTime.now());
            return Result.success(templateRepo.save(existing));
        } catch (Exception e) {
            return Result.error(500, "更新模板失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/banquet-templates/{id}")
    @Transactional
    public Result<?> deleteTemplate(@PathVariable Integer id) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            BanquetTemplate existing = templateRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "模板不存在");
            if (!UserContext.isDataScopeAll()) {
                try { UserContext.assertStoreAccess(existing.getStoreId()); }
                catch (IllegalArgumentException e) { return Result.error(403, "无权限"); }
            }
            templateRepo.delete(existing);
            return Result.success("已删除");
        } catch (Exception e) {
            return Result.error(500, "删除模板失败: " + e.getMessage());
        }
    }

    // ============ 宴会类型 ============

    @GetMapping("/banquet-types")
    public Result<List<BanquetType>> listTypes(@RequestParam(defaultValue = "1") Long storeId,
                                                 @RequestParam(required = false) Integer isActive) {
        try {
            storeId = resolveQueryStoreId(storeId);
            List<BanquetType> list;
            if (isActive != null) {
                list = typeRepo.findByStoreIdAndIsActive(storeId, isActive);
            } else {
                list = typeRepo.findByStoreId(storeId);
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "查询宴会类型失败: " + e.getMessage());
        }
    }

    @PostMapping("/banquet-types")
    @Transactional
    public Result<BanquetType> createType(@RequestBody BanquetType type) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            if (!UserContext.isDataScopeAll()) {
                type.setStoreId(UserContext.currentStoreId());
            }
            type.setId(null);
            return Result.success(typeRepo.save(type));
        } catch (Exception e) {
            try { TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); } catch (Exception ignore) {}
            return Result.error(500, "创建宴会类型失败: " + e.getMessage());
        }
    }

    @PutMapping("/banquet-types/{id}")
    @Transactional
    public Result<BanquetType> updateType(@PathVariable Integer id, @RequestBody BanquetType type) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            BanquetType existing = typeRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "类型不存在");
            if (!UserContext.isDataScopeAll()) {
                try { UserContext.assertStoreAccess(existing.getStoreId()); }
                catch (IllegalArgumentException e) { return Result.error(403, "无权限"); }
            }
            if (type.getTypeName() != null) existing.setTypeName(type.getTypeName());
            if (type.getDescription() != null) existing.setDescription(type.getDescription());
            if (type.getIsActive() != null) existing.setIsActive(type.getIsActive());
            existing.setUpdatedAt(LocalDateTime.now());
            return Result.success(typeRepo.save(existing));
        } catch (Exception e) {
            return Result.error(500, "更新宴会类型失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/banquet-types/{id}")
    @Transactional
    public Result<?> deleteType(@PathVariable Integer id) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            BanquetType existing = typeRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "类型不存在");
            if (!UserContext.isDataScopeAll()) {
                try { UserContext.assertStoreAccess(existing.getStoreId()); }
                catch (IllegalArgumentException e) { return Result.error(403, "无权限"); }
            }
            typeRepo.delete(existing);
            return Result.success("已删除");
        } catch (Exception e) {
            return Result.error(500, "删除宴会类型失败: " + e.getMessage());
        }
    }

    // ============ 模板-类型关联 ============

    @GetMapping("/banquet-template-rels")
    public Result<List<BanquetTemplateRel>> listRels(@RequestParam(defaultValue = "1") Long storeId,
                                                       @RequestParam(required = false) Integer banquetTypeId) {
        try {
            storeId = resolveQueryStoreId(storeId);
            List<BanquetTemplateRel> list;
            if (banquetTypeId != null) {
                list = relRepo.findByStoreIdAndBanquetTypeId(storeId, banquetTypeId);
            } else {
                list = relRepo.findByStoreId(storeId);
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "查询关联失败: " + e.getMessage());
        }
    }

    @PostMapping("/banquet-template-rels")
    @Transactional
    public Result<BanquetTemplateRel> createRel(@RequestBody BanquetTemplateRel rel) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            if (!UserContext.isDataScopeAll()) {
                rel.setStoreId(UserContext.currentStoreId());
            }
            rel.setId(null);
            return Result.success(relRepo.save(rel));
        } catch (Exception e) {
            try { TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); } catch (Exception ignore) {}
            return Result.error(500, "创建关联失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/banquet-template-rels/{id}")
    @Transactional
    public Result<?> deleteRel(@PathVariable Integer id) {
        try {
            if (!relRepo.existsById(id)) return Result.error(404, "关联不存在");
            relRepo.deleteById(id);
            return Result.success("已删除");
        } catch (Exception e) {
            return Result.error(500, "删除关联失败: " + e.getMessage());
        }
    }
}
