package com.osrm.application.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 创建角色请求
 */
@Data
public class CreateRoleRequest {

    @NotBlank(message = "角色编码不能为空")
    @Pattern(regexp = "^ROLE_[A-Z_]+$", message = "角色编码格式必须为ROLE_XXX，全大写")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    private String description;
}
