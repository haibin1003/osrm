package com.osrm.application.h5.dto;

import com.osrm.domain.h5.entity.H5AccessToken;

import java.time.LocalDateTime;

public class H5AccessTokenDTO {

    private Long id;
    private String token;
    private LocalDateTime expireAt;
    private Boolean enabled;
    private String createdByName;
    private String remark;
    private Long accessCount;
    private LocalDateTime lastAccessAt;
    private LocalDateTime createdAt;

    public static H5AccessTokenDTO from(H5AccessToken t) {
        H5AccessTokenDTO dto = new H5AccessTokenDTO();
        dto.setId(t.getId());
        dto.setToken(t.getToken());
        dto.setExpireAt(t.getExpireAt());
        dto.setEnabled(t.getEnabled());
        dto.setCreatedByName(t.getCreatedByName());
        dto.setRemark(t.getRemark());
        dto.setAccessCount(t.getAccessCount());
        dto.setLastAccessAt(t.getLastAccessAt());
        dto.setCreatedAt(t.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Long getAccessCount() { return accessCount; }
    public void setAccessCount(Long accessCount) { this.accessCount = accessCount; }
    public LocalDateTime getLastAccessAt() { return lastAccessAt; }
    public void setLastAccessAt(LocalDateTime lastAccessAt) { this.lastAccessAt = lastAccessAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
