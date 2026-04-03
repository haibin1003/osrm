package com.osrm.domain.user.repository;

import com.osrm.domain.user.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByRoleCode(String roleCode);

    boolean existsByRoleCode(String roleCode);

    @Query("SELECT r FROM Role r JOIN r.permissions p WHERE p.permissionCode = :permissionCode")
    List<Role> findByPermissionCode(@Param("permissionCode") String permissionCode);

    Set<Role> findByRoleCodeIn(Set<String> roleCodes);

    @Query("SELECT r FROM Role r WHERE " +
           "(:roleName IS NULL OR r.roleName LIKE %:roleName%) AND " +
           "(:roleCode IS NULL OR r.roleCode LIKE %:roleCode%)")
    Page<Role> findByConditions(@Param("roleName") String roleName,
                               @Param("roleCode") String roleCode,
                               Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.id = :roleId")
    Long countUsersByRoleId(@Param("roleId") Long roleId);
}
