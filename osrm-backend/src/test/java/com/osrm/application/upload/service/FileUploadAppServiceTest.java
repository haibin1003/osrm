package com.osrm.application.upload.service;

import com.osrm.application.upload.dto.FileUploadDTO;
import com.osrm.common.exception.BizException;
import com.osrm.domain.upload.entity.FileUpload;
import com.osrm.domain.upload.repository.FileUploadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileUploadAppServiceTest {

    @Mock
    private FileUploadRepository fileUploadRepository;

    @InjectMocks
    private FileUploadAppService fileUploadAppService;

    @Test
    void upload_emptyFile_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", new byte[0]);
        assertThrows(BizException.class,
                () -> fileUploadAppService.upload(file, "ARTIFACT", 1L, 1L));
    }

    @Test
    void upload_tooLargeFile_shouldThrow() {
        byte[] largeData = new byte[101 * 1024 * 1024]; // 101MB
        MockMultipartFile file = new MockMultipartFile("file", "large.bin", "application/octet-stream", largeData);
        assertThrows(BizException.class,
                () -> fileUploadAppService.upload(file, "ARTIFACT", 1L, 1L));
    }

    @Test
    void delete_nonExisting_shouldThrow() {
        when(fileUploadRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> fileUploadAppService.delete(999L));
    }

    @Test
    void findByRelated_shouldReturnList() {
        FileUpload f = new FileUpload();
        f.setId(1L);
        f.setOriginalName("test.jar");
        f.setFileSize(1024L);
        when(fileUploadRepository.findByRelatedTypeAndRelatedIdOrderByCreatedAtDesc("ARTIFACT", 1L))
                .thenReturn(List.of(f));

        List<FileUploadDTO> result = fileUploadAppService.findByRelated("ARTIFACT", 1L);
        assertEquals(1, result.size());
        assertEquals("test.jar", result.get(0).getOriginalName());
    }

    @Test
    void getFilePath_nonExisting_shouldThrow() {
        when(fileUploadRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> fileUploadAppService.getFilePath(999L));
    }
}
