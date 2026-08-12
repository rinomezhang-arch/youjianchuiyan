package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PrYonghu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrYonghuRepository extends JpaRepository<PrYonghu, Long>, JpaSpecificationExecutor<PrYonghu> {

    Optional<PrYonghu> findByYonghuzhanghao(String yonghuzhanghao);
}