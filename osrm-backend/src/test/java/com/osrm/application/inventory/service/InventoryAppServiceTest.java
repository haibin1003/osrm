package com.osrm.application.inventory.service;

import com.osrm.application.inventory.dto.request.CreateInventoryRequest;
import com.osrm.application.inventory.dto.request.RejectInventoryRequest;
import com.osrm.application.inventory.dto.response.InventoryDTO;
import com.osrm.common.exception.BizException;
import com.osrm.common.model.PageResult;
import com.osrm.domain.inventory.entity.InventoryRecord;
import com.osrm.domain.inventory.repository.InventoryRecordRepository;
import com.osrm.domain.system.entity.SystemSetting;
import com.osrm.domain.system.repository.SystemSettingRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryAppServiceTest {

    @Mock
    private InventoryRecordRepository inventoryRecordRepository;

    @Mock
    private SystemSettingRepository systemSettingRepository;

    @InjectMocks
    private InventoryAppService inventoryAppService;

    private InventoryRecord mockRecord;

    @BeforeEach
    void setUp() {
        mockRecord = new InventoryRecord();
        mockRecord.setId(1L);
        mockRecord.setRecordNo("INV-20260413-ABC12345");
        mockRecord.setUserId(1L);
        mockRecord.setPackageId(100L);
        mockRecord.setPackageName("MySQL");
        mockRecord.setVersionNo("8.0.31");
        mockRecord.setSoftwareType("DATABASE");
        mockRecord.setResponsiblePerson("张三");
        mockRecord.setBusinessSystemId(1L);
        mockRecord.setDeployEnvironment("PRODUCTION");
        mockRecord.setServerCount(5);
        mockRecord.setUsageScenario("数据库服务");
        mockRecord.setSourceType(InventoryRecord.SourceType.MANUAL);
        mockRecord.setStatus(InventoryRecord.InventoryStatus.PENDING);
        mockRecord.setCreatedAt(LocalDateTime.now());
        mockRecord.setUpdatedAt(LocalDateTime.now());
    }

    // ============ isInventoryFeatureEnabled ============

    @Test
    void isInventoryFeatureEnabled_whenEnabled_shouldReturnTrue() {
        SystemSetting setting = new SystemSetting();
        setting.setSettingValue("true");
        when(systemSettingRepository.findByCategoryAndSettingKey("INVENTORY", "ENABLE_INVENTORY_FEATURE"))
                .thenReturn(Optional.of(setting));

        boolean result = inventoryAppService.isInventoryFeatureEnabled();

        assertTrue(result);
    }

    @Test
    void isInventoryFeatureEnabled_whenDisabled_shouldReturnFalse() {
        SystemSetting setting = new SystemSetting();
        setting.setSettingValue("false");
        when(systemSettingRepository.findByCategoryAndSettingKey("INVENTORY", "ENABLE_INVENTORY_FEATURE"))
                .thenReturn(Optional.of(setting));

        boolean result = inventoryAppService.isInventoryFeatureEnabled();

        assertFalse(result);
    }

    @Test
    void isInventoryFeatureEnabled_whenNotConfigured_shouldReturnFalse() {
        when(systemSettingRepository.findByCategoryAndSettingKey("INVENTORY", "ENABLE_INVENTORY_FEATURE"))
                .thenReturn(Optional.empty());

        boolean result = inventoryAppService.isInventoryFeatureEnabled();

        assertFalse(result);
    }

    // ============ getMyInventory ============

    @Test
    void getMyInventory_withStatus_shouldReturnFilteredResults() {
        Page<InventoryRecord> page = new PageImpl<>(List.of(mockRecord));
        when(inventoryRecordRepository.findByUserIdAndStatus(eq(1L), eq(InventoryRecord.InventoryStatus.PENDING), any(Pageable.class)))
                .thenReturn(page);

        PageResult<InventoryDTO> result = inventoryAppService.getMyInventory(1L, "PENDING", 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("MySQL", result.getContent().get(0).getPackageName());
    }

    @Test
    void getMyInventory_withoutStatus_shouldReturnAllResults() {
        Page<InventoryRecord> page = new PageImpl<>(List.of(mockRecord));
        when(inventoryRecordRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        PageResult<InventoryDTO> result = inventoryAppService.getMyInventory(1L, null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ============ getPendingInventory ============

    @Test
    void getPendingInventory_shouldReturnPagedResults() {
        Page<InventoryRecord> page = new PageImpl<>(List.of(mockRecord));
        when(inventoryRecordRepository.findByStatus(eq(InventoryRecord.InventoryStatus.PENDING), any(Pageable.class)))
                .thenReturn(page);

        PageResult<InventoryDTO> result = inventoryAppService.getPendingInventory(1, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("PENDING", result.getContent().get(0).getStatus());
    }

    // ============ getAllInventory ============

    @Test
    void getAllInventory_shouldReturnPagedResults() {
        Page<InventoryRecord> page = new PageImpl<>(List.of(mockRecord));
        // Use nullable() to match null parameters
        when(inventoryRecordRepository.findByConditions(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        PageResult<InventoryDTO> result = inventoryAppService.getAllInventory(null, null, null, null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ============ getInventoryById ============

    @Test
    void getInventoryById_asOwner_shouldReturnDTO() {
        when(inventoryRecordRepository.findById(1L)).thenReturn(Optional.of(mockRecord));

        InventoryDTO dto = inventoryAppService.getInventoryById(1L, 1L, false);

        assertNotNull(dto);
        assertEquals("MySQL", dto.getPackageName());
    }

    @Test
    void getInventoryById_asAdmin_shouldReturnDTO() {
        when(inventoryRecordRepository.findById(1L)).thenReturn(Optional.of(mockRecord));

        InventoryDTO dto = inventoryAppService.getInventoryById(1L, 999L, true);

        assertNotNull(dto);
        assertEquals("MySQL", dto.getPackageName());
    }

    @Test
    void getInventoryById_asNonOwner_shouldThrowException() {
        when(inventoryRecordRepository.findById(1L)).thenReturn(Optional.of(mockRecord));

        BizException exception = assertThrows(BizException.class,
                () -> inventoryAppService.getInventoryById(1L, 999L, false));
        assertEquals("无权查看此记录", exception.getMessage());
    }

    @Test
    void getInventoryById_withNonExistingId_shouldThrowException() {
        when(inventoryRecordRepository.findById(999L)).thenReturn(Optional.empty());

        BizException exception = assertThrows(BizException.class,
                () -> inventoryAppService.getInventoryById(999L, 1L, false));
        assertEquals("存量登记记录不存在", exception.getMessage());
    }

    // ============ createInventory ============

    @Test
    void createInventory_whenEnabled_shouldCreateRecord() {
        SystemSetting setting = new SystemSetting();
        setting.setSettingValue("true");
        when(systemSettingRepository.findByCategoryAndSettingKey("INVENTORY", "ENABLE_INVENTORY_FEATURE"))
                .thenReturn(Optional.of(setting));
        when(inventoryRecordRepository.save(any(InventoryRecord.class)))
                .thenAnswer(invocation -> {
                    InventoryRecord saved = invocation.getArgument(0);
                    saved.setId(2L);
                    return saved;
                });

        CreateInventoryRequest request = new CreateInventoryRequest();
        request.setPackageName("PostgreSQL");
        request.setVersionNo("16.1");
        request.setSoftwareType("DATABASE");
        request.setResponsiblePerson("李四");
        request.setServerCount(3);

        InventoryDTO dto = inventoryAppService.createInventory(request, 2L, "李四");

        assertNotNull(dto);
        assertEquals("PostgreSQL", dto.getPackageName());
        assertEquals("PENDING", dto.getStatus());
        verify(inventoryRecordRepository).save(any(InventoryRecord.class));
    }

    @Test
    void createInventory_whenDisabled_shouldThrowException() {
        SystemSetting setting = new SystemSetting();
        setting.setSettingValue("false");
        when(systemSettingRepository.findByCategoryAndSettingKey("INVENTORY", "ENABLE_INVENTORY_FEATURE"))
                .thenReturn(Optional.of(setting));

        CreateInventoryRequest request = new CreateInventoryRequest();
        request.setPackageName("PostgreSQL");

        BizException exception = assertThrows(BizException.class,
                () -> inventoryAppService.createInventory(request, 1L, "张三"));
        assertEquals("存量登记功能已关闭", exception.getMessage());
    }

    // ============ updateInventory ============

    @Test
    void updateInventory_asOwner_shouldUpdateRecord() {
        when(inventoryRecordRepository.findById(1L)).thenReturn(Optional.of(mockRecord));
        when(inventoryRecordRepository.save(any(InventoryRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateInventoryRequest request = new CreateInventoryRequest();
        request.setPackageName("MySQL Updated");
        request.setVersionNo("8.0.32");

        InventoryDTO dto = inventoryAppService.updateInventory(1L, request, 1L, false);

        assertNotNull(dto);
        assertEquals("MySQL Updated", mockRecord.getPackageName());
    }

    @Test
    void updateInventory_asNonOwner_shouldThrowException() {
        when(inventoryRecordRepository.findById(1L)).thenReturn(Optional.of(mockRecord));

        CreateInventoryRequest request = new CreateInventoryRequest();
        request.setPackageName("MySQL Updated");

        BizException exception = assertThrows(BizException.class,
                () -> inventoryAppService.updateInventory(1L, request, 999L, false));
        assertEquals("无权修改此记录", exception.getMessage());
    }

    @Test
    void updateInventory_withApprovedStatus_shouldThrowException() {
        mockRecord.setStatus(InventoryRecord.InventoryStatus.APPROVED);
        when(inventoryRecordRepository.findById(1L)).thenReturn(Optional.of(mockRecord));

        CreateInventoryRequest request = new CreateInventoryRequest();
        request.setPackageName("MySQL Updated");

        BizException exception = assertThrows(BizException.class,
                () -> inventoryAppService.updateInventory(1L, request, 1L, false));
        assertEquals("只有待审批状态的记录可以修改", exception.getMessage());
    }

    // ============ approveInventory ============

    @Test
    void approveInventory_withPendingStatus_shouldApproveRecord() {
        when(inventoryRecordRepository.findById(1L)).thenReturn(Optional.of(mockRecord));
        when(inventoryRecordRepository.save(any(InventoryRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventoryDTO dto = inventoryAppService.approveInventory(1L, 999L);

        assertNotNull(dto);
        assertEquals("APPROVED", dto.getStatus());
        assertEquals(999L, dto.getApprovedBy());
    }

    @Test
    void approveInventory_withNonPendingStatus_shouldThrowException() {
        mockRecord.setStatus(InventoryRecord.InventoryStatus.APPROVED);
        when(inventoryRecordRepository.findById(1L)).thenReturn(Optional.of(mockRecord));

        BizException exception = assertThrows(BizException.class,
                () -> inventoryAppService.approveInventory(1L, 999L));
        assertEquals("只有待审批状态的记录可以批准", exception.getMessage());
    }

    // ============ rejectInventory ============

    @Test
    void rejectInventory_withPendingStatus_shouldRejectRecord() {
        when(inventoryRecordRepository.findById(1L)).thenReturn(Optional.of(mockRecord));
        when(inventoryRecordRepository.save(any(InventoryRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RejectInventoryRequest request = new RejectInventoryRequest();
        request.setReason("信息不全");

        InventoryDTO dto = inventoryAppService.rejectInventory(1L, request, 999L);

        assertNotNull(dto);
        assertEquals("REJECTED", dto.getStatus());
        assertEquals("信息不全", dto.getRejectReason());
    }

    @Test
    void rejectInventory_withNonPendingStatus_shouldThrowException() {
        mockRecord.setStatus(InventoryRecord.InventoryStatus.APPROVED);
        when(inventoryRecordRepository.findById(1L)).thenReturn(Optional.of(mockRecord));

        RejectInventoryRequest request = new RejectInventoryRequest();
        request.setReason("信息不全");

        BizException exception = assertThrows(BizException.class,
                () -> inventoryAppService.rejectInventory(1L, request, 999L));
        assertEquals("只有待审批状态的记录可以驳回", exception.getMessage());
    }

    // ============ deleteInventory ============

    @Test
    void deleteInventory_asOwner_shouldDeleteRecord() {
        when(inventoryRecordRepository.findById(1L)).thenReturn(Optional.of(mockRecord));

        inventoryAppService.deleteInventory(1L, 1L, false);

        verify(inventoryRecordRepository).delete(mockRecord);
    }

    @Test
    void deleteInventory_asAdmin_shouldDeleteRecord() {
        when(inventoryRecordRepository.findById(1L)).thenReturn(Optional.of(mockRecord));

        inventoryAppService.deleteInventory(1L, 999L, true);

        verify(inventoryRecordRepository).delete(mockRecord);
    }

    @Test
    void deleteInventory_asNonOwner_shouldThrowException() {
        when(inventoryRecordRepository.findById(1L)).thenReturn(Optional.of(mockRecord));

        BizException exception = assertThrows(BizException.class,
                () -> inventoryAppService.deleteInventory(1L, 999L, false));
        assertEquals("无权删除此记录", exception.getMessage());
    }

    @Test
    void deleteInventory_withNonExistingId_shouldThrowException() {
        when(inventoryRecordRepository.findById(999L)).thenReturn(Optional.empty());

        BizException exception = assertThrows(BizException.class,
                () -> inventoryAppService.deleteInventory(999L, 1L, false));
        assertEquals("存量登记记录不存在", exception.getMessage());
    }
}
