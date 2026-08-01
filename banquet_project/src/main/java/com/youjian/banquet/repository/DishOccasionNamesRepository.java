package com.youjian.banquet.repository;

import com.youjian.banquet.entity.DishOccasionNames;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DishOccasionNamesRepository
extends JpaRepository<DishOccasionNames, Long>,
JpaSpecificationExecutor<DishOccasionNames> {

    List<DishOccasionNames> findByStoreId(Long storeId);

    List<DishOccasionNames> findByDishIdAndStoreId(String dishId, Long storeId);

    void deleteByDishIdAndStoreId(String dishId, Long storeId);
}
