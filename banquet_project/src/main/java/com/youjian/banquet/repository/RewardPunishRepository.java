package com.youjian.banquet.repository;

import com.youjian.banquet.entity.RewardPunish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardPunishRepository extends JpaRepository<RewardPunish, Long> {

    List<RewardPunish> findByStoreId(Long storeId);

    List<RewardPunish> findByStaffId(Long staffId);

    List<RewardPunish> findByStaffIdAndFinalStatus(Long staffId, Integer finalStatus);

    List<RewardPunish> findByStoreIdAndFinalStatus(Long storeId, Integer finalStatus);

    List<RewardPunish> findByStoreIdAndIsSyncedToSalary(Long storeId, Integer isSynced);
}
