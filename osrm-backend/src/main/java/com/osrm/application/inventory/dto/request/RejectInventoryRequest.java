package com.osrm.application.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 驳回存量登记请求
 */
public class RejectInventoryRequest {

    @NotBlank(message = "驳回原因不能为空")
    @Size(max = 256, message = "驳回原因长度不能超过256")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
