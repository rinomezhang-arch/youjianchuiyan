package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PrMaterialInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrMaterialInfoRepository extends JpaRepository<PrMaterialInfo, Long>, JpaSpecificationExecutor<PrMaterialInfo> {

    List<PrMaterialInfo> findByGongyingshangzhanghao(String gongyingshangzhanghao);

    List<PrMaterialInfo> findByCailiaozhonglei(String cailiaozhonglei);

    List<PrMaterialInfo> findByCailiaomingchengContaining(String cailiaomingcheng);
}