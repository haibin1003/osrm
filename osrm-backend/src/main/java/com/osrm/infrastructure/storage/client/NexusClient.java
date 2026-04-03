package com.osrm.infrastructure.storage.client;

import com.osrm.application.storage.dto.ConnectionTestResultDTO;

import java.util.List;
import java.util.Map;

/**
 * Nexus 客户端接口
 */
public interface NexusClient {

    /**
     * 连接测试
     */
    ConnectionTestResultDTO testConnection(NexusConfig config);

    /**
     * 获取状态信息
     */
    Map<String, Object> getStatus(NexusConfig config);

    /**
     * 获取仓库列表
     */
    List<Map<String, Object>> listRepositories(NexusConfig config);

    /**
     * 检查仓库是否存在
     */
    boolean repositoryExists(NexusConfig config, String repoName);
}
