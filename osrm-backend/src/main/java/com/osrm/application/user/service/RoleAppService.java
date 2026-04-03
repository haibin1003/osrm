package com.osrm.application.user.service;

import com.osrm.application.user.dto.RoleDTO;
import com.osrm.application.user.dto.request.CreateRoleRequest;
import com.osrm.application.user.dto.request.UpdateRoleRequest;
import com.osrm.domain.user.entity.Permission;
import com.osrm.domain.user.entity.Role;
import com.osrm.domain.user.repository.PermissionRepository;
import com.osrm.domain.user.repository.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色应用服务
 */
@Service
@Transactional(readOnly = true)
public class RoleAppService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleAppService(RoleRepository roleRepository,
                          PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    public Page<RoleDTO> listRoles(String roleName, String roleCode, Pageable pageable) {
        Page<Role> rolePage = roleRepository.findByConditions(roleName, roleCode, pageable);
        return rolePage.map(this::convertToDTO);
    }

    public RoleDTO getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
        return convertToDTO(role);
    }

    @Transactional
    public RoleDTO createRole(CreateRoleRequest request) {
        if (roleRepository.existsByRoleCode(request.getRoleCode())) {
            throw new RuntimeException("角色编码已存在");
        }

        Role role = new Role();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        role.setEnabled(true);

        Role savedRole = roleRepository.save(role);
        return convertToDTO(savedRole);
    }

    @Transactional
    public RoleDTO updateRole(Long id, UpdateRoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());

        Role savedRole = roleRepository.save(role);
        return convertToDTO(savedRole);
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        // 检查角色是否被用户使用
        if (roleRepository.countUsersByRoleId(id) > 0) {
            throw new RuntimeException("角色已被用户使用，不能删除");
        }

        roleRepository.delete(role);
    }

    @Transactional
    public void configurePermissions(Long roleId, List<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        Set<Permission> permissions = new HashSet<>(
                permissionRepository.findAllById(permissionIds)
        );
        role.setPermissions(permissions);
        roleRepository.save(role);
    }

    public List<Long> getRolePermissionIds(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        if (role.getPermissions() == null) {
            return List.of();
        }

        return role.getPermissions().stream()
                .map(Permission::getId)
                .collect(Collectors.toList());
    }

    private RoleDTO convertToDTO(Role role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setRoleCode(role.getRoleCode());
        dto.setRoleName(role.getRoleName());
        dto.setDescription(role.getDescription());
        dto.setPermissionCount(role.getPermissions() != null ? role.getPermissions().size() : 0);
        dto.setCreateTime(role.getCreatedAt());
        return dto;
    }
}
