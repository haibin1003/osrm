package com.osrm.application.user.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色DTO
 */
@Data
public class RoleDTO {
    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer permissionCount;
    private LocalDateTime createTime;
}
