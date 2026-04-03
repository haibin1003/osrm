package com.osrm.application.artifact.service;

import com.osrm.application.artifact.dto.ArtifactUploadResult;
import com.osrm.common.exception.BizException;
import com.osrm.domain.software.entity.SoftwarePackage;
import com.osrm.domain.software.entity.SoftwareType;
import com.osrm.domain.software.entity.SoftwareVersion;
import com.osrm.domain.software.repository.SoftwarePackageRepository;
import com.osrm.domain.software.repository.SoftwareVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtifactUploadServiceTest {

    @Mock private SoftwarePackageRepository packageRepository;
    @Mock private SoftwareVersionRepository versionRepository;
    @InjectMocks private ArtifactUploadService artifactUploadService;

    private SoftwarePackage mockPkg;
    private SoftwareVersion mockVersion;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(artifactUploadService, "uploadBaseDir", System.getProperty("java.io.tmpdir") + "/osrm-test-uploads");

        mockPkg = new SoftwarePackage();
        mockPkg.setId(1L);
        mockPkg.setPackageName("test-app");
        mockPkg.setSoftwareType(SoftwareType.DOCKER_IMAGE);

        mockVersion = new SoftwareVersion();
        mockVersion.setId(1L);
        mockVersion.setVersionNo("1.0.0");
        mockVersion.setSoftwarePackage(mockPkg);
    }

    @Test
    void upload_emptyFile_shouldFail() {
        MockMultipartFile file = new MockMultipartFile("file", "test.tar", "application/x-tar", new byte[0]);

        ArtifactUploadResult result = artifactUploadService.uploadArtifact(1L, 1L, file);
        assertFalse(result.isSuccess());
        assertEquals("文件不能为空", result.getMessage());
    }

    @Test
    void upload_tooLargeFile_shouldFail() {
        byte[] largeData = new byte[501 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "big.tar", "application/x-tar", largeData);

        ArtifactUploadResult result = artifactUploadService.uploadArtifact(1L, 1L, file);
        assertFalse(result.isSuccess());
    }

    @Test
    void upload_nonExistingPackage_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile("file", "test.tar", "application/x-tar", "data".getBytes());
        when(packageRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BizException.class,
                () -> artifactUploadService.uploadArtifact(999L, 1L, file));
    }

    @Test
    void upload_versionMismatch_shouldFail() {
        SoftwarePackage otherPkg = new SoftwarePackage();
        otherPkg.setId(2L);
        SoftwareVersion otherVersion = new SoftwareVersion();
        otherVersion.setId(1L);
        otherVersion.setSoftwarePackage(otherPkg);

        MockMultipartFile file = new MockMultipartFile("file", "test.tar", "application/x-tar", "data".getBytes());
        when(packageRepository.findById(1L)).thenReturn(Optional.of(mockPkg));
        when(versionRepository.findById(1L)).thenReturn(Optional.of(otherVersion));

        ArtifactUploadResult result = artifactUploadService.uploadArtifact(1L, 1L, file);
        assertFalse(result.isSuccess());
        assertEquals("版本不属于该软件包", result.getMessage());
    }

    @Test
    void getArtifactPath_nonExistingVersion_shouldThrow() {
        when(versionRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> artifactUploadService.getArtifactPath(999L));
    }

    @Test
    void getArtifactPath_noFile_shouldThrow() {
        when(versionRepository.findById(1L)).thenReturn(Optional.of(mockVersion));
        assertThrows(BizException.class, () -> artifactUploadService.getArtifactPath(1L));
    }
}

@ExtendWith(MockitoExtension.class)
class DownloadCommandGeneratorTest {

    @Test
    void generateDockerCommand_shouldContainPull() {
        String cmd = DownloadCommandGenerator.generateDockerCommand("my-app", "1.0.0");
        assertTrue(cmd.contains("docker pull"));
        assertTrue(cmd.contains("my-app:1.0.0"));
    }

    @Test
    void generateHelmCommand_shouldContainPull() {
        String cmd = DownloadCommandGenerator.generateHelmCommand("my-chart", "2.0.0");
        assertTrue(cmd.contains("helm pull"));
    }

    @Test
    void generateMavenCommand_shouldContainDependency() {
        String cmd = DownloadCommandGenerator.generateMavenCommand("my-lib", "3.0.0");
        assertTrue(cmd.contains("<dependency>"));
        assertTrue(cmd.contains("my-lib"));
        assertTrue(cmd.contains("3.0.0"));
    }

    @Test
    void generateNpmCommand_shouldContainInstall() {
        String cmd = DownloadCommandGenerator.generateNpmCommand("my-pkg", "1.2.3");
        assertEquals("npm install my-pkg@1.2.3", cmd);
    }

    @Test
    void generatePypiCommand_shouldContainInstall() {
        String cmd = DownloadCommandGenerator.generatePypiCommand("my-py", "0.1.0");
        assertEquals("pip install my-py==0.1.0", cmd);
    }

    @Test
    void generateGenericCommand_shouldContainWget() {
        String cmd = DownloadCommandGenerator.generateGenericCommand("my-file", "1.0");
        assertTrue(cmd.contains("wget"));
    }

    @Test
    void generate_byType_shouldRouteCorrectly() {
        String docker = DownloadCommandGenerator.generate(SoftwareType.DOCKER_IMAGE, "a", "1", "f.tar");
        assertTrue(docker.contains("docker pull"));

        String maven = DownloadCommandGenerator.generate(SoftwareType.MAVEN, "b", "2", "b.jar");
        assertTrue(maven.contains("<dependency>"));

        String npm = DownloadCommandGenerator.generate(SoftwareType.NPM, "c", "3", "c.tgz");
        assertTrue(npm.contains("npm install"));
    }
}
