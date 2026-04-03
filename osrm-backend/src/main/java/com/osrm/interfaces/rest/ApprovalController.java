package com.osrm.interfaces.rest;

import com.osrm.domain.approval.entity.ApprovalRecord;
import com.osrm.domain.approval.repository.ApprovalRecordRepository;
import com.osrm.common.model.ApiResponse;
import com.osrm.common.model.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {

    @Autowired
    private ApprovalRecordRepository approvalRecordRepository;

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('package:approve')")
    public ApiResponse<PageResult<ApprovalRecord>> getPending(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ApprovalRecord> result = approvalRecordRepository.findByTargetTypeAndActionOrderByCreatedAtDesc(
                "SOFTWARE_PACKAGE", "SUBMIT", pageable);
        List<ApprovalRecord> content = result.getContent();
        return ApiResponse.success(PageResult.of(content, result.getTotalElements(), result.getTotalPages(), result.getSize(), result.getNumber() + 1));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('package:read')")
    public ApiResponse<PageResult<ApprovalRecord>> getHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ApprovalRecord> result = approvalRecordRepository.findByTargetTypeOrderByCreatedAtDesc("SOFTWARE_PACKAGE", pageable);
        List<ApprovalRecord> content = result.getContent();
        return ApiResponse.success(PageResult.of(content, result.getTotalElements(), result.getTotalPages(), result.getSize(), result.getNumber() + 1));
    }
}
