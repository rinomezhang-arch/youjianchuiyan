package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PackageMaster;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PackageMasterRepository
extends JpaRepository<PackageMaster, PackageMaster.PackageMasterId>,
JpaSpecificationExecutor<PackageMaster> {
    public List<PackageMaster> findByStoreId(Long var1);

    public List<PackageMaster> findByStoreIdAndStatus(Long var1, String var2);

    public List<PackageMaster> findByStoreIdAndCategory(Long var1, String var2);

    public Optional<PackageMaster> findByPackageIdAndStoreId(String var1, Long var2);

    public List<PackageMaster> findByStoreIdOrderBySortOrderAsc(Long var1);

    @Query(value="SELECT p FROM PackageMaster p WHERE p.storeId = :storeId AND (p.packageName LIKE %:keyword% OR p.category LIKE %:keyword%)")
    public List<PackageMaster> searchByKeyword(@Param(value="storeId") Long var1, @Param(value="keyword") String var2);

    public void deleteByPackageIdAndStoreId(String var1, Long var2);

    @Query(value="SELECT COUNT(p) FROM PackageMaster p WHERE p.packageId LIKE CONCAT(:prefix, '%')")
    public long countByPackageIdPrefix(@Param(value="prefix") String prefix);
}
