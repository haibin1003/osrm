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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SoftwarePackageAppService {

    private static final Logger logger = LoggerFactory.getLogger(SoftwarePackageAppService.class);

    private final SoftwarePackageRepository packageRepository;
    private final SoftwareVersionRepository versionRepository;

    @Autowired
    public SoftwarePackageAppService(SoftwarePackageRepository packageRepository,
                                      SoftwareVersionRepository versionRepository) {
        this.packageRepository = packageRepository;
        this.versionRepository = versionRepository;
    }

    /**
     * 分页查询软件包
     */
    public PageResult<SoftwarePackageDTO> findByConditions(String keyword, String type, String status,
                                                            Long categoryId, String sort, int page, int size) {
        Pageable pageable;
        if ("viewCount".equals(sort)) {
            pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "viewCount"));
        } else if ("downloadCount".equals(sort)) {
            pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "downloadCount"));
        } else {
            pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        SoftwareType typeEnum = type != null && !type.isEmpty() ? SoftwareType.valueOf(type) : null;
        PackageStatus statusEnum = status != null && !status.isEmpty() ? PackageStatus.valueOf(status) : null;

        Page<SoftwarePackage> result = packageRepository.findByConditions(keyword, typeEnum, statusEnum, categoryId, pageable);
        List<SoftwarePackageDTO> content = result.getContent().stream()
                .map(SoftwarePackageDTO::from)
                .collect(Collectors.toList());
        return PageResult.of(content, result.getTotalElements(), result.getTotalPages(), result.getSize(), result.getNumber() + 1);
    }

    /**
     * 获取软件包详情
     */
    public SoftwarePackageDTO findById(Long id) {
        SoftwarePackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new BizException("软件包不存在"));
        return SoftwarePackageDTO.from(pkg);
    }

    /**
     * 根据用户ID查询软件包列表
     */
    public PageResult<SoftwarePackageDTO> findByUserId(Long userId, String keyword, String type,
                                                        String status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        SoftwareType typeEnum = type != null && !type.isEmpty() ? SoftwareType.valueOf(type) : null;
        PackageStatus statusEnum = status != null && !status.isEmpty() ? PackageStatus.valueOf(status) : null;

        Page<SoftwarePackage> result = packageRepository.findByCreatedBy(userId, keyword, typeEnum, statusEnum, pageable);
        List<SoftwarePackageDTO> content = result.getContent().stream()
                .map(SoftwarePackageDTO::from)
                .collect(Collectors.toList());
        return PageResult.of(content, result.getTotalElements(), result.getTotalPages(), result.getSize(), result.getNumber() + 1);
    }

    /**
     * 根据 packageKey 查询
     */
    public SoftwarePackageDTO findByPackageKey(String packageKey) {
        SoftwarePackage pkg = packageRepository.findByPackageKey(packageKey)
                .orElseThrow(() -> new BizException("软件包不存在"));
        return SoftwarePackageDTO.from(pkg);
    }

    /**
     * 创建软件包
     */
    @Transactional
    public SoftwarePackageDTO create(CreatePackageRequest request, Long createdBy) {
        // 检查名称唯一性
        if (packageRepository.existsByPackageName(request.getPackageName())) {
            throw new BizException("软件包名称已存在");
        }
        // 检查 key 唯一性
        if (packageRepository.existsByPackageKey(request.getPackageKey())) {
            throw new BizException("软件包标识已存在");
        }

        SoftwarePackage pkg = new SoftwarePackage();
        pkg.setPackageName(request.getPackageName());
        pkg.setPackageKey(request.getPackageKey());
        pkg.setSoftwareType(request.getSoftwareType());
        // 如果没有提供 categoryId，使用默认分类（开发工具）
        pkg.setCategoryId(request.getCategoryId() != null ? request.getCategoryId() : 1L);
        pkg.setDescription(request.getDescription());
        pkg.setWebsiteUrl(request.getWebsiteUrl());
        pkg.setLicenseType(request.getLicenseType());
        pkg.setLicenseUrl(request.getLicenseUrl());
        pkg.setSourceUrl(request.getSourceUrl());
        pkg.setCreatedBy(createdBy);
        pkg.setStatus(PackageStatus.DRAFT);

        SoftwarePackage saved = packageRepository.save(pkg);
        logger.info("创建软件包成功: id={}, name={}, key={}", saved.getId(), saved.getPackageName(), saved.getPackageKey());
        return SoftwarePackageDTO.from(saved);
    }

    /**
     * 更新软件包
     */
    @Transactional
    public SoftwarePackageDTO update(Long id, UpdatePackageRequest request) {
        SoftwarePackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new BizException("软件包不存在"));

        // 检查状态是否允许编辑
        if (!pkg.getStatus().canEdit()) {
            throw new BizException("当前状态不能编辑软件包");
        }

        // 检查名称唯一性（如果名称改变）
        if (!pkg.getPackageName().equals(request.getPackageName())) {
            packageRepository.findByPackageName(request.getPackageName())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(id)) {
                            throw new BizException("软件包名称已存在");
                        }
                    });
        }

        pkg.setPackageName(request.getPackageName());
        pkg.setCategoryId(request.getCategoryId());
        pkg.setDescription(request.getDescription());
        pkg.setWebsiteUrl(request.getWebsiteUrl());
        pkg.setLicenseType(request.getLicenseType());
        pkg.setLicenseUrl(request.getLicenseUrl());
        pkg.setSourceUrl(request.getSourceUrl());

        SoftwarePackage saved = packageRepository.save(pkg);
        logger.info("更新软件包成功: id={}", saved.getId());
        return SoftwarePackageDTO.from(saved);
    }

    /**
     * 删除软件包
     */
    @Transactional
    public void delete(Long id) {
        SoftwarePackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new BizException("软件包不存在"));

        if (!pkg.getStatus().canDelete()) {
            throw new BizException("只有草稿状态的软件包才能删除");
        }

        packageRepository.delete(pkg);
        logger.info("删除软件包成功: id={}", id);
    }

    // ============ 状态流转 ============

    /**
     * 提交审核
     */
    @Transactional
    public SoftwarePackageDTO submit(Long id, Long operatorId) {
        SoftwarePackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new BizException("软件包不存在"));

        // 检查是否有版本
        long versionCount = versionRepository.countBySoftwarePackageId(id);
        if (versionCount == 0) {
            throw new BizException("请先添加至少一个版本");
        }

        try {
            pkg.submit();
        } catch (IllegalStateException e) {
            throw new BizException(e.getMessage());
        }
        SoftwarePackage saved = packageRepository.save(pkg);
        logger.info("软件包提交审核: id={}", id);
        return SoftwarePackageDTO.from(saved);
    }

    /**
     * 审批通过
     */
    @Transactional
    public SoftwarePackageDTO approve(Long id, Long approverId) {
        SoftwarePackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new BizException("软件包不存在"));

        try {
            pkg.approve(approverId);
        } catch (IllegalStateException e) {
            throw new BizException(e.getMessage());
        }

        // 发布第一个版本
        publishFirstVersion(id, approverId);

        SoftwarePackage saved = packageRepository.save(pkg);
        logger.info("软件包审批通过: id={}", id);
        return SoftwarePackageDTO.from(saved);
    }

    /**
     * 审批驳回
     */
    @Transactional
    public SoftwarePackageDTO reject(Long id, Long operatorId) {
        SoftwarePackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new BizException("软件包不存在"));

        try {
            pkg.reject();
        } catch (IllegalStateException e) {
            throw new BizException(e.getMessage());
        }
        SoftwarePackage saved = packageRepository.save(pkg);
        logger.info("软件包审批驳回: id={}", id);
        return SoftwarePackageDTO.from(saved);
    }

    /**
     * 下架
     */
    @Transactional
    public SoftwarePackageDTO offline(Long id, String reason) {
        SoftwarePackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new BizException("软件包不存在"));

        try {
            pkg.offline();
        } catch (IllegalStateException e) {
            throw new BizException(e.getMessage());
        }
        SoftwarePackage saved = packageRepository.save(pkg);
        logger.info("软件包下架: id={}, reason={}", id, reason);
        return SoftwarePackageDTO.from(saved);
    }

    /**
     * 重新上架
     */
    @Transactional
    public SoftwarePackageDTO republish(Long id) {
        SoftwarePackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new BizException("软件包不存在"));

        try {
            pkg.republish();
        } catch (IllegalStateException e) {
            throw new BizException(e.getMessage());
        }
        SoftwarePackage saved = packageRepository.save(pkg);
        logger.info("软件包重新上架: id={}", id);
        return SoftwarePackageDTO.from(saved);
    }

    // ============ 版本管理 ============

    /**
     * 获取版本列表
     */
    public List<SoftwareVersionDTO> getVersions(Long packageId) {
        return versionRepository.findBySoftwarePackageIdOrderByCreatedAtDesc(packageId)
                .stream()
                .map(SoftwareVersionDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 创建版本
     */
    @Transactional
    public SoftwareVersionDTO createVersion(Long packageId, CreateVersionRequest request, Long createdBy) {
        SoftwarePackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new BizException("软件包不存在"));

        // 检查版本号是否已存在
        if (versionRepository.existsBySoftwarePackageIdAndVersionNo(packageId, request.getVersionNo())) {
            throw new BizException("版本号已存在");
        }

        SoftwareVersion ver = new SoftwareVersion();
        ver.setSoftwarePackage(pkg);
        ver.setVersionNo(request.getVersionNo());
        ver.setStorageBackendId(request.getStorageBackendId());
        ver.setStoragePath(request.getStoragePath());
        ver.setArtifactUrl(request.getArtifactUrl());
        ver.setReleaseNotes(request.getReleaseNotes());
        ver.setFileSize(request.getFileSize());
        ver.setChecksum(request.getChecksum());
        ver.setCreatedBy(createdBy);
        ver.setStatus(VersionStatus.DRAFT);

        SoftwareVersion saved = versionRepository.save(ver);
        logger.info("创建版本成功: packageId={}, version={}", packageId, request.getVersionNo());
        return SoftwareVersionDTO.from(saved);
    }

    /**
     * 发布版本
     */
    @Transactional
    public SoftwareVersionDTO publishVersion(Long packageId, Long versionId, Long publisherId) {
        SoftwarePackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new BizException("软件包不存在"));

        if (pkg.getStatus() != PackageStatus.PUBLISHED) {
            throw new BizException("软件包未发布，不能发布版本");
        }

        SoftwareVersion ver = versionRepository.findById(versionId)
                .orElseThrow(() -> new BizException("版本不存在"));

        if (!ver.getSoftwarePackage().getId().equals(packageId)) {
            throw new BizException("版本不属于该软件包");
        }

        ver.publish(publisherId);

        // 清除其他版本的最新标记
        versionRepository.clearLatestFlagByPackageId(packageId);
        ver.setIsLatest(true);

        // 更新软件包的当前版本
        pkg.setCurrentVersion(ver.getVersionNo());
        packageRepository.save(pkg);

        SoftwareVersion saved = versionRepository.save(ver);
        logger.info("发布版本成功: packageId={}, versionId={}", packageId, versionId);
        return SoftwareVersionDTO.from(saved);
    }

    /**
     * 下线版本
     */
    @Transactional
    public SoftwareVersionDTO offlineVersion(Long packageId, Long versionId) {
        SoftwareVersion ver = versionRepository.findById(versionId)
                .orElseThrow(() -> new BizException("版本不存在"));

        if (!ver.getSoftwarePackage().getId().equals(packageId)) {
            throw new BizException("版本不属于该软件包");
        }

        ver.offline();

        // 如果是最新版本，清除标记
        if (Boolean.TRUE.equals(ver.getIsLatest())) {
            ver.setIsLatest(false);
            // 更新软件包当前版本
            SoftwarePackage pkg = ver.getSoftwarePackage();
            pkg.setCurrentVersion(null);
            packageRepository.save(pkg);
        }

        SoftwareVersion saved = versionRepository.save(ver);
        logger.info("下线版本成功: packageId={}, versionId={}", packageId, versionId);
        return SoftwareVersionDTO.from(saved);
    }

    /**
     * 删除版本
     */
    @Transactional
    public void deleteVersion(Long packageId, Long versionId) {
        SoftwareVersion ver = versionRepository.findById(versionId)
                .orElseThrow(() -> new BizException("版本不存在"));

        if (!ver.getSoftwarePackage().getId().equals(packageId)) {
            throw new BizException("版本不属于该软件包");
        }

        if (ver.getStatus() != VersionStatus.DRAFT) {
            throw new BizException("只有草稿状态的版本才能删除");
        }

        versionRepository.delete(ver);
        logger.info("删除版本成功: packageId={}, versionId={}", packageId, versionId);
    }

    // ============ 辅助方法 ============

    private void publishFirstVersion(Long packageId, Long publisherId) {
        // 查找第一个草稿版本并发布
        List<SoftwareVersion> draftVersions = versionRepository.findBySoftwarePackageIdAndStatus(packageId, VersionStatus.DRAFT);
        if (!draftVersions.isEmpty()) {
            SoftwareVersion firstVersion = draftVersions.get(0);
            firstVersion.publish(publisherId);
            firstVersion.setIsLatest(true);
            versionRepository.save(firstVersion);

            // 更新软件包当前版本
            SoftwarePackage pkg = packageRepository.findById(packageId).orElseThrow();
            pkg.setCurrentVersion(firstVersion.getVersionNo());
            packageRepository.save(pkg);
        }
    }

    /**
     * 获取软件类型列表
     */
    public List<Map<String, String>> getSoftwareTypes() {
        return Arrays.stream(SoftwareType.values())
                .map(type -> Map.of(
                        "code", type.name(),
                        "name", type.getName(),
                        "storageType", type.getStorageType()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 获取待审批的软件包列表
     */
    public PageResult<SoftwarePackageDTO> findPendingPackages(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SoftwarePackage> result = packageRepository.findByStatus(PackageStatus.PENDING, pageable);
        List<SoftwarePackageDTO> content = result.getContent().stream()
                .map(SoftwarePackageDTO::from)
                .collect(Collectors.toList());
        return PageResult.of(content, result.getTotalElements(), result.getTotalPages(), result.getSize(), result.getNumber() + 1);
    }
}
