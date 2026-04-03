package com.osrm.domain.category.repository;

import com.osrm.domain.category.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByTagCode(String tagCode);

    boolean existsByTagCode(String tagCode);

    boolean existsByTagName(String tagName);
}
