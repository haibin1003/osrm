package com.osrm.application.user.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * 更新用户请求
 */
@Data
public class UpdateUserRequest {

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotEmpty(message = "角色不能为空")
    private List<Long> roleIds;

    @NotNull(message = "状态不能为空")
    private Boolean enabled;
}
