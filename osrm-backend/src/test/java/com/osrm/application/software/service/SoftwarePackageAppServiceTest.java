package com.osrm.application.software.service;

import com.osrm.application.software.dto.request.CreatePackageRequest;
import com.osrm.application.software.dto.request.CreateVersionRequest;
import com.osrm.application.software.dto.request.UpdatePackageRequest;
import com.osrm.application.software.dto.response.SoftwarePackageDTO;
import com.osrm.application.software.dto.response.SoftwareVersionDTO;
import com.osrm.common.exception.BizException;
import com.osrm.common.model.PageResult;
import com.osrm.domain.software.entity.*;
import com.osrm.domain.software.repository.SoftwarePackageRepository;
import com.osrm.domain.software.repository.SoftwareVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SoftwarePackageAppServiceTest {

    @Mock private SoftwarePackageRepository packageRepository;
    @Mock private SoftwareVersionRepository versionRepository;
    @InjectMocks private SoftwarePackageAppService service;

    private SoftwarePackage mockPkg;

    @BeforeEach
    void setUp() {
        mockPkg = new SoftwarePackage();
        mockPkg.setId(1L);
        mockPkg.setPackageName("my-app");
        mockPkg.setPackageKey("my-app");
        mockPkg.setSoftwareType(SoftwareType.DOCKER_IMAGE);
        mockPkg.setStatus(PackageStatus.DRAFT);
        mockPkg.setVersions(new ArrayList<>());
        mockPkg.setCreatedBy(1L);
    }

    @Test
    void findByConditions_shouldReturnPaged() {
        Page<SoftwarePackage> page = new PageImpl<>(List.of(mockPkg));
        when(packageRepository.findByConditions(isNull(), isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(page);
        PageResult<SoftwarePackageDTO> result = service.findByConditions(null, null, null, null, null, 1, 10);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findById_shouldReturn() {
        when(packageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        SoftwarePackageDTO dto = service.findById(1L);
        assertEquals("my-app", dto.getPackageName());
    }

    @Test
    void findById_nonExisting_shouldThrow() {
        when(packageRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> service.findById(999L));
    }

    @Test
    void create_shouldCreate() {
        CreatePackageRequest req = new CreatePackageRequest();
        req.setPackageName("new-app");
        req.setPackageKey("new-app");
        req.setSoftwareType(SoftwareType.MAVEN);

        when(packageRepository.existsByPackageName("new-app")).thenReturn(false);
        when(packageRepository.existsByPackageKey("new-app")).thenReturn(false);
        when(packageRepository.save(any())).thenAnswer(inv -> { SoftwarePackage p = inv.getArgument(0); p.setId(2L); return p; });

        SoftwarePackageDTO dto = service.create(req, 1L);
        assertEquals("new-app", dto.getPackageName());
        assertEquals(PackageStatus.DRAFT, dto.getStatus());
    }

    @Test
    void create_duplicateName_shouldThrow() {
        CreatePackageRequest req = new CreatePackageRequest();
        req.setPackageName("my-app");
        req.setPackageKey("my-app2");
        req.setSoftwareType(SoftwareType.DOCKER_IMAGE);

        when(packageRepository.existsByPackageName("my-app")).thenReturn(true);
        assertThrows(BizException.class, () -> service.create(req, 1L));
    }

    @Test
    void update_draft_shouldUpdate() {
        UpdatePackageRequest req = new UpdatePackageRequest();
        req.setPackageName("更新名称");
        req.setDescription("新描述");

        when(packageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        when(packageRepository.findByPackageName("更新名称")).thenReturn(Optional.of(mockPkg));
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SoftwarePackageDTO dto = service.update(1L, req);
        assertEquals("更新名称", dto.getPackageName());
    }

    @Test
    void update_nonDraft_shouldThrow() {
        mockPkg.setStatus(PackageStatus.PUBLISHED);
        UpdatePackageRequest req = new UpdatePackageRequest();
        req.setPackageName("名称");
        when(packageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        assertThrows(BizException.class, () -> service.update(1L, req));
    }

    @Test
    void submit_withVersions_shouldSubmit() {
        when(packageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        when(versionRepository.countBySoftwarePackageId(1L)).thenReturn(1L);
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SoftwarePackageDTO dto = service.submit(1L, 1L);
        assertEquals(PackageStatus.PENDING, dto.getStatus());
    }

    @Test
    void submit_noVersions_shouldThrow() {
        when(packageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        when(versionRepository.countBySoftwarePackageId(1L)).thenReturn(0L);
        assertThrows(BizException.class, () -> service.submit(1L, 1L));
    }

    @Test
    void submit_nonDraft_shouldThrow() {
        mockPkg.setStatus(PackageStatus.PUBLISHED);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        assertThrows(BizException.class, () -> service.submit(1L, 1L));
    }

    @Test
    void approve_pending_shouldApprove() {
        mockPkg.setStatus(PackageStatus.PENDING);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(versionRepository.findBySoftwarePackageIdAndStatus(1L, VersionStatus.DRAFT)).thenReturn(new ArrayList<>());

        SoftwarePackageDTO dto = service.approve(1L, 1L);
        assertEquals(PackageStatus.PUBLISHED, dto.getStatus());
    }

    @Test
    void approve_nonPending_shouldThrow() {
        when(packageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        assertThrows(BizException.class, () -> service.approve(1L, 1L));
    }

    @Test
    void reject_pending_shouldReject() {
        mockPkg.setStatus(PackageStatus.PENDING);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SoftwarePackageDTO dto = service.reject(1L, 1L);
        assertEquals(PackageStatus.DRAFT, dto.getStatus());
    }

    @Test
    void delete_draft_shouldDelete() {
        when(packageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        service.delete(1L);
        verify(packageRepository).delete(mockPkg);
    }

    @Test
    void delete_nonDraft_shouldThrow() {
        mockPkg.setStatus(PackageStatus.PUBLISHED);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        assertThrows(BizException.class, () -> service.delete(1L));
    }

    @Test
    void createVersion_shouldCreate() {
        CreateVersionRequest req = new CreateVersionRequest();
        req.setVersionNo("1.0.0");
        req.setStorageBackendId(1L);
        req.setReleaseNotes("初始版本");

        when(packageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        when(versionRepository.existsBySoftwarePackageIdAndVersionNo(1L, "1.0.0")).thenReturn(false);
        when(versionRepository.save(any())).thenAnswer(inv -> { SoftwareVersion v = inv.getArgument(0); v.setId(1L); return v; });

        SoftwareVersionDTO dto = service.createVersion(1L, req, 1L);
        assertEquals("1.0.0", dto.getVersionNo());
    }

    @Test
    void createVersion_duplicateVersion_shouldThrow() {
        CreateVersionRequest req = new CreateVersionRequest();
        req.setVersionNo("1.0.0");
        req.setStorageBackendId(1L);

        when(packageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        when(versionRepository.existsBySoftwarePackageIdAndVersionNo(1L, "1.0.0")).thenReturn(true);
        assertThrows(BizException.class, () -> service.createVersion(1L, req, 1L));
    }

    @Test
    void getSoftwareTypes_shouldReturnAll() {
        assertEquals(6, service.getSoftwareTypes().size());
    }

    // ============ 状态机扩展 ============

    @Test
    void offline_published_shouldOffline() {
        mockPkg.setStatus(PackageStatus.PUBLISHED);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SoftwarePackageDTO dto = service.offline(1L, "测试下架");
        assertEquals(PackageStatus.OFFLINE, dto.getStatus());
    }

    @Test
    void offline_nonPublished_shouldThrow() {
        mockPkg.setStatus(PackageStatus.DRAFT);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        assertThrows(BizException.class, () -> service.offline(1L, "测试"));
    }

    @Test
    void republish_offline_shouldRepublish() {
        mockPkg.setStatus(PackageStatus.OFFLINE);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SoftwarePackageDTO dto = service.republish(1L);
        assertEquals(PackageStatus.PUBLISHED, dto.getStatus());
    }

    @Test
    void republish_nonOffline_shouldThrow() {
        mockPkg.setStatus(PackageStatus.DRAFT);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        assertThrows(BizException.class, () -> service.republish(1L));
    }
}
