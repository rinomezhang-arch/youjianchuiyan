package com.youjian.banquet.repository;

import com.youjian.banquet.entity.SysNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysNotificationRepository extends JpaRepository<SysNotification, Long> {

    List<SysNotification> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    List<SysNotification> findByStoreIdAndIsReadOrderByCreatedAtDesc(Long storeId, Integer isRead);

    List<SysNotification> findByStoreIdAndNotifyTypeOrderByCreatedAtDesc(Long storeId, String notifyType);

    List<SysNotification> findByStoreIdAndRelatedTypeAndRelatedIdOrderByCreatedAtDesc(
            Long storeId, String relatedType, Long relatedId);
}
