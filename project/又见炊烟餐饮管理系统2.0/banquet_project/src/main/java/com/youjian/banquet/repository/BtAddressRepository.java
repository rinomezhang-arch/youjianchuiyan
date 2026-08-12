package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BtAddress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 地址 Repository
 */
@Repository
public interface BtAddressRepository extends JpaRepository<BtAddress, Long>, JpaSpecificationExecutor<BtAddress> {

    Page<BtAddress> findByUserid(Long userid, Pageable pageable);

    Optional<BtAddress> findByUseridAndIsdefault(Long userid, String isdefault);

    @Modifying
    @Query("UPDATE BtAddress a SET a.isdefault = '否' WHERE a.userid = :userid")
    void clearDefaultByUserid(@Param("userid") Long userid);
}