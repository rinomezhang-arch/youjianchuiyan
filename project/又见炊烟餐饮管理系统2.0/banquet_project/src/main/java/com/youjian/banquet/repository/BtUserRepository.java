package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BtUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 管理员用户 Repository
 */
@Repository
public interface BtUserRepository extends JpaRepository<BtUser, Long>, JpaSpecificationExecutor<BtUser> {

    Optional<BtUser> findByUsername(String username);

    boolean existsByUsername(String username);
}