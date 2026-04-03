package com.osrm.application.subscription.service;

import com.osrm.application.subscription.dto.request.CreateSubscriptionRequest;
import com.osrm.application.subscription.dto.response.DownloadTokenDTO;
import com.osrm.application.subscription.dto.response.SubscriptionDTO;
import com.osrm.common.exception.BizException;
import com.osrm.common.model.PageResult;
import com.osrm.domain.business.entity.BusinessSystem;
import com.osrm.domain.business.repository.BusinessSystemRepository;
import com.osrm.domain.software.entity.SoftwarePackage;
import com.osrm.domain.software.entity.SoftwareVersion;
import com.osrm.domain.software.repository.SoftwarePackageRepository;
import com.osrm.domain.software.repository.SoftwareVersionRepository;
import com.osrm.domain.subscription.entity.DownloadToken;
import com.osrm.domain.subscription.entity.Subscription;
import com.osrm.domain.subscription.entity.SubscriptionStatus;
import com.osrm.domain.subscription.repository.DownloadTokenRepository;
import com.osrm.domain.subscription.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SubscriptionAppService {

    private final SubscriptionRepository subscriptionRepository;
    private final DownloadTokenRepository downloadTokenRepository;
    private final SoftwarePackageRepository softwarePackageRepository;
    private final SoftwareVersionRepository softwareVersionRepository;
    private final BusinessSystemRepository businessSystemRepository;

    @Autowired
    public SubscriptionAppService(SubscriptionRepository subscriptionRepository,
                                  DownloadTokenRepository downloadTokenRepository,
                                  SoftwarePackageRepository softwarePackageRepository,
                                  SoftwareVersionRepository softwareVersionRepository,
                                  BusinessSystemRepository businessSystemRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.downloadTokenRepository = downloadTokenRepository;
        this.softwarePackageRepository = softwarePackageRepository;
        this.softwareVersionRepository = softwareVersionRepository;
        this.businessSystemRepository = businessSystemRepository;
    }

    public PageResult<SubscriptionDTO> findByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Subscription> result = subscriptionRepository.findByUserId(userId, pageable);
        return toPageResult(result, page, size);
    }

    public PageResult<SubscriptionDTO> findByStatus(SubscriptionStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Subscription> result = subscriptionRepository.findByStatus(status, pageable);
        return toPageResult(result, page, size);
    }

    @Transactional
    public SubscriptionDTO create(CreateSubscriptionRequest request, Long userId) {
        SoftwarePackage pkg = softwarePackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new BizException("软件包不存在"));

        BusinessSystem system = businessSystemRepository.findById(request.getBusinessSystemId())
                .orElseThrow(() -> new BizException("业务系统不存在"));

        if (subscriptionRepository.existsByPackageIdAndBusinessSystemIdAndStatus(
                request.getPackageId(), request.getBusinessSystemId(), SubscriptionStatus.APPROVED)) {
            throw new BizException("该业务系统已订阅此软件包");
        }

        Subscription sub = new Subscription();
        // 生成订阅编号
        sub.setSubscriptionNo(generateSubscriptionNo());
        sub.setUserId(userId);
        sub.setPackageId(pkg.getId());
        sub.setBusinessSystemId(system.getId());
        sub.setUsageScenario(request.getUseScene());
        sub.setStatus(SubscriptionStatus.PENDING);
        sub.setMaxRenewalCount(3);
        sub.setCurrentRenewalCount(0);

        if (request.getVersionId() != null) {
            SoftwareVersion ver = softwareVersionRepository.findById(request.getVersionId())
                    .orElseThrow(() -> new BizException("版本不存在"));
            sub.setVersionId(ver.getId());
        } else {
            // 如果没有指定版本，使用最新版本
            SoftwareVersion latestVer = softwareVersionRepository.findLatestVersionByPackageId(pkg.getId())
                    .orElseThrow(() -> new BizException("该软件包没有可用版本"));
            sub.setVersionId(latestVer.getId());
        }

        Subscription saved = subscriptionRepository.save(sub);
        return populateDtoRelations(SubscriptionDTO.from(saved));
    }

    @Transactional
    public SubscriptionDTO approve(Long id, Long operatorId) {
        Subscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new BizException("订阅记录不存在"));
        if (sub.getStatus() != SubscriptionStatus.PENDING) {
            throw new BizException("只有待审批状态的订阅才能批准");
        }
        sub.setStatus(SubscriptionStatus.APPROVED);
        sub.setApprovedBy(operatorId);
        sub.setApprovedAt(LocalDateTime.now());
        sub.setStartDate(LocalDate.now());
        sub.setEndDate(LocalDate.now().plusYears(1));
        subscriptionRepository.save(sub);

        generateToken(sub);
        return populateDtoRelations(SubscriptionDTO.from(sub));
    }

    @Transactional
    public SubscriptionDTO reject(Long id, Long operatorId, String reason) {
        Subscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new BizException("订阅记录不存在"));
        if (sub.getStatus() != SubscriptionStatus.PENDING) {
            throw new BizException("只有待审批状态的订阅才能驳回");
        }
        sub.setStatus(SubscriptionStatus.REJECTED);
        sub.setApprovedBy(operatorId);
        sub.setApprovedAt(LocalDateTime.now());
        sub.setRejectReason(reason);
        return populateDtoRelations(SubscriptionDTO.from(subscriptionRepository.save(sub)));
    }

    @Transactional
    public void revokeToken(Long subscriptionId) {
        DownloadToken token = downloadTokenRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new BizException("下载令牌不存在"));
        token.setEnabled(false);
        downloadTokenRepository.save(token);
    }

    public DownloadTokenDTO getToken(Long subscriptionId) {
        DownloadToken token = downloadTokenRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new BizException("下载令牌不存在"));
        return DownloadTokenDTO.from(token);
    }

    private void generateToken(Subscription sub) {
        downloadTokenRepository.findBySubscriptionId(sub.getId()).ifPresent(t -> {
            t.setEnabled(false);
            downloadTokenRepository.save(t);
        });

        DownloadToken token = new DownloadToken();
        token.setSubscriptionId(sub.getId());
        token.setToken(UUID.randomUUID().toString().replace("-", ""));
        token.setExpireAt(LocalDateTime.now().plusDays(30));
        token.setMaxDownloads(10);
        token.setUsedCount(0);
        token.setEnabled(true);
        downloadTokenRepository.save(token);
    }

    private PageResult<SubscriptionDTO> toPageResult(Page<Subscription> page, int page1, int size) {
        return PageResult.of(
                page.getContent().stream()
                        .map(SubscriptionDTO::from)
                        .map(this::populateDtoRelations)
                        .toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getSize(),
                page.getNumber() + 1
        );
    }

    private String generateSubscriptionNo() {
        return "SUB-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" +
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private SubscriptionDTO populateDtoRelations(SubscriptionDTO dto) {
        if (dto.getPackageId() != null) {
            softwarePackageRepository.findById(dto.getPackageId())
                    .ifPresent(pkg -> dto.setPackageName(pkg.getPackageName()));
        }
        if (dto.getBusinessSystemId() != null) {
            businessSystemRepository.findById(dto.getBusinessSystemId())
                    .ifPresent(sys -> dto.setSystemName(sys.getSystemName()));
        }
        if (dto.getVersionId() != null) {
            softwareVersionRepository.findById(dto.getVersionId())
                    .ifPresent(ver -> dto.setVersionNumber(ver.getVersionNo()));
        }
        return dto;
    }
}
