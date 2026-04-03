package com.osrm.application.storage.service;

import com.osrm.application.storage.dto.StorageBackendDTO;
import com.osrm.application.storage.dto.StorageTypeDTO;
import com.osrm.application.storage.dto.request.CreateStorageBackendRequest;
import com.osrm.application.storage.dto.request.UpdateStorageBackendRequest;
import com.osrm.common.exception.BizException;
import com.osrm.common.model.PageResult;
import com.osrm.domain.storage.entity.HealthStatus;
import com.osrm.domain.storage.entity.StorageBackend;
import com.osrm.domain.storage.entity.StorageBackendType;
import com.osrm.domain.storage.repository.StorageBackendRepository;
import com.osrm.infrastructure.config.PasswordEncryptor;
import com.osrm.infrastructure.storage.client.HarborClient;
import com.osrm.infrastructure.storage.client.NexusClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 存储后端应用服务测试类
 */
@ExtendWith(MockitoExtension.class)
class StorageBackendAppServiceTest {

    @Mock
    private StorageBackendRepository storageBackendRepository;

    @Mock
    private HarborClient harborClient;

    @Mock
    private NexusClient nexusClient;

    @Mock
    private PasswordEncryptor passwordEncryptor;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private StorageBackendAppService storageBackendAppService;

    private StorageBackend mockBackend;

    @BeforeEach
    void setUp() {
        mockBackend = new StorageBackend();
        mockBackend.setId(1L);
        mockBackend.setBackendCode("harbor-prod");
        mockBackend.setBackendName("生产Harbor");
        mockBackend.setBackendType(StorageBackendType.HARBOR);
        mockBackend.setEndpoint("https://harbor.example.com");
        mockBackend.setAccessKey("admin");
        mockBackend.setSecretKey("encrypted-password");
        mockBackend.setNamespace("library");
        mockBackend.setIsDefault(false);
        mockBackend.setEnabled(true);
        mockBackend.setHealthStatus(HealthStatus.UNKNOWN);
        mockBackend.setDescription("生产环境Harbor");
        mockBackend.setCreatedBy(1L);
        mockBackend.setCreatedAt(LocalDateTime.now());
        mockBackend.setUpdatedAt(LocalDateTime.now());
    }

    // ============ findByConditions ============

