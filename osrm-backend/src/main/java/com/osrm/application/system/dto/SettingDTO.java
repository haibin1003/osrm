package com.osrm.application.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统设置DTO
 */
@Data
public class SettingDTO {
    private Long id;
    private String category;
    private String key;
    private String value;
    private String description;
    private LocalDateTime updateTime;
}
