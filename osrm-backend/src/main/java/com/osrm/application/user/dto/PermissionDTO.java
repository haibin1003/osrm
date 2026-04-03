package com.osrm.application.user.dto;

import lombok.Data;

import java.util.List;

/**
 * 权限DTO
 */
@Data
public class PermissionDTO {
    private Long id;
    private Long parentId;
    private String permissionCode;
    private String permissionName;
    private String resourceType;
    private String action;
    private String path;
    private String icon;
    private Integer sortOrder;
    private List<PermissionDTO> children;
}
