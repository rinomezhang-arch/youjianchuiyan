package com.youjian.banquet.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品配方版本。
 * <p>
 * 每保存一次配方生成一条，版本号在「同门店同菜品」内从 1 递增。
 * 配方直接决定菜品成本，改过什么、谁改的、改之前什么样，必须留得住，
 * 所以历史明细行不删除，只在 {@link DishRecipe#getIsActive()} 上置 0，并挂到对应版本。
 */
@Entity
@Table(name = "recipe_revision")
public class RecipeRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "revision_id")
    private Long revisionId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "dish_id", nullable = false, length = 40)
    private String dishId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "item_count", nullable = false)
    private Integer itemCount;

    /** 该版本配方合计标准成本，与 dish_master.cost_price 在同一事务里写入，两者必须一致。 */
    @Column(name = "total_cost", precision = 15, scale = 4)
    private BigDecimal totalCost;

    @Column(name = "created_by", length = 40)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "note", length = 200)
    private String note;

    public Long getRevisionId() { return revisionId; }
    public void setRevisionId(Long revisionId) { this.revisionId = revisionId; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public String getDishId() { return dishId; }
    public void setDishId(String dishId) { this.dishId = dishId; }

    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }

    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
