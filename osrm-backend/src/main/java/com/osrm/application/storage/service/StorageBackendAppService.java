package com.osrm.application.storage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.osrm.application.storage.dto.*;
import com.osrm.application.storage.dto.request.CreateStorageBackendRequest;
import com.osrm.application.storage.dto.request.TestConnectionRequest;
import com.osrm.application.storage.dto.request.UpdateStorageBackendRequest;
import com.osrm.common.exception.BizException;
import com.osrm.common.model.PageResult;
import com.osrm.domain.storage.entity.HealthStatus;
import com.osrm.domain.storage.entity.StorageBackend;
import com.osrm.domain.storage.entity.StorageBackendType;
import com.osrm.domain.storage.repository.StorageBackendRepository;
import com.osrm.infrastructure.config.PasswordEncryptor;
import com.osrm.infrastructure.storage.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 存储后端应用服务
 */
@Service
public class StorageBackendAppService {

    private static final Logger logger = LoggerFactory.getLogger(StorageBackendAppService.class);

    private final StorageBackendRepository storageBackendRepository;
    private final HarborClient harborClient;
    private final NexusClient nexusClient;
    private final PasswordEncryptor passwordEncryptor;
    private final ObjectMapper objectMapper;

    public StorageBackendAppService(StorageBackendRepository storageBackendRepository,
                                    HarborClient harborClient,
                                    NexusClient nexusClient,
                                    PasswordEncryptor passwordEncryptor,
                                    ObjectMapper objectMapper) {
        this.storageBackendRepository = storageBackendRepository;
        this.harborClient = harborClient;
        this.nexusClient = nexusClient;
        this.passwordEncryptor = passwordEncryptor;
        this.objectMapper = objectMapper;
    }

    /**
     * 分页查询存储后端
     */
    public PageResult<StorageBackendDTO> findByConditions(String keyword, String type, String status, int page, int size) {
        StorageBackendType backendType = type != null ? StorageBackendType.valueOf(type) : null;
        HealthStatus healthStatus = status != null ? HealthStatus.valueOf(status) : null;

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<StorageBackend> result = storageBackendRepository.findByConditions(keyword, backendType, healthStatus, pageable);

        List<StorageBackendDTO> dtoList = result.getContent().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());