    @Test
    void findByConditions_shouldReturnPagedResults() {
        // Given
        Page<StorageBackend> page = new PageImpl<>(List.of(mockBackend));
        when(storageBackendRepository.findByConditions(
            isNull(), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(page);

        // When
        PageResult<StorageBackendDTO> result = storageBackendAppService.findByConditions(
            null, null, null, 1, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("harbor-prod", result.getContent().get(0).getBackendCode());
    }

    @Test
    void findByConditions_withKeyword_shouldFilter() {
        // Given
        Page<StorageBackend> page = new PageImpl<>(List.of(mockBackend));
        when(storageBackendRepository.findByConditions(
            eq("harbor"), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(page);

        // When
        PageResult<StorageBackendDTO> result = storageBackendAppService.findByConditions(
            "harbor", null, null, 1, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findByConditions_withType_shouldFilter() {
        // Given
        Page<StorageBackend> page = new PageImpl<>(List.of(mockBackend));
        when(storageBackendRepository.findByConditions(
            isNull(), eq(StorageBackendType.HARBOR), isNull(), any(Pageable.class)))
            .thenReturn(page);

        // When
        PageResult<StorageBackendDTO> result = storageBackendAppService.findByConditions(
            null, "HARBOR", null, 1, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ============ findById ============

    @Test
    void findById_withExistingId_shouldReturnDTO() {
        // Given
        when(storageBackendRepository.findById(1L)).thenReturn(Optional.of(mockBackend));

        // When
        StorageBackendDTO dto = storageBackendAppService.findById(1L);

        // Then
        assertNotNull(dto);
        assertEquals("harbor-prod", dto.getBackendCode());
        assertEquals("生产Harbor", dto.getBackendName());
        assertNull(dto.getSecretKey());
    }

    @Test
    void findById_withNonExistingId_shouldThrowException() {
        // Given
        when(storageBackendRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        BizException exception = assertThrows(BizException.class,
            () -> storageBackendAppService.findById(999L));
        assertEquals("存储后端不存在", exception.getMessage());
    }

    // ============ create ============

    @Test
    void create_withValidRequest_shouldCreateBackend() {
        // Given
        CreateStorageBackendRequest request = new CreateStorageBackendRequest();
        request.setBackendName("开发Nexus");
        request.setBackendType(StorageBackendType.NEXUS);
        request.setEndpoint("https://nexus.example.com");
        request.setAccessKey("admin");
        request.setSecretKey("password123");
        request.setNamespace("maven-releases");
        request.setIsDefault(false);
        request.setDescription("开发环境Nexus");

        when(storageBackendRepository.existsByBackendCode(anyString())).thenReturn(false);
        when(storageBackendRepository.existsByBackendName("开发Nexus")).thenReturn(false);
        when(passwordEncryptor.encrypt("password123")).thenReturn("encrypted-password");
        when(storageBackendRepository.save(any(StorageBackend.class)))
            .thenAnswer(invocation -> {
                StorageBackend saved = invocation.getArgument(0);
                saved.setId(2L);
                return saved;
            });

        // When
        StorageBackendDTO dto = storageBackendAppService.create(request, 1L);

        // Then
        assertNotNull(dto);
        assertNotNull(dto.getBackendCode());
        assertTrue(dto.getBackendCode().startsWith("nexus-"));
        verify(storageBackendRepository).save(any(StorageBackend.class));
        verify(passwordEncryptor).encrypt("password123");
    }

    @Test
    void create_withDuplicateName_shouldThrowException() {
        // Given
        CreateStorageBackendRequest request = new CreateStorageBackendRequest();
        request.setBackendName("生产Harbor");

        when(storageBackendRepository.existsByBackendName("生产Harbor")).thenReturn(true);

        // When & Then
        BizException exception = assertThrows(BizException.class,
            () -> storageBackendAppService.create(request, 1L));
        assertEquals("后端名称已存在", exception.getMessage());
    }

    @Test
    void create_withIsDefault_shouldUnsetOtherDefaults() {
        // Given
        CreateStorageBackendRequest request = new CreateStorageBackendRequest();
        request.setBackendName("主NAS");
        request.setBackendType(StorageBackendType.NAS);
        request.setEndpoint("/mnt/storage");
        request.setIsDefault(true);

        StorageBackend existingDefault = new StorageBackend();
        existingDefault.setId(1L);
        existingDefault.setIsDefault(true);

        when(storageBackendRepository.existsByBackendCode(anyString())).thenReturn(false);
        when(storageBackendRepository.existsByBackendName("主NAS")).thenReturn(false);
        when(storageBackendRepository.findByIsDefaultTrue()).thenReturn(Optional.of(existingDefault));
        when(storageBackendRepository.save(any(StorageBackend.class)))
            .thenAnswer(invocation -> {
                StorageBackend saved = invocation.getArgument(0);
                if (saved.getId() == null) saved.setId(2L);
                return saved;
            });

        // When
        storageBackendAppService.create(request, 1L);

        // Then
        verify(storageBackendRepository, atLeast(2)).save(any(StorageBackend.class));
        assertFalse(existingDefault.getIsDefault());
    }

    // ============ update ============

    @Test
    void update_withValidRequest_shouldUpdateBackend() {
        // Given
        UpdateStorageBackendRequest request = new UpdateStorageBackendRequest();
        request.setBackendName("更新后的Harbor");
        request.setEndpoint("https://harbor-new.example.com");
        request.setAccessKey("new-admin");
        request.setNamespace("new-library");
        request.setIsDefault(false);
        request.setEnabled(true);
        request.setDescription("更新后的描述");

        when(storageBackendRepository.findById(1L)).thenReturn(Optional.of(mockBackend));
        when(storageBackendRepository.findByBackendName("更新后的Harbor"))
            .thenReturn(Optional.empty());
        when(storageBackendRepository.save(any(StorageBackend.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        StorageBackendDTO dto = storageBackendAppService.update(1L, request);

        // Then
        assertNotNull(dto);
        assertEquals("更新后的Harbor", mockBackend.getBackendName());
        verify(storageBackendRepository).save(mockBackend);
    }

    @Test
    void update_withNonExistingId_shouldThrowException() {
        // Given
        UpdateStorageBackendRequest request = new UpdateStorageBackendRequest();
        request.setBackendName("名称");
        request.setEndpoint("https://example.com");

        when(storageBackendRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        BizException exception = assertThrows(BizException.class,
            () -> storageBackendAppService.update(999L, request));
        assertEquals("存储后端不存在", exception.getMessage());
    }

    @Test
    void update_withDuplicateName_shouldThrowException() {
        // Given
        UpdateStorageBackendRequest request = new UpdateStorageBackendRequest();
        request.setBackendName("其他后端名称");
        request.setEndpoint("https://example.com");

        StorageBackend otherBackend = new StorageBackend();
        otherBackend.setId(2L);
        otherBackend.setBackendName("其他后端名称");

        when(storageBackendRepository.findById(1L)).thenReturn(Optional.of(mockBackend));
        when(storageBackendRepository.findByBackendName("其他后端名称"))
            .thenReturn(Optional.of(otherBackend));

        // When & Then
        BizException exception = assertThrows(BizException.class,
            () -> storageBackendAppService.update(1L, request));
        assertEquals("后端名称已存在", exception.getMessage());
    }

    @Test
    void update_withNewSecretKey_shouldEncrypt() {
        // Given
        UpdateStorageBackendRequest request = new UpdateStorageBackendRequest();
        request.setBackendName("生产Harbor");
        request.setEndpoint("https://harbor.example.com");
        request.setSecretKey("newPassword");

        when(storageBackendRepository.findById(1L)).thenReturn(Optional.of(mockBackend));
        when(storageBackendRepository.findByBackendName("生产Harbor"))
            .thenReturn(Optional.of(mockBackend));
        when(passwordEncryptor.encrypt("newPassword")).thenReturn("new-encrypted");
        when(storageBackendRepository.save(any(StorageBackend.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        storageBackendAppService.update(1L, request);

        // Then
        verify(passwordEncryptor).encrypt("newPassword");
    }

    // ============ delete ============

    @Test
    void delete_withValidId_shouldDelete() {
        // Given
        mockBackend.setIsDefault(false);
        mockBackend.setEnabled(false);
        when(storageBackendRepository.findById(1L)).thenReturn(Optional.of(mockBackend));

        // When
        storageBackendAppService.delete(1L);

        // Then
        verify(storageBackendRepository).delete(mockBackend);
    }

    @Test
    void delete_withDefaultBackend_shouldThrowException() {
        // Given
        mockBackend.setIsDefault(true);
        when(storageBackendRepository.findById(1L)).thenReturn(Optional.of(mockBackend));

        // When & Then
        BizException exception = assertThrows(BizException.class,
            () -> storageBackendAppService.delete(1L));
        assertEquals("默认存储后端不能删除，请先设置其他后端为默认", exception.getMessage());
    }

    @Test
    void delete_withEnabledBackend_shouldThrowException() {
        // Given
        mockBackend.setIsDefault(false);
        mockBackend.setEnabled(true);
        when(storageBackendRepository.findById(1L)).thenReturn(Optional.of(mockBackend));

        // When & Then
        BizException exception = assertThrows(BizException.class,
            () -> storageBackendAppService.delete(1L));
        assertEquals("已启用的存储后端不能删除，请先停用后再删除", exception.getMessage());
    }

    @Test
    void delete_withNonExistingId_shouldThrowException() {
        // Given
        when(storageBackendRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        BizException exception = assertThrows(BizException.class,
            () -> storageBackendAppService.delete(999L));
        assertEquals("存储后端不存在", exception.getMessage());
    }

    // ============ setDefault ============

    @Test
    void setDefault_shouldSetDefaultAndUnsetOthers() {
        // Given
        StorageBackend oldDefault = new StorageBackend();
        oldDefault.setId(2L);
        oldDefault.setIsDefault(true);

        when(storageBackendRepository.findById(1L)).thenReturn(Optional.of(mockBackend));
        when(storageBackendRepository.findByIsDefaultTrue()).thenReturn(Optional.of(oldDefault));
        when(storageBackendRepository.save(any(StorageBackend.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        StorageBackendDTO dto = storageBackendAppService.setDefault(1L);

        // Then
        assertNotNull(dto);
        assertTrue(mockBackend.getIsDefault());
        assertFalse(oldDefault.getIsDefault());
    }

    @Test
    void setDefault_withNonExistingId_shouldThrowException() {
        // Given
        when(storageBackendRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        BizException exception = assertThrows(BizException.class,
            () -> storageBackendAppService.setDefault(999L));
        assertEquals("存储后端不存在", exception.getMessage());
    }

    // ============ setEnabled ============

    @Test
    void setEnabled_shouldToggleStatus() {
        // Given
        when(storageBackendRepository.findById(1L)).thenReturn(Optional.of(mockBackend));
        when(storageBackendRepository.save(any(StorageBackend.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        StorageBackendDTO dto = storageBackendAppService.setEnabled(1L, false);

        // Then
        assertNotNull(dto);
        assertFalse(mockBackend.getEnabled());
    }

    @Test
    void setEnabled_disableDefault_shouldThrowException() {
        // Given
        mockBackend.setIsDefault(true);
        when(storageBackendRepository.findById(1L)).thenReturn(Optional.of(mockBackend));

        // When & Then
        BizException exception = assertThrows(BizException.class,
            () -> storageBackendAppService.setEnabled(1L, false));
        assertEquals("默认存储后端不能停用，请先设置其他后端为默认", exception.getMessage());
    }

    // ============ getStorageTypes ============

    @Test
    void getStorageTypes_shouldReturnAllTypes() {
        // When
        List<StorageTypeDTO> types = storageBackendAppService.getStorageTypes();

        // Then
        assertNotNull(types);
        assertEquals(3, types.size());
        assertTrue(types.stream().anyMatch(t -> "HARBOR".equals(t.getCode())));
        assertTrue(types.stream().anyMatch(t -> "NEXUS".equals(t.getCode())));
        assertTrue(types.stream().anyMatch(t -> "NAS".equals(t.getCode())));
    }

    // ============ convertToDTO ============

    @Test
    void convertToDTO_shouldNotReturnSecretKey() {
        // Given
        when(storageBackendRepository.findById(1L)).thenReturn(Optional.of(mockBackend));

        // When
        StorageBackendDTO dto = storageBackendAppService.findById(1L);

        // Then
        assertNull(dto.getSecretKey());
    }

    @Test
    void convertToDTO_shouldParseConfigJson() throws Exception {
        // Given
        mockBackend.setConfigJson("{\"protocol\":\"HTTPS\"}");
        when(storageBackendRepository.findById(1L)).thenReturn(Optional.of(mockBackend));
        when(objectMapper.readValue(eq("{\"protocol\":\"HTTPS\"}"), eq(Map.class)))
            .thenReturn(Map.of("protocol", "HTTPS"));

        // When
        StorageBackendDTO dto = storageBackendAppService.findById(1L);

        // Then
        assertNotNull(dto.getConfig());
    }
}
