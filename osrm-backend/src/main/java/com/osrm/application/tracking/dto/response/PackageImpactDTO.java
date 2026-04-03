package com.osrm.application.tracking.dto.response;

import com.osrm.application.tracking.dto.PackageNode;

import java.util.List;
import java.util.Map;

/**
 * 软件影响分析 DTO
 */
public class PackageImpactDTO {
    private PackageNode packageInfo;
    private List<AffectedSystemInfo> affectedSystems;
    private ImpactStatistics statistics;

    public PackageImpactDTO() {}

    // Getters and Setters
    public PackageNode getPackageInfo() { return packageInfo; }
    public void setPackageInfo(PackageNode packageInfo) { this.packageInfo = packageInfo; }
    public List<AffectedSystemInfo> getAffectedSystems() { return affectedSystems; }
    public void setAffectedSystems(List<AffectedSystemInfo> affectedSystems) { this.affectedSystems = affectedSystems; }
    public ImpactStatistics getStatistics() { return statistics; }
    public void setStatistics(ImpactStatistics statistics) { this.statistics = statistics; }

    /**
     * 受影响的系统信息
     */
    public static class AffectedSystemInfo {
        private Long systemId;
        private String systemName;
        private String systemCode;
        private String domain;
        private String versionNumber;

        // Getters and Setters
        public Long getSystemId() { return systemId; }
        public void setSystemId(Long systemId) { this.systemId = systemId; }
        public String getSystemName() { return systemName; }
        public void setSystemName(String systemName) { this.systemName = systemName; }
        public String getSystemCode() { return systemCode; }
        public void setSystemCode(String systemCode) { this.systemCode = systemCode; }
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public String getVersionNumber() { return versionNumber; }
        public void setVersionNumber(String versionNumber) { this.versionNumber = versionNumber; }
    }

    /**
     * 影响统计
     */
    public static class ImpactStatistics {
        private Integer totalSystems;
        private Map<String, Integer> byDomain;
        private Map<String, Integer> byVersion;

        // Getters and Setters
        public Integer getTotalSystems() { return totalSystems; }
        public void setTotalSystems(Integer totalSystems) { this.totalSystems = totalSystems; }
        public Map<String, Integer> getByDomain() { return byDomain; }
        public void setByDomain(Map<String, Integer> byDomain) { this.byDomain = byDomain; }
        public Map<String, Integer> getByVersion() { return byVersion; }
        public void setByVersion(Map<String, Integer> byVersion) { this.byVersion = byVersion; }
    }
}
