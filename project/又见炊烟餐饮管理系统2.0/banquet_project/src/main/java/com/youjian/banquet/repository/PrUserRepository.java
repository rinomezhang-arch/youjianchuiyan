package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PrUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrUserRepository extends JpaRepository<PrUser, Long>, JpaSpecificationExecutor<PrUser> {

    Optional<PrUser> findByUsername(String username);
}