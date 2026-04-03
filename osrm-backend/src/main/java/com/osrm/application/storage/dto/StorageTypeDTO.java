package com.osrm.application.storage.dto;

import com.osrm.domain.storage.entity.StorageBackendType;

/**
 * 存储类型 DTO
 */
public class StorageTypeDTO {

    private String code;
    private String name;
    private String description;

    public StorageTypeDTO() {
    }

    public StorageTypeDTO(StorageBackendType type) {
        this.code = type.name();
        this.name = type.getDisplayName();
        this.description = type.getDescription();
    }

    public StorageTypeDTO(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    // Getters and Setters

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
