package com.osrm.application.storage.service;

import com.osrm.domain.storage.entity.HealthStatus;
import com.osrm.domain.storage.entity.StorageBackend;
import com.osrm.domain.storage.repository.StorageBackendRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 存储后端健康检查服务
 */
@Service
public class StorageHealthCheckService {

    private static final Logger logger = LoggerFactory.getLogger(StorageHealthCheckService.class);

    private final StorageBackendRepository storageBackendRepository;
    private final StorageBackendAppService storageBackendAppService;

    public StorageHealthCheckService(StorageBackendRepository storageBackendRepository,
                                     StorageBackendAppService storageBackendAppService) {
        this.storageBackendRepository = storageBackendRepository;
        this.storageBackendAppService = storageBackendAppService;
    }

    /**
     * 定时健康检查任务
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000) // 5分钟 = 300000毫秒
    public void scheduledHealthCheck() {
        logger.debug("开始执行存储后端健康检查任务");

        List<StorageBackend> backends = storageBackendRepository.findAllByEnabledTrue();

        for (StorageBackend backend : backends) {
            try {
                logger.debug("检查存储后端健康状态: {} ({})", backend.getBackendName(), backend.getId());

                var result = storageBackendAppService.healthCheck(backend.getId());

                // 如果状态变化，记录日志
                if (backend.getHealthStatus() != result.getHealthStatus()) {
                    if (result.getHealthStatus() == HealthStatus.ONLINE) {
                        logger.info("存储后端恢复在线: {} ({})", backend.getBackendName(), backend.getId());
                    } else {
                        logger.warn("存储后端状态异常: {} ({}), 状态: {}, 错误: {}",
                            backend.getBackendName(), backend.getId(),
                            result.getHealthStatus(), result.getErrorMessage());
                    }
                }

            } catch (Exception e) {
                logger.error("健康检查执行失败: {} ({})", backend.getBackendName(), backend.getId(), e);
            }
        }

        logger.debug("存储后端健康检查任务完成，共检查 {} 个存储后端", backends.size());
    }
}
