package com.youjian.banquet.repository;

import com.youjian.banquet.entity.RecipeRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeRevisionRepository extends JpaRepository<RecipeRevision, Long> {

    /** 版本列表，最新在前。 */
    List<RecipeRevision> findByStoreIdAndDishIdOrderByVersionNoDesc(Long storeId, String dishId);

    Optional<RecipeRevision> findByStoreIdAndDishIdAndVersionNo(Long storeId, String dishId, Integer versionNo);

    /**
     * 取下一个版本号。
     * <p>
     * 调用前保存流程已对 dish_master 对应行加了 FOR UPDATE 排他锁，同一菜品的并发保存被串行化，
     * 所以这里读到的最大版本号不会被别的事务插队；即便如此，
     * recipe_revision 上仍有 (store_id, dish_id, version_no) 唯一键作为第二道防线。
     */
    @Query("SELECT COALESCE(MAX(r.versionNo), 0) FROM RecipeRevision r " +
           "WHERE r.storeId = :storeId AND r.dishId = :dishId")
    Integer findMaxVersionNo(@Param("storeId") Long storeId, @Param("dishId") String dishId);
}
