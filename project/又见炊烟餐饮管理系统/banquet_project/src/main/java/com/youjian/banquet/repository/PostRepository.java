package com.youjian.banquet.repository;

import com.youjian.banquet.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {

    List<Post> findByDeptId(Integer deptId);

    List<Post> findByDeptIdOrderBySortOrderAsc(Integer deptId);
}
