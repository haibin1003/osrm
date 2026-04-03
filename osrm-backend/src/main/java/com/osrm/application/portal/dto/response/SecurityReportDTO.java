package com.osrm.application.portal.dto.response;

public class SecurityReportDTO {

    private int score;
    private int criticalCount;
    private int highCount;
    private int mediumCount;
    private int lowCount;
    private String scanTime;
    private String status;

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getCriticalCount() { return criticalCount; }
    public void setCriticalCount(int criticalCount) { this.criticalCount = criticalCount; }

    public int getHighCount() { return highCount; }
    public void setHighCount(int highCount) { this.highCount = highCount; }

    public int getMediumCount() { return mediumCount; }
    public void setMediumCount(int mediumCount) { this.mediumCount = mediumCount; }

    public int getLowCount() { return lowCount; }
    public void setLowCount(int lowCount) { this.lowCount = lowCount; }

    public String getScanTime() { return scanTime; }
    public void setScanTime(String scanTime) { this.scanTime = scanTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
