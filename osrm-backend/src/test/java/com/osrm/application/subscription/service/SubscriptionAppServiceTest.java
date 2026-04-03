package com.osrm.application.subscription.service;

import com.osrm.application.subscription.dto.request.CreateSubscriptionRequest;
import com.osrm.application.subscription.dto.response.DownloadTokenDTO;
import com.osrm.application.subscription.dto.response.SubscriptionDTO;
import com.osrm.common.exception.BizException;
import com.osrm.common.model.PageResult;
import com.osrm.domain.business.entity.BusinessSystem;
import com.osrm.domain.business.repository.BusinessSystemRepository;
import com.osrm.domain.software.entity.PackageStatus;
import com.osrm.domain.software.entity.SoftwarePackage;
import com.osrm.domain.software.entity.SoftwareType;
import com.osrm.domain.software.entity.SoftwareVersion;
import com.osrm.domain.software.repository.SoftwarePackageRepository;
import com.osrm.domain.software.repository.SoftwareVersionRepository;
import com.osrm.domain.subscription.entity.DownloadToken;
import com.osrm.domain.subscription.entity.Subscription;
import com.osrm.domain.subscription.entity.SubscriptionStatus;
import com.osrm.domain.subscription.repository.DownloadTokenRepository;
import com.osrm.domain.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionAppServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private DownloadTokenRepository downloadTokenRepository;
    @Mock private SoftwarePackageRepository softwarePackageRepository;
    @Mock private SoftwareVersionRepository softwareVersionRepository;
    @Mock private BusinessSystemRepository businessSystemRepository;
    @InjectMocks private SubscriptionAppService service;

    private SoftwarePackage mockPkg;
    private BusinessSystem mockSystem;
    private Subscription mockSub;

    @BeforeEach
    void setUp() {
        mockPkg = new SoftwarePackage();
        mockPkg.setId(1L);
        mockPkg.setPackageName("my-app");
        mockPkg.setSoftwareType(SoftwareType.DOCKER_IMAGE);
        mockPkg.setStatus(PackageStatus.PUBLISHED);

        mockSystem = new BusinessSystem();
        mockSystem.setId(1L);
        mockSystem.setSystemCode("sys-001");
        mockSystem.setSystemName("业务系统A");

        mockSub = new Subscription();
        mockSub.setId(1L);
        mockSub.setPackageId(1L);
        mockSub.setBusinessSystemId(1L);
        mockSub.setUserId(1L);
        mockSub.setVersionId(1L);
        mockSub.setStatus(SubscriptionStatus.PENDING);
    }

    @Test
    void findByUserId_shouldReturnPaged() {
        Page<Subscription> page = new PageImpl<>(List.of(mockSub));
        when(subscriptionRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(page);
        PageResult<SubscriptionDTO> result = service.findByUserId(1L, 1, 10);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findByStatus_shouldReturnPaged() {
        Page<Subscription> page = new PageImpl<>(List.of(mockSub));
        when(subscriptionRepository.findByStatus(eq(SubscriptionStatus.PENDING), any(Pageable.class))).thenReturn(page);
        PageResult<SubscriptionDTO> result = service.findByStatus(SubscriptionStatus.PENDING, 1, 10);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void create_shouldCreate() {
        CreateSubscriptionRequest req = new CreateSubscriptionRequest();
        req.setPackageId(1L);
        req.setBusinessSystemId(1L);
        req.setUseScene("测试场景");

        SoftwareVersion latestVer = new SoftwareVersion();
        latestVer.setId(1L);
        latestVer.setVersionNo("1.0.0");

        when(softwarePackageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        when(businessSystemRepository.findById(1L)).thenReturn(Optional.of(mockSystem));
        when(softwareVersionRepository.findLatestVersionByPackageId(1L)).thenReturn(Optional.of(latestVer));
        when(subscriptionRepository.existsByPackageIdAndBusinessSystemIdAndStatus(1L, 1L, SubscriptionStatus.APPROVED)).thenReturn(false);
        when(subscriptionRepository.save(any())).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        SubscriptionDTO dto = service.create(req, 1L);
        assertEquals(1L, dto.getPackageId());
        assertEquals(SubscriptionStatus.PENDING, dto.getStatus());
    }

    @Test
    void create_packageNotFound_shouldThrow() {
        CreateSubscriptionRequest req = new CreateSubscriptionRequest();
        req.setPackageId(999L);
        req.setBusinessSystemId(1L);

        when(softwarePackageRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> service.create(req, 1L));
    }

    @Test
    void create_systemNotFound_shouldThrow() {
        CreateSubscriptionRequest req = new CreateSubscriptionRequest();
        req.setPackageId(1L);
        req.setBusinessSystemId(999L);

        when(softwarePackageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        when(businessSystemRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> service.create(req, 1L));
    }

    @Test
    void create_duplicateApproved_shouldThrow() {
        CreateSubscriptionRequest req = new CreateSubscriptionRequest();
        req.setPackageId(1L);
        req.setBusinessSystemId(1L);

        when(softwarePackageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        when(businessSystemRepository.findById(1L)).thenReturn(Optional.of(mockSystem));
        when(subscriptionRepository.existsByPackageIdAndBusinessSystemIdAndStatus(1L, 1L, SubscriptionStatus.APPROVED)).thenReturn(true);
        assertThrows(BizException.class, () -> service.create(req, 1L));
    }

    @Test
    void approve_shouldApprove() {
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(mockSub));
        when(downloadTokenRepository.findBySubscriptionId(1L)).thenReturn(Optional.empty());
        when(downloadTokenRepository.save(any())).thenAnswer(inv -> {
            DownloadToken t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        SubscriptionDTO dto = service.approve(1L, 2L);
        assertEquals(SubscriptionStatus.APPROVED, dto.getStatus());
        verify(downloadTokenRepository).save(any(DownloadToken.class));
    }

    @Test
    void approve_nonPending_shouldThrow() {
        mockSub.setStatus(SubscriptionStatus.APPROVED);
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(mockSub));
        assertThrows(BizException.class, () -> service.approve(1L, 2L));
    }

    @Test
    void reject_shouldReject() {
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(mockSub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionDTO dto = service.reject(1L, 2L, "不符合要求");
        assertEquals(SubscriptionStatus.REJECTED, dto.getStatus());
    }

    @Test
    void reject_nonPending_shouldThrow() {
        mockSub.setStatus(SubscriptionStatus.APPROVED);
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(mockSub));
        assertThrows(BizException.class, () -> service.reject(1L, 2L, "理由"));
    }

    @Test
    void getToken_shouldReturn() {
        DownloadToken token = new DownloadToken();
        token.setId(1L);
        token.setSubscriptionId(1L);
        token.setToken("abc123");
        token.setExpireAt(LocalDateTime.now().plusDays(30));
        token.setMaxDownloads(10);
        token.setUsedCount(0);
        token.setEnabled(true);

        when(downloadTokenRepository.findBySubscriptionId(1L)).thenReturn(Optional.of(token));
        DownloadTokenDTO dto = service.getToken(1L);
        assertEquals("abc123", dto.getToken());
    }

    @Test
    void getToken_notFound_shouldThrow() {
        when(downloadTokenRepository.findBySubscriptionId(1L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> service.getToken(1L));
    }

    @Test
    void revokeToken_shouldDisable() {
        DownloadToken token = new DownloadToken();
        token.setId(1L);
        token.setEnabled(true);
        when(downloadTokenRepository.findBySubscriptionId(1L)).thenReturn(Optional.of(token));

        service.revokeToken(1L);
        assertFalse(token.getEnabled());
        verify(downloadTokenRepository).save(token);
    }

    @Test
    void create_withVersion_shouldSetVersion() {
        CreateSubscriptionRequest req = new CreateSubscriptionRequest();
        req.setPackageId(1L);
        req.setBusinessSystemId(1L);
        req.setVersionId(10L);
        req.setUseScene("测试");

        SoftwareVersion ver = new SoftwareVersion();
        ver.setId(10L);
        ver.setVersionNo("1.0.0");

        when(softwarePackageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        when(businessSystemRepository.findById(1L)).thenReturn(Optional.of(mockSystem));
        when(subscriptionRepository.existsByPackageIdAndBusinessSystemIdAndStatus(1L, 1L, SubscriptionStatus.APPROVED)).thenReturn(false);
        when(softwareVersionRepository.findById(10L)).thenReturn(Optional.of(ver));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        SubscriptionDTO dto = service.create(req, 1L);
        assertEquals("1.0.0", dto.getVersionNumber());
    }
}
