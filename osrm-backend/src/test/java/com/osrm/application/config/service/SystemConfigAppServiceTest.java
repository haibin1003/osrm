package com.osrm.application.config.service;

import com.osrm.domain.config.entity.SystemConfig;
import com.osrm.domain.config.repository.SystemConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemConfigAppServiceTest {

    @Mock
    private SystemConfigRepository configRepository;

    @InjectMocks
    private SystemConfigAppService systemConfigAppService;

    @Test
    void getAllConfigs_shouldReturnWithDefaults() {
        SystemConfig c = new SystemConfig();
        c.setConfigKey("platform.name");
        c.setConfigValue("MyPlatform");

        when(configRepository.findAll()).thenReturn(List.of(c));

        Map<String, String> result = systemConfigAppService.getAllConfigs();
        assertEquals("MyPlatform", result.get("platform.name"));
        assertEquals("7d", result.get("token.defaultLifetime")); // default
        assertEquals("10", result.get("token.maxDownloads")); // default
    }

    @Test
    void getConfig_existing_shouldReturnValue() {
        SystemConfig c = new SystemConfig();
        c.setConfigValue("30d");

        when(configRepository.findByConfigKey("token.defaultLifetime")).thenReturn(Optional.of(c));
        assertEquals("30d", systemConfigAppService.getConfig("token.defaultLifetime"));
    }

    @Test
    void getConfig_nonExisting_shouldReturnDefault() {
        when(configRepository.findByConfigKey("token.defaultLifetime")).thenReturn(Optional.empty());
        assertEquals("7d", systemConfigAppService.getConfig("token.defaultLifetime"));
    }

    @Test
    void setConfig_shouldSave() {
        when(configRepository.findByConfigKey("token.defaultLifetime")).thenReturn(Optional.empty());
        when(configRepository.save(any(SystemConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        systemConfigAppService.setConfig("token.defaultLifetime", "30d", "令牌有效期");
        verify(configRepository).save(any(SystemConfig.class));
    }

    @Test
    void getTokenMaxDownloads_shouldReturnInt() {
        SystemConfig c = new SystemConfig();
        c.setConfigValue("20");

        when(configRepository.findByConfigKey("token.maxDownloads")).thenReturn(Optional.of(c));
        assertEquals(20, systemConfigAppService.getTokenMaxDownloads());
    }

    @Test
    void getTokenMaxDownloads_invalid_shouldReturnDefault() {
        SystemConfig c = new SystemConfig();
        c.setConfigValue("abc");

        when(configRepository.findByConfigKey("token.maxDownloads")).thenReturn(Optional.of(c));
        assertEquals(10, systemConfigAppService.getTokenMaxDownloads());
    }
}
