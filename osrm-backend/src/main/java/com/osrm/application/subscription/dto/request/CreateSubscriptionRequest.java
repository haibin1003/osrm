package com.osrm.application.subscription.dto.request;

public class CreateSubscriptionRequest {

    private Long packageId;
    private Long versionId;
    private Long businessSystemId;
    private String useScene;

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }
    public Long getBusinessSystemId() { return businessSystemId; }
    public void setBusinessSystemId(Long businessSystemId) { this.businessSystemId = businessSystemId; }
    public String getUseScene() { return useScene; }
    public void setUseScene(String useScene) { this.useScene = useScene; }
}
