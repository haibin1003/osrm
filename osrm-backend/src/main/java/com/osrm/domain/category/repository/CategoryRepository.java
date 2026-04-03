package com.osrm.domain.category.repository;

import com.osrm.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByCategoryCode(String categoryCode);

    boolean existsByCategoryCode(String categoryCode);

    List<Category> findByParentIsNullOrderBySortOrderAsc();

    List<Category> findByParentIdOrderBySortOrderAsc(Long parentId);

    @Query("SELECT COALESCE(MAX(c.sortOrder), 0) FROM Category c WHERE c.parent.id = :parentId")
    Integer findMaxSortOrderByParentId(Long parentId);

    @Query("SELECT COALESCE(MAX(c.sortOrder), 0) FROM Category c WHERE c.parent IS NULL")
    Integer findMaxSortOrderForRoot();
}
