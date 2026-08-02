package com.youjian.banquet.repository;

import com.youjian.banquet.entity.ToolInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToolInventoryRepository extends JpaRepository<ToolInventory, Long> {
    List<ToolInventory> findByStoreId(Long storeId);
    List<ToolInventory> findByStoreIdAndStatus(Long storeId, String status);
    Optional<ToolInventory> findByInventoryNo(String inventoryNo);
}
