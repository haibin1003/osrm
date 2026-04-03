package com.osrm.domain.upload.repository;

import com.osrm.domain.upload.entity.FileUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {

    List<FileUpload> findByRelatedTypeAndRelatedIdOrderByCreatedAtDesc(String relatedType, Long relatedId);

    List<FileUpload> findByRelatedIdOrderByCreatedAtDesc(Long relatedId);
}
