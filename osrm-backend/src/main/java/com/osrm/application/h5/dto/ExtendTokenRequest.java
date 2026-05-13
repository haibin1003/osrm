package com.osrm.application.h5.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class ExtendTokenRequest {

    @Min(1) @Max(365)
    private int extraDays;

    public int getExtraDays() { return extraDays; }
    public void setExtraDays(int extraDays) { this.extraDays = extraDays; }
}
