package com.osrm.application.subscription.dto.response;

import com.osrm.domain.subscription.entity.DownloadToken;

import java.time.LocalDateTime;

public class DownloadTokenDTO {

    private Long id;
    private Long subscriptionId;
    private String token;
    private LocalDateTime expireAt;
    private Integer maxDownloads;
    private Integer usedCount;
    private Boolean enabled;
    private LocalDateTime createdAt;

    public static DownloadTokenDTO from(DownloadToken token) {
        DownloadTokenDTO dto = new DownloadTokenDTO();
        dto.setId(token.getId());
        dto.setSubscriptionId(token.getSubscriptionId());
        dto.setToken(token.getToken());
        dto.setExpireAt(token.getExpireAt());
        dto.setMaxDownloads(token.getMaxDownloads());
        dto.setUsedCount(token.getUsedCount());
        dto.setEnabled(token.getEnabled());
        dto.setCreatedAt(token.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
    public Integer getMaxDownloads() { return maxDownloads; }
    public void setMaxDownloads(Integer maxDownloads) { this.maxDownloads = maxDownloads; }
    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
