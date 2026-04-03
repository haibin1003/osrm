package com.osrm.application.system.service;

import com.osrm.application.system.dto.SettingDTO;
import com.osrm.domain.system.entity.SystemSetting;
import com.osrm.domain.system.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统设置应用服务
 */
@Service
@Transactional(readOnly = true)
public class SettingAppService {

    private final SystemSettingRepository settingRepository;

    public SettingAppService(SystemSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    public Map<String, Map<String, String>> getAllSettings() {
        List<SystemSetting> allSettings = settingRepository.findAll();
        Map<String, Map<String, String>> result = new HashMap<>();

        for (SystemSetting setting : allSettings) {
            result.computeIfAbsent(setting.getCategory(), k -> new HashMap<>())
                    .put(setting.getSettingKey(), setting.getSettingValue());
        }

        return result;
    }

    public Map<String, String> getSettingsByCategory(String category) {
        List<SystemSetting> settings = settingRepository.findByCategory(category);
        return settings.stream()
                .collect(Collectors.toMap(
                        SystemSetting::getSettingKey,
                        s -> s.getSettingValue() != null ? s.getSettingValue() : ""
                ));
    }

    @Transactional
    public void updateSetting(String category, String key, String value) {
        SystemSetting setting = settingRepository
                .findByCategoryAndSettingKey(category, key)
                .orElse(new SystemSetting());

        setting.setCategory(category);
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        settingRepository.save(setting);
    }

    @Transactional
    public void updateSettings(String category, Map<String, String> settings) {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            updateSetting(category, entry.getKey(), entry.getValue());
        }
    }

    public List<SettingDTO> getSettingsByCategoryAsList(String category) {
        List<SystemSetting> settings = settingRepository.findByCategory(category);
        return settings.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private SettingDTO convertToDTO(SystemSetting setting) {
        SettingDTO dto = new SettingDTO();
        dto.setId(setting.getId());
        dto.setCategory(setting.getCategory());
        dto.setKey(setting.getSettingKey());
        dto.setValue(setting.getSettingValue());
        dto.setDescription(setting.getDescription());
        dto.setUpdateTime(setting.getUpdatedAt());
        return dto;
    }
}
