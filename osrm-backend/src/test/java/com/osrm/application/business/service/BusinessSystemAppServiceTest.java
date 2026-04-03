package com.osrm.application.business.service;

import com.osrm.application.business.dto.request.CreateBusinessSystemRequest;
import com.osrm.application.business.dto.request.UpdateBusinessSystemRequest;
import com.osrm.application.business.dto.response.BusinessSystemDTO;
import com.osrm.common.exception.BizException;
import com.osrm.common.model.PageResult;
import com.osrm.domain.business.entity.BusinessDomain;
import com.osrm.domain.business.entity.BusinessSystem;
import com.osrm.domain.business.repository.BusinessSystemRepository;
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
class BusinessSystemAppServiceTest {

    @Mock
    private BusinessSystemRepository businessSystemRepository;

    @InjectMocks
    private BusinessSystemAppService businessSystemAppService;

    private BusinessSystem mockSystem;

    @BeforeEach
    void setUp() {
        mockSystem = new BusinessSystem();
        mockSystem.setId(1L);
        mockSystem.setSystemCode("order-sys");
        mockSystem.setSystemName("订单系统");
        mockSystem.setDomain(BusinessDomain.BUSINESS);
        mockSystem.setResponsiblePerson("张三");
        mockSystem.setDescription("订单管理系统");
        mockSystem.setEnabled(true);
        mockSystem.setCreatedBy(1L);
        mockSystem.setCreatedAt(LocalDateTime.now());
        mockSystem.setUpdatedAt(LocalDateTime.now());
    }

    // ============ findByConditions ============

