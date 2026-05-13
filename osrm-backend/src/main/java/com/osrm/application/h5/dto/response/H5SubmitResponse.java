package com.osrm.application.h5.dto.response;

import java.util.List;

public class H5SubmitResponse {

    private List<Long> inventoryIds;
    private int count;
    private String username;
    private String initialPassword;

    public H5SubmitResponse() {}

    public H5SubmitResponse(List<Long> inventoryIds, String username, String initialPassword) {
        this.inventoryIds = inventoryIds;
        this.count = inventoryIds.size();
        this.username = username;
        this.initialPassword = initialPassword;
    }

    public List<Long> getInventoryIds() { return inventoryIds; }
    public void setInventoryIds(List<Long> inventoryIds) { this.inventoryIds = inventoryIds; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getInitialPassword() { return initialPassword; }
    public void setInitialPassword(String initialPassword) { this.initialPassword = initialPassword; }
}
