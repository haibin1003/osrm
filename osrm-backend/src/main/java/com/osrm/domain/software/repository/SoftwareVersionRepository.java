package com.osrm.domain.software.repository;

import com.osrm.domain.software.entity.SoftwareVersion;
import com.osrm.domain.software.entity.VersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SoftwareVersionRepository extends JpaRepository<SoftwareVersion, Long> {

    List<SoftwareVersion> findBySoftwarePackageIdOrderByCreatedAtDesc(Long packageId);

    boolean existsBySoftwarePackageIdAndVersionNo(Long packageId, String versionNo);

    Optional<SoftwareVersion> findBySoftwarePackageIdAndVersionNo(Long packageId, String versionNo);

    List<SoftwareVersion> findBySoftwarePackageIdAndStatus(Long packageId, VersionStatus status);

    @Modifying
    @Query("UPDATE SoftwareVersion v SET v.isLatest = false WHERE v.softwarePackage.id = :packageId")
    void clearLatestFlagByPackageId(@Param("packageId") Long packageId);

    @Query("SELECT v FROM SoftwareVersion v WHERE v.softwarePackage.id = :packageId AND v.isLatest = true")
    Optional<SoftwareVersion> findLatestVersionByPackageId(@Param("packageId") Long packageId);

    long countBySoftwarePackageId(Long packageId);
}
