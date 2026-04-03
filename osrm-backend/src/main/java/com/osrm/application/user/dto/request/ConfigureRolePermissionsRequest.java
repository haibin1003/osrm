package com.osrm.application.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 配置角色权限请求
 */
@Data
public class ConfigureRolePermissionsRequest {

    @NotNull(message = "权限ID列表不能为空")
    private List<Long> permissionIds;
}
