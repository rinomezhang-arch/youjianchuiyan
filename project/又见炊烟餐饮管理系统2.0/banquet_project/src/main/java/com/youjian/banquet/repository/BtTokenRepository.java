package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BtToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Token Repository
 */
@Repository
public interface BtTokenRepository extends JpaRepository<BtToken, Long>, JpaSpecificationExecutor<BtToken> {

    Optional<BtToken> findByToken(String token);

    Optional<BtToken> findByUseridAndRole(Long userid, String role);
}