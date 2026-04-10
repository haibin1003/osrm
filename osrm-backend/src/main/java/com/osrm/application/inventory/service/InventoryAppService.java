package com.osrm.application.inventory.service;

import com.osrm.application.inventory.dto.request.CreateInventoryRequest;
import com.osrm.application.inventory.dto.request.RejectInventoryRequest;
import com.osrm.application.inventory.dto.response.InventoryDTO;
import com.osrm.common.exception.BizException;
import com.osrm.common.model.PageResult;
import com.osrm.domain.inventory.entity.InventoryRecord;
import com.osrm.domain.inventory.repository.InventoryRecordRepository;
import com.osrm.domain.system.repository.SystemSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class InventoryAppService {

    private final InventoryRecordRepository inventoryRecordRepository;
    private final SystemSettingRepository systemSettingRepository;

    @Autowired
    public InventoryAppService(InventoryRecordRepository inventoryRecordRepository,
                               SystemSettingRepository systemSettingRepository) {
        this.inventoryRecordRepository = inventoryRecordRepository;
        this.systemSettingRepository = systemSettingRepository;
    }

    /**
     * 检查存量登记功能是否启用
     */
    public boolean isInventoryFeatureEnabled() {
        return systemSettingRepository.findByCategoryAndSettingKey("INVENTORY", "ENABLE_INVENTORY_FEATURE")
                .map(setting -> "true".equalsIgnoreCase(setting.getSettingValue()))
                .orElse(false);
    }

    /**
     * 获取我的存量登记列表（数据权限：只能查看自己的）
     */
    public PageResult<InventoryDTO> getMyInventory(Long userId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<InventoryRecord> pageResult;
        if (status != null && !status.isEmpty()) {
            InventoryRecord.InventoryStatus statusEnum = InventoryRecord.InventoryStatus.valueOf(status);
            pageResult = inventoryRecordRepository.findByUserIdAndStatus(userId, statusEnum, pageable);
        } else {
            pageResult = inventoryRecordRepository.findByUserId(userId, pageable);
        }

        List<InventoryDTO> content = pageResult.getContent().stream()
                .map(InventoryDTO::from)
                .collect(Collectors.toList());

        return PageResult.of(content, pageResult.getTotalElements(), pageResult.getTotalPages(),
                pageResult.getSize(), pageResult.getNumber() + 1);
    }

    /**
     * 获取待审批列表（管理员使用）
     */
    public PageResult<InventoryDTO> getPendingInventory(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InventoryRecord> pageResult = inventoryRecordRepository.findByStatus(
                InventoryRecord.InventoryStatus.PENDING, pageable);

        List<InventoryDTO> content = pageResult.getContent().stream()
                .map(InventoryDTO::from)
                .collect(Collectors.toList());

        return PageResult.of(content, pageResult.getTotalElements(), pageResult.getTotalPages(),
                pageResult.getSize(), pageResult.getNumber() + 1);
    }

    /**
     * 获取所有存量列表（管理员使用）
     */
    public PageResult<InventoryDTO> getAllInventory(Long userId, String status, String packageName,
                                                     Long businessSystemId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        InventoryRecord.InventoryStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            statusEnum = InventoryRecord.InventoryStatus.valueOf(status);
        }

        Page<InventoryRecord> pageResult = inventoryRecordRepository.findByConditions(
                userId, statusEnum, packageName, businessSystemId, pageable);

        List<InventoryDTO> content = pageResult.getContent().stream()
                .map(InventoryDTO::from)
                .collect(Collectors.toList());

        return PageResult.of(content, pageResult.getTotalElements(), pageResult.getTotalPages(),
                pageResult.getSize(), pageResult.getNumber() + 1);
    }

    /**
     * 获取存量登记详情
     */
    public InventoryDTO getInventoryById(Long id, Long currentUserId, boolean isAdmin) {
        InventoryRecord record = inventoryRecordRepository.findById(id)
                .orElseThrow(() -> new BizException("存量登记记录不存在"));

        // 数据权限检查：非管理员只能查看自己的记录
        if (!isAdmin && !record.getUserId().equals(currentUserId)) {
            throw new BizException("无权查看此记录");
        }

        return InventoryDTO.from(record);
    }

    /**
     * 创建存量登记
     */
    @Transactional
    public InventoryDTO createInventory(CreateInventoryRequest request, Long userId, String userName) {
        if (!isInventoryFeatureEnabled()) {
            throw new BizException("存量登记功能已关闭");
        }

        InventoryRecord record = new InventoryRecord();
        record.setRecordNo(generateRecordNo());
        record.setUserId(userId);
        record.setPackageId(request.getPackageId());
        record.setPackageName(request.getPackageName());
        record.setVersionNo(request.getVersionNo());
        record.setSoftwareType(request.getSoftwareType());
        // 负责人默认是登记人自己
        record.setResponsiblePerson(request.getResponsiblePerson() != null ?
                request.getResponsiblePerson() : userName);
        record.setBusinessSystemId(request.getBusinessSystemId());
        record.setDeployEnvironment(request.getDeployEnvironment());
        record.setServerCount(request.getServerCount() != null ? request.getServerCount() : 1);
        record.setUsageScenario(request.getUsageScenario());
        record.setRemarks(request.getRemarks());
        record.setSourceType(InventoryRecord.SourceType.MANUAL);
        record.setStatus(InventoryRecord.InventoryStatus.PENDING);

        InventoryRecord saved = inventoryRecordRepository.save(record);
        return InventoryDTO.from(saved);
    }

    /**
     * 更新存量登记
     */
    @Transactional
    public InventoryDTO updateInventory(Long id, CreateInventoryRequest request, Long userId, boolean isAdmin) {
        InventoryRecord record = inventoryRecordRepository.findById(id)
                .orElseThrow(() -> new BizException("存量登记记录不存在"));

        // 数据权限检查：非管理员只能修改自己的记录
        if (!isAdmin && !record.getUserId().equals(userId)) {
            throw new BizException("无权修改此记录");
        }

        // 只有待审批状态可以修改
        if (record.getStatus() != InventoryRecord.InventoryStatus.PENDING) {
            throw new BizException("只有待审批状态的记录可以修改");
        }

        record.setPackageId(request.getPackageId());
        record.setPackageName(request.getPackageName());
        record.setVersionNo(request.getVersionNo());
        record.setSoftwareType(request.getSoftwareType());
        if (request.getResponsiblePerson() != null) {
            record.setResponsiblePerson(request.getResponsiblePerson());
        }
        record.setBusinessSystemId(request.getBusinessSystemId());
        record.setDeployEnvironment(request.getDeployEnvironment());
        record.setServerCount(request.getServerCount());
        record.setUsageScenario(request.getUsageScenario());
        record.setRemarks(request.getRemarks());

        InventoryRecord saved = inventoryRecordRepository.save(record);
        return InventoryDTO.from(saved);
    }

    /**
     * 批准存量登记
     */
    @Transactional
    public InventoryDTO approveInventory(Long id, Long approverId) {
        InventoryRecord record = inventoryRecordRepository.findById(id)
                .orElseThrow(() -> new BizException("存量登记记录不存在"));

        if (record.getStatus() != InventoryRecord.InventoryStatus.PENDING) {
            throw new BizException("只有待审批状态的记录可以批准");
        }

        record.approve(approverId);
        InventoryRecord saved = inventoryRecordRepository.save(record);
        return InventoryDTO.from(saved);
    }

    /**
     * 驳回存量登记
     */
    @Transactional
    public InventoryDTO rejectInventory(Long id, RejectInventoryRequest request, Long approverId) {
        InventoryRecord record = inventoryRecordRepository.findById(id)
                .orElseThrow(() -> new BizException("存量登记记录不存在"));

        if (record.getStatus() != InventoryRecord.InventoryStatus.PENDING) {
            throw new BizException("只有待审批状态的记录可以驳回");
        }

        record.reject(approverId, request.getReason());
        InventoryRecord saved = inventoryRecordRepository.save(record);
        return InventoryDTO.from(saved);
    }

    /**
     * 删除存量登记
     */
    @Transactional
    public void deleteInventory(Long id, Long userId, boolean isAdmin) {
        InventoryRecord record = inventoryRecordRepository.findById(id)
                .orElseThrow(() -> new BizException("存量登记记录不存在"));

        // 数据权限检查：非管理员只能删除自己的记录
        if (!isAdmin && !record.getUserId().equals(userId)) {
            throw new BizException("无权删除此记录");
        }

        inventoryRecordRepository.delete(record);
    }

    /**
     * 生成登记编号
     */
    private String generateRecordNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "INV-" + date + "-" + uuid;
    }
}
