package com.osrm.application.upload.service;

import com.osrm.application.upload.dto.FileUploadDTO;
import com.osrm.common.exception.BizException;
import com.osrm.domain.upload.entity.FileUpload;
import com.osrm.domain.upload.repository.FileUploadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FileUploadAppService {

    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB

    private final FileUploadRepository fileUploadRepository;

    @Value("${osrm.upload.dir:./uploads}")
    private String uploadDir;

    @Autowired
    public FileUploadAppService(FileUploadRepository fileUploadRepository) {
        this.fileUploadRepository = fileUploadRepository;
    }

    @Transactional
    public FileUploadDTO upload(MultipartFile file, String relatedType, Long relatedId, Long uploadedBy) {
        if (file.isEmpty()) {
            throw new BizException("文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException("文件大小不能超过100MB");
        }

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + extension;

        try {
            Path dir = Paths.get(uploadDir, relatedType != null ? relatedType.toLowerCase() : "common");
            Files.createDirectories(dir);
            Path filePath = dir.resolve(storedName);
            file.transferTo(filePath.toFile());

            String md5 = calculateMD5(filePath);

            FileUpload record = new FileUpload();
            record.setOriginalName(originalName);
            record.setStoredName(storedName);
            record.setFilePath(filePath.toString());
            record.setFileSize(file.getSize());
            record.setContentType(file.getContentType());
            record.setMd5Hash(md5);
            record.setRelatedType(relatedType);
            record.setRelatedId(relatedId);
            record.setUploadedBy(uploadedBy);

            FileUpload saved = fileUploadRepository.save(record);
            return FileUploadDTO.from(saved);
        } catch (IOException e) {
            throw new BizException("文件上传失败: " + e.getMessage());
        }
    }

    public List<FileUploadDTO> findByRelated(String relatedType, Long relatedId) {
        return fileUploadRepository.findByRelatedTypeAndRelatedIdOrderByCreatedAtDesc(relatedType, relatedId)
                .stream().map(FileUploadDTO::from).collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        FileUpload file = fileUploadRepository.findById(id)
                .orElseThrow(() -> new BizException("文件不存在"));
        try {
            Files.deleteIfExists(Paths.get(file.getFilePath()));
        } catch (IOException e) {
            // 忽略删除物理文件失败
        }
        fileUploadRepository.delete(file);
    }

    public Path getFilePath(Long id) {
        FileUpload file = fileUploadRepository.findById(id)
                .orElseThrow(() -> new BizException("文件不存在"));
        return Paths.get(file.getFilePath());
    }

    private String calculateMD5(Path filePath) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] digest = md.digest(fileBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "unknown";
        }
    }
}
