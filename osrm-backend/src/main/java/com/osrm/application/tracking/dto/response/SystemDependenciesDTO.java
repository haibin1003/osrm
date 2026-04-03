package com.osrm.application.tracking.dto.response;

import com.osrm.application.tracking.dto.SystemNode;

import java.util.List;
import java.util.Map;

/**
 * 系统依赖详情 DTO
 */
public class SystemDependenciesDTO {
    private SystemNode system;
    private List<PackageDependencyInfo> packages;
    private DependencyStatistics statistics;

    public SystemDependenciesDTO() {}

    // Getters and Setters
    public SystemNode getSystem() { return system; }
    public void setSystem(SystemNode system) { this.system = system; }
    public List<PackageDependencyInfo> getPackages() { return packages; }
    public void setPackages(List<PackageDependencyInfo> packages) { this.packages = packages; }
    public DependencyStatistics getStatistics() { return statistics; }
    public void setStatistics(DependencyStatistics statistics) { this.statistics = statistics; }

    /**
     * 软件包依赖信息
     */
    public static class PackageDependencyInfo {
        private Long packageId;
        private String packageName;
        private String packageKey;
        private String softwareType;
        private String versionNumber;
        private String status;

        // Getters and Setters
        public Long getPackageId() { return packageId; }
        public void setPackageId(Long packageId) { this.packageId = packageId; }
        public String getPackageName() { return packageName; }
        public void setPackageName(String packageName) { this.packageName = packageName; }
        public String getPackageKey() { return packageKey; }
        public void setPackageKey(String packageKey) { this.packageKey = packageKey; }
        public String getSoftwareType() { return softwareType; }
        public void setSoftwareType(String softwareType) { this.softwareType = softwareType; }
        public String getVersionNumber() { return versionNumber; }
        public void setVersionNumber(String versionNumber) { this.versionNumber = versionNumber; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    /**
     * 依赖统计
     */
    public static class DependencyStatistics {
        private Integer totalPackages;
        private Map<String, Integer> byType;

        // Getters and Setters
        public Integer getTotalPackages() { return totalPackages; }
        public void setTotalPackages(Integer totalPackages) { this.totalPackages = totalPackages; }
        public Map<String, Integer> getByType() { return byType; }
        public void setByType(Map<String, Integer> byType) { this.byType = byType; }
    }
}
