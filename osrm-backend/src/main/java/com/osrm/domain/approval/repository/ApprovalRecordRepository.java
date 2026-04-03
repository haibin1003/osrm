package com.osrm.domain.approval.repository;

import com.osrm.domain.approval.entity.ApprovalRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRecordRepository extends JpaRepository<ApprovalRecord, Long> {

    Page<ApprovalRecord> findByTargetTypeAndActionOrderByCreatedAtDesc(String targetType, String action, Pageable pageable);

    Page<ApprovalRecord> findByTargetTypeOrderByCreatedAtDesc(String targetType, Pageable pageable);

    List<ApprovalRecord> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);
}
