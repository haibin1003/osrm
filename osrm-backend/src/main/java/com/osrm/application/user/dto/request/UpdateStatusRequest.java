package com.osrm.application.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新状态请求
 */
@Data
public class UpdateStatusRequest {

    @NotNull(message = "状态不能为空")
    private Boolean enabled;
}