        return PageResult.of(dtoList, result.getTotalElements(), result.getTotalPages(), size, page);
    }

    /**
     * 获取详情
     */
    public StorageBackendDTO findById(Long id) {
        StorageBackend backend = storageBackendRepository.findById(id)
            .orElseThrow(() -> new BizException("存储后端不存在"));
        return convertToDTO(backend);
    }

    /**
     * 创建存储后端
     */
    @Transactional
    public StorageBackendDTO create(CreateStorageBackendRequest request, Long createdBy) {
        // 检查名称是否已存在
        if (storageBackendRepository.existsByBackendName(request.getBackendName())) {
            throw new BizException("后端名称已存在");
        }

        // 自动生成后端编码：类型前缀 + 随机6位
        String backendCode = generateBackendCode(request.getBackendType());

        StorageBackend backend = new StorageBackend();
        backend.setBackendCode(backendCode);
        backend.setBackendName(request.getBackendName());
        backend.setBackendType(request.getBackendType());
        backend.setEndpoint(request.getEndpoint());
        backend.setAccessKey(request.getAccessKey());
        backend.setNamespace(request.getNamespace());
        backend.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);
        backend.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        backend.setDescription(request.getDescription());
        backend.setCreatedBy(createdBy);
        backend.setHealthStatus(HealthStatus.UNKNOWN);

        // 加密密码
        if (request.getSecretKey() != null) {
            backend.setSecretKey(passwordEncryptor.encrypt(request.getSecretKey()));
        }

        // 处理配置JSON
        if (request.getConfig() != null) {
            try {
                backend.setConfigJson(objectMapper.writeValueAsString(request.getConfig()));
            } catch (JsonProcessingException e) {
                throw new BizException("配置格式错误");
            }
        }

        // 如果设置为默认，取消其他默认设置
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            unsetOtherDefaults();
        }

        StorageBackend saved = storageBackendRepository.save(backend);
        return convertToDTO(saved);
    }

    /**
     * 更新存储后端
     */
    @Transactional
    public StorageBackendDTO update(Long id, UpdateStorageBackendRequest request) {
        StorageBackend backend = storageBackendRepository.findById(id)
            .orElseThrow(() -> new BizException("存储后端不存在"));

        // 检查名称是否与其他冲突
        storageBackendRepository.findByBackendName(request.getBackendName())
            .ifPresent(other -> {
                if (!other.getId().equals(id)) {
                    throw new BizException("后端名称已存在");
                }
            });

        // 更新字段
        backend.setBackendName(request.getBackendName());
        backend.setEndpoint(request.getEndpoint());
        backend.setAccessKey(request.getAccessKey());
        backend.setNamespace(request.getNamespace());
        backend.setIsDefault(request.getIsDefault());
        backend.setEnabled(request.getEnabled());
        backend.setDescription(request.getDescription());

        // 密码更新（如果提供了新密码）
        if (request.getSecretKey() != null && !request.getSecretKey().isEmpty()) {
            backend.setSecretKey(passwordEncryptor.encrypt(request.getSecretKey()));
        }

        // 处理配置JSON
        if (request.getConfig() != null) {
            try {
                backend.setConfigJson(objectMapper.writeValueAsString(request.getConfig()));
            } catch (JsonProcessingException e) {
                throw new BizException("配置格式错误");
            }
        }

        // 如果设置为默认，取消其他默认设置
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            unsetOtherDefaults(id);
        }

        // 重置健康状态，下次检查时更新
        backend.setHealthStatus(HealthStatus.UNKNOWN);
        backend.setErrorMessage(null);

        StorageBackend saved = storageBackendRepository.save(backend);
        return convertToDTO(saved);
    }

    /**
     * 删除存储后端
     */
    @Transactional
    public void delete(Long id) {
        StorageBackend backend = storageBackendRepository.findById(id)
            .orElseThrow(() -> new BizException("存储后端不存在"));

        // 检查是否是默认后端
        if (Boolean.TRUE.equals(backend.getIsDefault())) {
            throw new BizException("默认存储后端不能删除，请先设置其他后端为默认");
        }

        // 检查是否已启用
        if (Boolean.TRUE.equals(backend.getEnabled())) {
            throw new BizException("已启用的存储后端不能删除，请先停用后再删除");
        }

        storageBackendRepository.delete(backend);
    }

    /**
     * 连接测试
     */
    public ConnectionTestResultDTO testConnection(TestConnectionRequest request) {
        return switch (request.getBackendType()) {
            case HARBOR -> testHarborConnection(request);
            case NEXUS -> testNexusConnection(request);
            case NAS -> testNasConnection(request);
        };
    }

    /**
     * 健康检查
     */
    public HealthCheckResultDTO healthCheck(Long id) {
        StorageBackend backend = storageBackendRepository.findById(id)
            .orElseThrow(() -> new BizException("存储后端不存在"));

        long startTime = System.currentTimeMillis();
        ConnectionTestResultDTO result;

        try {
            switch (backend.getBackendType()) {
                case HARBOR -> {
                    HarborConfig config = HarborConfig.fromEndpoint(
                        backend.getEndpoint(),
                        backend.getAccessKey(),
                        passwordEncryptor.decrypt(backend.getSecretKey())
                    );
                    result = harborClient.testConnection(config);
                }
                case NEXUS -> {
                    NexusConfig config = NexusConfig.fromEndpoint(
                        backend.getEndpoint(),
                        backend.getAccessKey(),
                        passwordEncryptor.decrypt(backend.getSecretKey())
                    );
                    result = nexusClient.testConnection(config);
                }
                case NAS -> result = ConnectionTestResultDTO.success("NAS存储连接正常");
                default -> throw new BizException("不支持的存储类型");
            }

            long responseTime = System.currentTimeMillis() - startTime;

            // 更新健康状态
            if (result.isSuccess()) {
                backend.markOnline();
            } else {
                backend.markOffline(result.getMessage());
            }
            storageBackendRepository.save(backend);

            HealthCheckResultDTO dto = new HealthCheckResultDTO();
            dto.setId(id);
            dto.setHealthStatus(backend.getHealthStatus());
            dto.setLastHealthCheck(backend.getLastHealthCheck());
            dto.setErrorMessage(backend.getErrorMessage());
            dto.setResponseTimeMs(responseTime);
            return dto;

        } catch (Exception e) {
            logger.error("健康检查失败: {}", id, e);
            backend.markOffline(e.getMessage());
            storageBackendRepository.save(backend);

            HealthCheckResultDTO dto = new HealthCheckResultDTO();
            dto.setId(id);
            dto.setHealthStatus(HealthStatus.ERROR);
            dto.setLastHealthCheck(backend.getLastHealthCheck());
            dto.setErrorMessage(e.getMessage());
            dto.setResponseTimeMs(System.currentTimeMillis() - startTime);
            return dto;
        }
    }

    /**
     * 获取所有存储类型
     */
    public List<StorageTypeDTO> getStorageTypes() {
        return Arrays.stream(StorageBackendType.values())
            .map(StorageTypeDTO::new)
            .collect(Collectors.toList());
    }

    /**
     * 设为默认存储后端
     */
    @Transactional
    public StorageBackendDTO setDefault(Long id) {
        StorageBackend backend = storageBackendRepository.findById(id)
            .orElseThrow(() -> new BizException("存储后端不存在"));

        unsetOtherDefaults(id);
        backend.setAsDefault();
        StorageBackend saved = storageBackendRepository.save(backend);
        return convertToDTO(saved);
    }

    /**
     * 启用/停用存储后端
     */
    @Transactional
    public StorageBackendDTO setEnabled(Long id, boolean enabled) {
        StorageBackend backend = storageBackendRepository.findById(id)
            .orElseThrow(() -> new BizException("存储后端不存在"));

        if (enabled) {
            backend.enable();
        } else {
            // 不能停用默认后端
            if (Boolean.TRUE.equals(backend.getIsDefault())) {
                throw new BizException("默认存储后端不能停用，请先设置其他后端为默认");
            }
            backend.disable();
        }

        StorageBackend saved = storageBackendRepository.save(backend);
        return convertToDTO(saved);
    }

    // ============ 私有方法 ============

    /**
     * 自动生成唯一后端编码：类型前缀 + 随机6位字符
     * 例如：harbor-a1b2c3, nexus-x7k8m9, nas-p3q4r5
     */
    private String generateBackendCode(StorageBackendType type) {
        String prefix = type.name().toLowerCase();
        String code;
        do {
            String suffix = UUID.randomUUID().toString().substring(0, 6);
            code = prefix + "-" + suffix;
        } while (storageBackendRepository.existsByBackendCode(code));
        return code;
    }

    private ConnectionTestResultDTO testHarborConnection(TestConnectionRequest request) {
        HarborConfig config = HarborConfig.fromEndpoint(
            request.getEndpoint(),
            request.getAccessKey(),
            request.getSecretKey()
        );

        // 如果有配置，设置协议
        if (request.getConfig() instanceof Map) {
            Map<String, Object> cfg = (Map<String, Object>) request.getConfig();
            if (cfg.get("protocol") != null) {
                config.setProtocol((String) cfg.get("protocol"));
            }
        }

        return harborClient.testConnection(config);
    }

    private ConnectionTestResultDTO testNexusConnection(TestConnectionRequest request) {
        NexusConfig config = NexusConfig.fromEndpoint(
            request.getEndpoint(),
            request.getAccessKey(),
            request.getSecretKey()
        );

        // 如果有配置，设置协议
        if (request.getConfig() instanceof Map) {
            Map<String, Object> cfg = (Map<String, Object>) request.getConfig();
            if (cfg.get("protocol") != null) {
                config.setProtocol((String) cfg.get("protocol"));
            }
        }

        return nexusClient.testConnection(config);
    }

    private ConnectionTestResultDTO testNasConnection(TestConnectionRequest request) {
        // NAS 连接测试：检查路径是否可访问
        return ConnectionTestResultDTO.success("NAS存储连接测试通过（模拟）");
    }

    private void unsetOtherDefaults() {
        unsetOtherDefaults(null);
    }

    private void unsetOtherDefaults(Long excludeId) {
        storageBackendRepository.findByIsDefaultTrue().ifPresent(defaultBackend -> {
            if (excludeId == null || !defaultBackend.getId().equals(excludeId)) {
                defaultBackend.unsetDefault();
                storageBackendRepository.save(defaultBackend);
            }
        });
    }

    private StorageBackendDTO convertToDTO(StorageBackend backend) {
        StorageBackendDTO dto = new StorageBackendDTO();
        BeanUtils.copyProperties(backend, dto);

        // 密码不返回
        dto.setSecretKey(null);

        // 解析配置JSON
        if (backend.getConfigJson() != null) {
            try {
                dto.setConfig(objectMapper.readValue(backend.getConfigJson(), Map.class));
            } catch (JsonProcessingException e) {
                logger.warn("解析存储后端配置JSON失败: {}", backend.getId());
            }
        }

        return dto;
    }

    /**
     * 获取所有存储后端的健康状态
     */
    public List<HealthCheckResultDTO> getAllHealthStatus() {
        List<StorageBackend> backends = storageBackendRepository.findAll();
        return backends.stream()
            .map(backend -> {
                try {
                    return healthCheck(backend.getId());
                } catch (Exception e) {
                    HealthCheckResultDTO dto = new HealthCheckResultDTO();
                    dto.setId(backend.getId());
                    dto.setHealthStatus(HealthStatus.ERROR);
                    dto.setErrorMessage("检查失败: " + e.getMessage());
                    dto.setLastHealthCheck(java.time.LocalDateTime.now());
                    return dto;
                }
            })
            .collect(Collectors.toList());
    }
}
