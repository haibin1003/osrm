package com.osrm.application.user.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 个人资料DTO
 */
@Data
public class ProfileDTO {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private String bio;
    private String avatar;
    private List<String> roles;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
}
