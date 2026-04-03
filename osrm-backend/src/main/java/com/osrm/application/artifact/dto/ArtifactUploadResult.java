package com.osrm.application.artifact.dto;

/**
 * 上传制品结果
 */
public class ArtifactUploadResult {

    private boolean success;
    private String filePath;
    private Long fileSize;
    private String md5Hash;
    private String downloadCommand;
    private String message;

    public static ArtifactUploadResult success(String filePath, Long fileSize, String md5Hash, String downloadCommand) {
        ArtifactUploadResult r = new ArtifactUploadResult();
        r.success = true;
        r.filePath = filePath;
        r.fileSize = fileSize;
        r.md5Hash = md5Hash;
        r.downloadCommand = downloadCommand;
        r.message = "上传成功";
        return r;
    }

    public static ArtifactUploadResult failure(String message) {
        ArtifactUploadResult r = new ArtifactUploadResult();
        r.success = false;
        r.message = message;
        return r;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getMd5Hash() { return md5Hash; }
    public void setMd5Hash(String md5Hash) { this.md5Hash = md5Hash; }
    public String getDownloadCommand() { return downloadCommand; }
    public void setDownloadCommand(String downloadCommand) { this.downloadCommand = downloadCommand; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
