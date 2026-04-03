package com.osrm.infrastructure.storage.client;

import com.osrm.application.storage.dto.ConnectionTestResultDTO;

import java.util.List;
import java.util.Map;

/**
 * Harbor 客户端接口
 */
public interface HarborClient {

    /**
     * 连接测试
     */
    ConnectionTestResultDTO testConnection(HarborConfig config);

    /**
     * 获取系统信息
     */
    Map<String, Object> getSystemInfo(HarborConfig config);

    /**
     * 获取项目列表
     */
    List<Map<String, Object>> listProjects(HarborConfig config);

    /**
     * 检查项目是否存在
     */
    boolean projectExists(HarborConfig config, String projectName);
}
