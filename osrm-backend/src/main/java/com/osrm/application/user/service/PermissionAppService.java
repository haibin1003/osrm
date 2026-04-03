package com.osrm.application.user.service;

import com.osrm.application.user.dto.PermissionDTO;
import com.osrm.application.user.dto.request.CreatePermissionRequest;
import com.osrm.domain.user.entity.Permission;
import com.osrm.domain.user.repository.PermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限应用服务
 */
@Service
@Transactional(readOnly = true)
public class PermissionAppService {

    private final PermissionRepository permissionRepository;

    public PermissionAppService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public List<PermissionDTO> getPermissionTree() {
        List<Permission> allPermissions = permissionRepository.findAll();
        return buildTree(allPermissions, null);
    }

    public List<PermissionDTO> listPermissions(String permissionName, String permissionCode, String resourceType) {
        List<Permission> permissions;
        if (permissionName != null || permissionCode != null || resourceType != null) {
            permissions = permissionRepository.findByConditions(permissionName, permissionCode, resourceType);
        } else {
            permissions = permissionRepository.findAll();
        }
        return permissions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public PermissionDTO createPermission(CreatePermissionRequest request) {
        if (permissionRepository.existsByPermissionCode(request.getPermissionCode())) {
            throw new RuntimeException("权限编码已存在");
        }

        Permission permission = new Permission();
        permission.setParentId(request.getParentId());
        permission.setPermissionName(request.getPermissionName());
        permission.setPermissionCode(request.getPermissionCode());
        permission.setResourceType(request.getResourceType());
        permission.setAction(request.getAction());
        permission.setPath(request.getPath());
        permission.setIcon(request.getIcon());
        permission.setSortOrder(request.getSortOrder());
        permission.setDescription(request.getDescription());

        Permission saved = permissionRepository.save(permission);
        return convertToDTO(saved);
    }

    @Transactional
    public void deletePermission(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("权限不存在"));

        // 检查是否有子权限
        List<Permission> children = permissionRepository.findByParentId(id);
        if (!children.isEmpty()) {
            throw new RuntimeException("有子权限，不能删除");
        }

        permissionRepository.delete(permission);
    }

    private List<PermissionDTO> buildTree(List<Permission> allPermissions, Long parentId) {
        return allPermissions.stream()
                .filter(p -> (parentId == null && p.getParentId() == null) ||
                           (parentId != null && parentId.equals(p.getParentId())))
                .sorted(Comparator.comparingInt(Permission::getSortOrder))
                .map(p -> {
                    PermissionDTO dto = convertToDTO(p);
                    dto.setChildren(buildTree(allPermissions, p.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private PermissionDTO convertToDTO(Permission permission) {
        PermissionDTO dto = new PermissionDTO();
        dto.setId(permission.getId());
        dto.setParentId(permission.getParentId());
        dto.setPermissionCode(permission.getPermissionCode());
        dto.setPermissionName(permission.getPermissionName());
        dto.setResourceType(permission.getResourceType());
        dto.setAction(permission.getAction());
        dto.setPath(permission.getPath());
        dto.setIcon(permission.getIcon());
        dto.setSortOrder(permission.getSortOrder());
        return dto;
    }
}
