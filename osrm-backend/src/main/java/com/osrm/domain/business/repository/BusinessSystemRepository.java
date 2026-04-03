package com.osrm.domain.business.repository;

import com.osrm.domain.business.entity.BusinessDomain;
import com.osrm.domain.business.entity.BusinessSystem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusinessSystemRepository extends JpaRepository<BusinessSystem, Long> {

    Optional<BusinessSystem> findBySystemCode(String systemCode);

    Optional<BusinessSystem> findBySystemName(String systemName);

    boolean existsBySystemCode(String systemCode);

    boolean existsBySystemName(String systemName);

    @Query("SELECT b FROM BusinessSystem b WHERE " +
           "(:keyword IS NULL OR b.systemCode LIKE %:keyword% OR b.systemName LIKE %:keyword%) AND " +
           "(:domain IS NULL OR b.domain = :domain) AND " +
           "(:enabled IS NULL OR b.enabled = :enabled)")
    Page<BusinessSystem> findByConditions(
            @Param("keyword") String keyword,
            @Param("domain") BusinessDomain domain,
            @Param("enabled") Boolean enabled,
            Pageable pageable);

    Integer countByEnabled(boolean enabled);

    java.util.List<BusinessSystem> findByEnabled(boolean enabled);
}
