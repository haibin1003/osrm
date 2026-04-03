package com.osrm.application.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建权限请求
 */
@Data
public class CreatePermissionRequest {

    private Long parentId;

    @NotBlank(message = "权限名称不能为空")
    private String permissionName;

    @NotBlank(message = "权限编码不能为空")
    private String permissionCode;

    @NotBlank(message = "资源类型不能为空")
    private String resourceType;

    private String action;

    private String path;

    private String icon;

    private Integer sortOrder = 0;

    private String description;
}
