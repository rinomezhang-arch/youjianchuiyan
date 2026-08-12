package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PrToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrTokenRepository extends JpaRepository<PrToken, Long>, JpaSpecificationExecutor<PrToken> {

    Optional<PrToken> findByToken(String token);

    Optional<PrToken> findByUseridAndRole(Long userid, String role);
}