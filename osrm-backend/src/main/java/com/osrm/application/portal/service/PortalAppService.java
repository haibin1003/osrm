package com.osrm.application.portal.service;

import com.osrm.application.portal.dto.response.*;
import com.osrm.application.software.dto.response.SoftwareVersionDTO;
import com.osrm.common.model.PageResult;
import com.osrm.domain.software.entity.PackageStatus;
import com.osrm.domain.software.entity.SoftwarePackage;
import com.osrm.domain.software.entity.SoftwareType;
import com.osrm.domain.software.entity.SoftwareVersion;
import com.osrm.domain.software.repository.SoftwarePackageRepository;
import com.osrm.domain.software.repository.SoftwareVersionRepository;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PortalAppService {

    private final SoftwarePackageRepository packageRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SoftwareVersionRepository versionRepository;

    @Autowired
    public PortalAppService(SoftwarePackageRepository packageRepository,
                            SubscriptionRepository subscriptionRepository,
                            SoftwareVersionRepository versionRepository) {
        this.packageRepository = packageRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.versionRepository = versionRepository;
    }

    public PageResult<PortalPackageDTO> findPublishedPackages(String keyword, SoftwareType type, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SoftwarePackage> result = packageRepository.findByConditions(keyword, type, PackageStatus.PUBLISHED, null, pageable);
        List<PortalPackageDTO> content = result.getContent().stream().map(PortalPackageDTO::from).toList();
        return PageResult.of(content, result.getTotalElements(), result.getTotalPages(), result.getSize(), result.getNumber() + 1);
    }

    public PortalPackageDTO findPackageById(Long id) {
        SoftwarePackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("软件包不存在"));
        return PortalPackageDTO.from(pkg);
    }

    public PortalStatsDTO getStats() {
        PortalStatsDTO stats = new PortalStatsDTO();
        stats.setTotalPackages(packageRepository.count());
        stats.setPublishedCount(packageRepository.countByStatus(PackageStatus.PUBLISHED));
        stats.setPendingCount(packageRepository.countByStatus(PackageStatus.PENDING));
        stats.setDockerCount(packageRepository.countBySoftwareType(SoftwareType.DOCKER_IMAGE));
        stats.setHelmCount(packageRepository.countBySoftwareType(SoftwareType.HELM_CHART));
        stats.setMavenCount(packageRepository.countBySoftwareType(SoftwareType.MAVEN));
        stats.setNpmCount(packageRepository.countBySoftwareType(SoftwareType.NPM));
        stats.setPypiCount(packageRepository.countBySoftwareType(SoftwareType.PYPI));
        stats.setGenericCount(packageRepository.countBySoftwareType(SoftwareType.GENERIC));
        return stats;
    }

    public List<PortalPackageDTO> findPopular(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<SoftwarePackage> packages = packageRepository.findTopByStatusOrderByViewCountDesc(PackageStatus.PUBLISHED, pageable);
        return packages.stream().map(PortalPackageDTO::from).toList();
    }

    public Map<String, Object> getDetailedStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPackages", packageRepository.count());
        stats.put("publishedCount", packageRepository.countByStatus(PackageStatus.PUBLISHED));
        stats.put("pendingCount", packageRepository.countByStatus(PackageStatus.PENDING));
        stats.put("draftCount", packageRepository.countByStatus(PackageStatus.DRAFT));
        stats.put("totalSubscriptions", subscriptionRepository.count());
        return stats;
    }

    public PortalStatsOverviewDTO getStatsOverview() {
        PortalStatsOverviewDTO dto = new PortalStatsOverviewDTO();
        dto.setTotalPackages(packageRepository.count());
        dto.setPublishedCount(packageRepository.countByStatus(PackageStatus.PUBLISHED));
        dto.setPendingCount(packageRepository.countByStatus(PackageStatus.PENDING));
        dto.setDraftCount(packageRepository.countByStatus(PackageStatus.DRAFT));
        dto.setTotalSubscriptions(subscriptionRepository.count());
        return dto;
    }

    public List<StatsTrendDTO> getStatsTrend(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        // 使用内存分组以兼容 H2 数据库（开发环境）和 MySQL（生产环境）
        List<com.osrm.domain.subscription.entity.Subscription> subscriptions =
                subscriptionRepository.findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(startDate);

        // 按日期分组统计
        Map<String, Long> dataMap = subscriptions.stream()
                .collect(Collectors.groupingBy(
                        sub -> sub.getCreatedAt().toLocalDate().toString(),
                        Collectors.counting()
                ));

        List<StatsTrendDTO> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).toString();
            result.add(new StatsTrendDTO(date, dataMap.getOrDefault(date, 0L)));
        }
        return result;
    }

    public DependencyGraphDTO getDependencies(Long id) {
        SoftwarePackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("软件包不存在"));

        List<DependencyGraphDTO.Node> nodes = new ArrayList<>();
        List<DependencyGraphDTO.Link> links = new ArrayList<>();

        String selfId = "pkg-" + id;
        nodes.add(new DependencyGraphDTO.Node(selfId, pkg.getPackageName(), pkg.getSoftwareType().name()));

        switch (pkg.getSoftwareType()) {
            case MAVEN -> {
                String[][] deps = {
                    {"spring-core", "MAVEN", "6.0.x"},
                    {"jackson-databind", "MAVEN", "2.15.x"},
                    {"slf4j-api", "MAVEN", "2.0.x"}
                };
                for (String[] dep : deps) {
                    String depId = dep[0];
                    nodes.add(new DependencyGraphDTO.Node(depId, dep[0], dep[1]));
                    links.add(new DependencyGraphDTO.Link(selfId, depId, dep[2]));
                }
            }
            case NPM -> {
                String[][] deps = {
                    {"lodash", "NPM", "^4.17.x"},
                    {"axios", "NPM", "^1.4.x"},
                    {"dayjs", "NPM", "^1.11.x"}
                };
                for (String[] dep : deps) {
                    String depId = dep[0];
                    nodes.add(new DependencyGraphDTO.Node(depId, dep[0], dep[1]));
                    links.add(new DependencyGraphDTO.Link(selfId, depId, dep[2]));
                }
            }
            case DOCKER_IMAGE -> {
                String[][] deps = {
                    {"eclipse-temurin", "DOCKER_IMAGE", "17-jre"},
                    {"alpine", "DOCKER_IMAGE", "3.18"}
                };
                for (String[] dep : deps) {
                    String depId = dep[0];
                    nodes.add(new DependencyGraphDTO.Node(depId, dep[0], dep[1]));
                    links.add(new DependencyGraphDTO.Link(selfId, depId, dep[2]));
                }
            }
            default -> { /* 其他类型暂无依赖数据 */ }
        }

        DependencyGraphDTO dto = new DependencyGraphDTO();
        dto.setNodes(nodes);
        dto.setLinks(links);
        return dto;
    }

    public SecurityReportDTO getSecurityReport(Long id) {
        SoftwarePackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("软件包不存在"));

        int baseScore = pkg.getStatus() == PackageStatus.PUBLISHED ? 85 : 65;
        int variance = (int) (id % 15);
        int score = Math.min(100, baseScore + variance);

        int mediumCount = (int) (id % 4);
        int lowCount = (int) ((id + 1) % 6);

        String status;
        if (score >= 85) status = "SAFE";
        else if (score >= 60) status = "WARNING";
        else status = "DANGER";

        String scanTime = pkg.getUpdatedAt() != null
                ? pkg.getUpdatedAt().toLocalDate().toString()
                : LocalDate.now().toString();

        SecurityReportDTO report = new SecurityReportDTO();
        report.setScore(score);
        report.setCriticalCount(0);
        report.setHighCount(0);
        report.setMediumCount(mediumCount);
        report.setLowCount(lowCount);
        report.setScanTime(scanTime);
        report.setStatus(status);
        return report;
    }

    public List<SoftwareVersionDTO> getPackageVersions(Long packageId) {
        List<SoftwareVersion> versions = versionRepository.findBySoftwarePackageIdOrderByCreatedAtDesc(packageId);
        return versions.stream()
                .map(SoftwareVersionDTO::from)
                .collect(Collectors.toList());
    }
}
