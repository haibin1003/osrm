package com.osrm.application.config.service;

import com.osrm.common.exception.BizException;
import com.osrm.domain.config.entity.SystemConfig;
import com.osrm.domain.config.repository.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class SystemConfigAppService {

    private final SystemConfigRepository configRepository;

    // 默认值
    private static final Map<String, String> DEFAULTS = Map.of(
            "token.defaultLifetime", "7d",
            "token.maxDownloads", "10",
            "platform.name", "OSRM"
    );

    @Autowired
    public SystemConfigAppService(SystemConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    public Map<String, String> getAllConfigs() {
        Map<String, String> result = new HashMap<>(DEFAULTS);
        configRepository.findAll().forEach(c -> result.put(c.getConfigKey(), c.getConfigValue()));
        return result;
    }

    public String getConfig(String key) {
        return configRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(DEFAULTS.get(key));
    }

    @Transactional
    public void setConfig(String key, String value, String description) {
        SystemConfig config = configRepository.findByConfigKey(key)
                .orElse(new SystemConfig());
        config.setConfigKey(key);
        config.setConfigValue(value);
        if (description != null) {
            config.setDescription(description);
        }
        configRepository.save(config);
    }

    @Transactional
    public void updateConfigs(Map<String, String> configs) {
        configs.forEach((key, value) -> {
            if (DEFAULTS.containsKey(key)) {
                setConfig(key, value, null);
            }
        });
    }

    public String getTokenLifetime() {
        return getConfig("token.defaultLifetime");
    }

    public int getTokenMaxDownloads() {
        String val = getConfig("token.maxDownloads");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 10;
        }
    }
}
