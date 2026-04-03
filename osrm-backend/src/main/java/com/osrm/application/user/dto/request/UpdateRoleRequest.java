package com.osrm.application.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新角色请求
 */
@Data
public class UpdateRoleRequest {

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    private String description;
}
