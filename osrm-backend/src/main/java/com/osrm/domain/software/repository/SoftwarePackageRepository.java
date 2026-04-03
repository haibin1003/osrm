package com.osrm.domain.software.repository;

import com.osrm.domain.software.entity.PackageStatus;
import com.osrm.domain.software.entity.SoftwarePackage;
import com.osrm.domain.software.entity.SoftwareType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SoftwarePackageRepository extends JpaRepository<SoftwarePackage, Long> {

    Optional<SoftwarePackage> findByPackageName(String packageName);

    Optional<SoftwarePackage> findByPackageKey(String packageKey);

    boolean existsByPackageName(String packageName);

    boolean existsByPackageKey(String packageKey);

    @Query("SELECT p FROM SoftwarePackage p WHERE " +
           "(:keyword IS NULL OR p.packageName LIKE %:keyword% OR p.packageKey LIKE %:keyword% OR p.description LIKE %:keyword%) AND " +
           "(:type IS NULL OR p.softwareType = :type) AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:categoryId IS NULL OR p.categoryId = :categoryId)")
    Page<SoftwarePackage> findByConditions(
            @Param("keyword") String keyword,
            @Param("type") SoftwareType type,
            @Param("status") PackageStatus status,
            @Param("categoryId") Long categoryId,
            Pageable pageable);

    @Query("SELECT p FROM SoftwarePackage p WHERE p.createdBy = :userId AND " +
           "(:keyword IS NULL OR p.packageName LIKE %:keyword% OR p.packageKey LIKE %:keyword%) AND " +
           "(:type IS NULL OR p.softwareType = :type) AND " +
           "(:status IS NULL OR p.status = :status)")
    Page<SoftwarePackage> findByCreatedBy(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("type") SoftwareType type,
            @Param("status") PackageStatus status,
            Pageable pageable);

    long countByStatus(PackageStatus status);

    long countBySoftwareType(SoftwareType softwareType);

    @Query("SELECT p FROM SoftwarePackage p WHERE p.status = :status")
    Page<SoftwarePackage> findByStatus(@Param("status") PackageStatus status, Pageable pageable);

    @Query("SELECT p FROM SoftwarePackage p WHERE p.status = :status ORDER BY p.viewCount DESC")
    List<SoftwarePackage> findTopByStatusOrderByViewCountDesc(@Param("status") PackageStatus status, Pageable pageable);

    List<SoftwarePackage> findByStatus(PackageStatus status);

    Integer countByStatusAndCreatedAtBefore(PackageStatus status, java.time.LocalDateTime createdAt);
}
