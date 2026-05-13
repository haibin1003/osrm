package com.osrm.application.h5.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class GenerateTokenRequest {

    @Min(1) @Max(365)
    private int validDays = 7;

    private String remark;

    public int getValidDays() { return validDays; }
    public void setValidDays(int validDays) { this.validDays = validDays; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
