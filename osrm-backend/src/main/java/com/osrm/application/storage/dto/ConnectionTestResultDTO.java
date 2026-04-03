package com.osrm.application.storage.dto;

/**
 * 连接测试结果 DTO
 */
public class ConnectionTestResultDTO {

    private boolean success;
    private String message;
    private Object serverInfo;
    private Long responseTimeMs;

    public ConnectionTestResultDTO() {
    }

    public ConnectionTestResultDTO(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public ConnectionTestResultDTO(boolean success, String message, Object serverInfo) {
        this.success = success;
        this.message = message;
        this.serverInfo = serverInfo;
    }

    // Getters and Setters

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getServerInfo() {
        return serverInfo;
    }

    public void setServerInfo(Object serverInfo) {
        this.serverInfo = serverInfo;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    /**
     * 快速创建成功结果
     */
    public static ConnectionTestResultDTO success(String message) {
        return new ConnectionTestResultDTO(true, message);
    }

    /**
     * 快速创建成功结果（带服务器信息）
     */
    public static ConnectionTestResultDTO success(String message, Object serverInfo) {
        return new ConnectionTestResultDTO(true, message, serverInfo);
    }

    /**
     * 快速创建失败结果
     */
    public static ConnectionTestResultDTO failure(String message) {
        return new ConnectionTestResultDTO(false, message);
    }
}