    @Test
    void findByConditions_shouldReturnPagedResults() {
        Page<BusinessSystem> page = new PageImpl<>(List.of(mockSystem));
        when(businessSystemRepository.findByConditions(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        PageResult<BusinessSystemDTO> result = businessSystemAppService.findByConditions(null, null, null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("order-sys", result.getContent().get(0).getSystemCode());
    }

    @Test
    void findByConditions_withKeyword_shouldFilter() {
        Page<BusinessSystem> page = new PageImpl<>(List.of(mockSystem));
        when(businessSystemRepository.findByConditions(eq("order"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        PageResult<BusinessSystemDTO> result = businessSystemAppService.findByConditions("order", null, null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findByConditions_withDomain_shouldFilter() {
        Page<BusinessSystem> page = new PageImpl<>(List.of(mockSystem));
        when(businessSystemRepository.findByConditions(isNull(), eq(BusinessDomain.BUSINESS), isNull(), any(Pageable.class)))
                .thenReturn(page);

        PageResult<BusinessSystemDTO> result = businessSystemAppService.findByConditions(null, "BUSINESS", null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ============ findById ============

    @Test
    void findById_withExistingId_shouldReturnDTO() {
        when(businessSystemRepository.findById(1L)).thenReturn(Optional.of(mockSystem));

        BusinessSystemDTO dto = businessSystemAppService.findById(1L);

        assertNotNull(dto);
        assertEquals("order-sys", dto.getSystemCode());
        assertEquals("订单系统", dto.getSystemName());
        assertEquals("业务域", dto.getDomainName());
    }

    @Test
    void findById_withNonExistingId_shouldThrowException() {
        when(businessSystemRepository.findById(999L)).thenReturn(Optional.empty());

        BizException exception = assertThrows(BizException.class,
                () -> businessSystemAppService.findById(999L));
        assertEquals("业务系统不存在", exception.getMessage());
    }

    // ============ create ============

    @Test
    void create_withValidRequest_shouldCreateSystem() {
        CreateBusinessSystemRequest request = new CreateBusinessSystemRequest();
        request.setSystemCode("payment-sys");
        request.setSystemName("支付系统");
        request.setDomain(BusinessDomain.BUSINESS);
        request.setResponsiblePerson("李四");
        request.setDescription("支付管理系统");

        when(businessSystemRepository.existsBySystemCode("payment-sys")).thenReturn(false);
        when(businessSystemRepository.existsBySystemName("支付系统")).thenReturn(false);
        when(businessSystemRepository.save(any(BusinessSystem.class)))
                .thenAnswer(invocation -> {
                    BusinessSystem saved = invocation.getArgument(0);
                    saved.setId(2L);
                    return saved;
                });

        BusinessSystemDTO dto = businessSystemAppService.create(request, 1L);

        assertNotNull(dto);
        assertEquals("payment-sys", dto.getSystemCode());
        assertEquals("支付系统", dto.getSystemName());
        verify(businessSystemRepository).save(any(BusinessSystem.class));
    }

    @Test
    void create_withDuplicateCode_shouldThrowException() {
        CreateBusinessSystemRequest request = new CreateBusinessSystemRequest();
        request.setSystemCode("order-sys");
        request.setSystemName("新系统");

        when(businessSystemRepository.existsBySystemCode("order-sys")).thenReturn(true);

        BizException exception = assertThrows(BizException.class,
                () -> businessSystemAppService.create(request, 1L));
        assertEquals("系统编码已存在", exception.getMessage());
    }

    @Test
    void create_withDuplicateName_shouldThrowException() {
        CreateBusinessSystemRequest request = new CreateBusinessSystemRequest();
        request.setSystemCode("new-sys");
        request.setSystemName("订单系统");

        when(businessSystemRepository.existsBySystemCode("new-sys")).thenReturn(false);
        when(businessSystemRepository.existsBySystemName("订单系统")).thenReturn(true);

        BizException exception = assertThrows(BizException.class,
                () -> businessSystemAppService.create(request, 1L));
        assertEquals("系统名称已存在", exception.getMessage());
    }

    // ============ update ============

    @Test
    void update_withValidRequest_shouldUpdateSystem() {
        UpdateBusinessSystemRequest request = new UpdateBusinessSystemRequest();
        request.setSystemName("订单管理系统");
        request.setDomain(BusinessDomain.BUSINESS);
        request.setResponsiblePerson("王五");
        request.setDescription("更新后的描述");

        when(businessSystemRepository.findById(1L)).thenReturn(Optional.of(mockSystem));
        when(businessSystemRepository.findBySystemName("订单管理系统")).thenReturn(Optional.of(mockSystem));
        when(businessSystemRepository.save(any(BusinessSystem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BusinessSystemDTO dto = businessSystemAppService.update(1L, request);

        assertNotNull(dto);
        assertEquals("订单管理系统", mockSystem.getSystemName());
        verify(businessSystemRepository).save(mockSystem);
    }

    @Test
    void update_withNonExistingId_shouldThrowException() {
        UpdateBusinessSystemRequest request = new UpdateBusinessSystemRequest();
        request.setSystemName("名称");
        request.setDomain(BusinessDomain.BUSINESS);

        when(businessSystemRepository.findById(999L)).thenReturn(Optional.empty());

        BizException exception = assertThrows(BizException.class,
                () -> businessSystemAppService.update(999L, request));
        assertEquals("业务系统不存在", exception.getMessage());
    }

    @Test
    void update_withDuplicateName_shouldThrowException() {
        UpdateBusinessSystemRequest request = new UpdateBusinessSystemRequest();
        request.setSystemName("其他系统名称");
        request.setDomain(BusinessDomain.BUSINESS);

        BusinessSystem otherSystem = new BusinessSystem();
        otherSystem.setId(2L);
        otherSystem.setSystemName("其他系统名称");

        when(businessSystemRepository.findById(1L)).thenReturn(Optional.of(mockSystem));
        when(businessSystemRepository.findBySystemName("其他系统名称")).thenReturn(Optional.of(otherSystem));

        BizException exception = assertThrows(BizException.class,
                () -> businessSystemAppService.update(1L, request));
        assertEquals("系统名称已存在", exception.getMessage());
    }

    // ============ setEnabled ============

    @Test
    void setEnabled_shouldToggleStatus() {
        when(businessSystemRepository.findById(1L)).thenReturn(Optional.of(mockSystem));
        when(businessSystemRepository.save(any(BusinessSystem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BusinessSystemDTO dto = businessSystemAppService.setEnabled(1L, false);

        assertNotNull(dto);
        assertFalse(mockSystem.getEnabled());
    }

    @Test
    void setEnabled_withNonExistingId_shouldThrowException() {
        when(businessSystemRepository.findById(999L)).thenReturn(Optional.empty());

        BizException exception = assertThrows(BizException.class,
                () -> businessSystemAppService.setEnabled(999L, false));
        assertEquals("业务系统不存在", exception.getMessage());
    }

    // ============ delete ============

    @Test
    void delete_withDisabledSystem_shouldDelete() {
        mockSystem.setEnabled(false);
        when(businessSystemRepository.findById(1L)).thenReturn(Optional.of(mockSystem));

        businessSystemAppService.delete(1L);

        verify(businessSystemRepository).delete(mockSystem);
    }

    @Test
    void delete_withEnabledSystem_shouldThrowException() {
        when(businessSystemRepository.findById(1L)).thenReturn(Optional.of(mockSystem));

        BizException exception = assertThrows(BizException.class,
                () -> businessSystemAppService.delete(1L));
        assertEquals("已启用的业务系统不能删除，请先停用后再删除", exception.getMessage());
    }

    @Test
    void delete_withNonExistingId_shouldThrowException() {
        when(businessSystemRepository.findById(999L)).thenReturn(Optional.empty());

        BizException exception = assertThrows(BizException.class,
                () -> businessSystemAppService.delete(999L));
        assertEquals("业务系统不存在", exception.getMessage());
    }

    // ============ getAllDomains ============

    @Test
    void getAllDomains_shouldReturnAllDomains() {
        List<BusinessDomain> domains = businessSystemAppService.getAllDomains();

        assertNotNull(domains);
        assertEquals(5, domains.size());
    }
}
