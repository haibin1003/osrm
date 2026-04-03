package com.osrm.domain.user.repository;

import com.osrm.domain.user.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByPermissionCode(String permissionCode);

    boolean existsByPermissionCode(String permissionCode);

    List<Permission> findByParentId(Long parentId);

    @Query("SELECT p FROM Permission p WHERE " +
           "(:permissionName IS NULL OR p.permissionName LIKE %:permissionName%) AND " +
           "(:permissionCode IS NULL OR p.permissionCode LIKE %:permissionCode%) AND " +
           "(:resourceType IS NULL OR p.resourceType = :resourceType)")
    List<Permission> findByConditions(@Param("permissionName") String permissionName,
                                    @Param("permissionCode") String permissionCode,
                                    @Param("resourceType") String resourceType);
}
