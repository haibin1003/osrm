package com.osrm.application.tracking.service;

import com.osrm.application.tracking.dto.*;
import com.osrm.application.tracking.dto.response.PackageImpactDTO;
import com.osrm.application.tracking.dto.response.SystemDependenciesDTO;
import com.osrm.domain.business.entity.BusinessSystem;
import com.osrm.domain.business.repository.BusinessSystemRepository;
import com.osrm.domain.inventory.entity.InventoryRecord;
import com.osrm.domain.inventory.repository.InventoryRecordRepository;
import com.osrm.domain.software.entity.SoftwarePackage;
import com.osrm.domain.software.repository.SoftwarePackageRepository;
import com.osrm.domain.software.repository.SoftwareVersionRepository;
import com.osrm.domain.subscription.entity.Subscription;
import com.osrm.domain.subscription.entity.SubscriptionStatus;
import com.osrm.domain.subscription.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 关联图谱应用服务
 */
@Service
@Transactional(readOnly = true)
public class RelationshipGraphAppService {

    private final SubscriptionRepository subscriptionRepository;
    private final BusinessSystemRepository businessSystemRepository;
    private final SoftwarePackageRepository softwarePackageRepository;
    private final SoftwareVersionRepository softwareVersionRepository;
    private final InventoryRecordRepository inventoryRecordRepository;

    @Autowired
    public RelationshipGraphAppService(SubscriptionRepository subscriptionRepository,
                                       BusinessSystemRepository businessSystemRepository,
                                       SoftwarePackageRepository softwarePackageRepository,
                                       SoftwareVersionRepository softwareVersionRepository,
                                       InventoryRecordRepository inventoryRecordRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.businessSystemRepository = businessSystemRepository;
        this.softwarePackageRepository = softwarePackageRepository;
        this.softwareVersionRepository = softwareVersionRepository;
        this.inventoryRecordRepository = inventoryRecordRepository;
    }

    /**
     * 构建完整的系统-软件关联图
     * 同时包含订阅关系和存量上报数据
     */
    public RelationshipGraphDTO buildRelationshipGraph(String domain, String softwareType, String status) {
        // 获取所有启用的业务系统
        List<BusinessSystem> systems = businessSystemRepository.findByEnabled(true);

        // 按业务域过滤
        if (domain != null && !domain.isEmpty()) {
            systems = systems.stream()
                    .filter(s -> s.getDomain() != null && s.getDomain().name().equals(domain))
                    .toList();
        }

        // 获取所有已发布的软件包
        List<SoftwarePackage> packages = softwarePackageRepository.findAll();

        // 按软件类型过滤
        if (softwareType != null && !softwareType.isEmpty()) {
            packages = packages.stream()
                    .filter(p -> p.getSoftwareType() != null && p.getSoftwareType().name().equals(softwareType))
                    .toList();
        }

        // 获取所有订阅关系
        List<Subscription> subscriptions = subscriptionRepository.findAll();

        // 按状态过滤订阅
        if (status != null && !status.isEmpty()) {
            subscriptions = subscriptions.stream()
                    .filter(s -> s.getStatus() != null && s.getStatus().name().equals(status))
                    .toList();
        }

        // 获取存量记录（已批准的）
        List<InventoryRecord> inventoryRecords = inventoryRecordRepository.findAll().stream()
                .filter(r -> r.getStatus() == InventoryRecord.InventoryStatus.APPROVED)
                .toList();

        // 获取有关联的系统ID和软件包ID（来自订阅）
        Set<Long> connectedSystemIdsFromSub = subscriptions.stream()
                .map(Subscription::getBusinessSystemId)
                .collect(Collectors.toSet());

        // 获取有关联的系统ID和软件包ID（来自存量）
        Set<Long> connectedSystemIdsFromInv = inventoryRecords.stream()
                .map(InventoryRecord::getBusinessSystemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 存量记录通过 packageName 匹配软件包
        Map<String, SoftwarePackage> packageNameMap = packages.stream()
                .collect(Collectors.toMap(SoftwarePackage::getPackageName, p -> p, (a, b) -> a));

        Set<Long> connectedPackageIdsFromInv = inventoryRecords.stream()
                .map(r -> {
                    SoftwarePackage pkg = packageNameMap.get(r.getPackageName());
                    return pkg != null ? pkg.getId() : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> connectedPackageIdsFromSub = subscriptions.stream()
                .map(Subscription::getPackageId)
                .collect(Collectors.toSet());

        // 合并所有关联的系统ID
        Set<Long> allConnectedSystemIds = new HashSet<>(connectedSystemIdsFromSub);
        allConnectedSystemIds.addAll(connectedSystemIdsFromInv);

        // 合并所有关联的软件包ID
        Set<Long> allConnectedPackageIds = new HashSet<>(connectedPackageIdsFromSub);
        allConnectedPackageIds.addAll(connectedPackageIdsFromInv);

        // 过滤出有关联的系统和软件包
        List<BusinessSystem> filteredSystems = systems.stream()
                .filter(s -> allConnectedSystemIds.contains(s.getId()))
                .toList();

        List<SoftwarePackage> filteredPackages = packages.stream()
                .filter(p -> allConnectedPackageIds.contains(p.getId()))
                .toList();

        // 构建节点
        List<GraphNode> nodes = new ArrayList<>();

        // 系统节点
        for (BusinessSystem system : filteredSystems) {
            nodes.add(new SystemNode(
                    "system:" + system.getId(),
                    system.getSystemName(),
                    system.getId(),
                    system.getSystemCode(),
                    system.getDomain() != null ? system.getDomain().name() : null,
                    system.getEnabled()
            ));
        }

        // 软件包节点
        for (SoftwarePackage pkg : filteredPackages) {
            nodes.add(new PackageNode(
                    "package:" + pkg.getId(),
                    pkg.getPackageName(),
                    pkg.getId(),
                    pkg.getPackageKey(),
                    pkg.getSoftwareType() != null ? pkg.getSoftwareType().name() : null,
                    pkg.getStatus() != null ? pkg.getStatus().name() : null
            ));
        }

        // 构建边（订阅关系）
        List<GraphEdge> edges = new ArrayList<>();
        int edgeId = 1;
        for (Subscription sub : subscriptions) {
            // 只包含有节点的边
            boolean hasSystem = filteredSystems.stream().anyMatch(s -> s.getId().equals(sub.getBusinessSystemId()));
            boolean hasPackage = filteredPackages.stream().anyMatch(p -> p.getId().equals(sub.getPackageId()));

            if (hasSystem && hasPackage) {
                edges.add(new GraphEdge(
                        "edge:" + edgeId++,
                        "system:" + sub.getBusinessSystemId(),
                        "package:" + sub.getPackageId(),
                        getVersionNumber(sub.getVersionId()),
                        sub.getStatus() != null ? sub.getStatus().name() : null,
                        "SUBSCRIPTION"
                ));
            }
        }

        // 构建边（存量记录）
        for (InventoryRecord inv : inventoryRecords) {
            boolean hasSystem = filteredSystems.stream().anyMatch(s -> s.getId().equals(inv.getBusinessSystemId()));
            if (!hasSystem) continue;

            SoftwarePackage pkg = packageNameMap.get(inv.getPackageName());
            if (pkg == null) continue;
            boolean hasPackage = filteredPackages.stream().anyMatch(p -> p.getId().equals(pkg.getId()));
            if (!hasPackage) continue;

            edges.add(new GraphEdge(
                    "edge:inv:" + edgeId++,
                    "system:" + inv.getBusinessSystemId(),
                    "package:" + pkg.getId(),
                    inv.getVersionNo(),
                    inv.getStatus() != null ? inv.getStatus().name() : null,
                    "INVENTORY"
            ));
        }

        // 元数据
        GraphMetadata metadata = new GraphMetadata(
                filteredSystems.size(),
                filteredPackages.size(),
                edges.size()
        );

        return new RelationshipGraphDTO(nodes, edges, metadata);
    }

    /**
     * 获取系统依赖详情（包含订阅和存量）
     */
    public SystemDependenciesDTO getSystemDependencies(Long systemId) {
        BusinessSystem system = businessSystemRepository.findById(systemId).orElse(null);
        if (system == null) {
            return new SystemDependenciesDTO();
        }

        // 获取该系统的所有订阅
        List<Subscription> subscriptions = subscriptionRepository.findAll().stream()
                .filter(s -> s.getBusinessSystemId().equals(systemId))
                .toList();

        // 获取该系统的存量记录
        List<InventoryRecord> inventoryRecords = inventoryRecordRepository.findAll().stream()
                .filter(r -> systemId.equals(r.getBusinessSystemId()))
                .filter(r -> r.getStatus() == InventoryRecord.InventoryStatus.APPROVED)
                .toList();

        // 通过 packageName 匹配软件包
        Map<String, SoftwarePackage> packageNameMap = softwarePackageRepository.findAll().stream()
                .collect(Collectors.toMap(SoftwarePackage::getPackageName, p -> p, (a, b) -> a));

        List<SystemDependenciesDTO.PackageDependencyInfo> packages = new ArrayList<>();
        Map<String, Integer> byType = new HashMap<>();

        // 处理订阅
        for (Subscription sub : subscriptions) {
            SoftwarePackage pkg = softwarePackageRepository.findById(sub.getPackageId()).orElse(null);
            if (pkg != null) {
                SystemDependenciesDTO.PackageDependencyInfo info = new SystemDependenciesDTO.PackageDependencyInfo();
                info.setPackageId(pkg.getId());
                info.setPackageName(pkg.getPackageName());
                info.setPackageKey(pkg.getPackageKey());
                info.setSoftwareType(pkg.getSoftwareType() != null ? pkg.getSoftwareType().name() : null);
                info.setVersionNumber(getVersionNumber(sub.getVersionId()));
                info.setStatus(sub.getStatus() != null ? sub.getStatus().name() : null);
                packages.add(info);

                // 统计
                String type = pkg.getSoftwareType() != null ? pkg.getSoftwareType().name() : "UNKNOWN";
                byType.merge(type, 1, Integer::sum);
            }
        }

        // 处理存量记录
        for (InventoryRecord inv : inventoryRecords) {
            SoftwarePackage pkg = packageNameMap.get(inv.getPackageName());
            if (pkg == null) continue;

            // 避免重复添加
            boolean exists = packages.stream().anyMatch(p -> p.getPackageId().equals(pkg.getId()));
            if (exists) continue;

            SystemDependenciesDTO.PackageDependencyInfo info = new SystemDependenciesDTO.PackageDependencyInfo();
            info.setPackageId(pkg.getId());
            info.setPackageName(pkg.getPackageName());
            info.setPackageKey(pkg.getPackageKey());
            info.setSoftwareType(pkg.getSoftwareType() != null ? pkg.getSoftwareType().name() : null);
            info.setVersionNumber(inv.getVersionNo());
            info.setStatus(inv.getStatus() != null ? inv.getStatus().name() : null);
            packages.add(info);

            // 统计
            String type = pkg.getSoftwareType() != null ? pkg.getSoftwareType().name() : "UNKNOWN";
            byType.merge(type, 1, Integer::sum);
        }

        SystemDependenciesDTO result = new SystemDependenciesDTO();
        result.setSystem(new SystemNode(
                "system:" + system.getId(),
                system.getSystemName(),
                system.getId(),
                system.getSystemCode(),
                system.getDomain() != null ? system.getDomain().name() : null,
                system.getEnabled()
        ));
        result.setPackages(packages);

        SystemDependenciesDTO.DependencyStatistics stats = new SystemDependenciesDTO.DependencyStatistics();
        stats.setTotalPackages(packages.size());
        stats.setByType(byType);
        result.setStatistics(stats);

        return result;
    }

    /**
     * 获取软件影响分析（包含订阅和存量）
     */
    public PackageImpactDTO getPackageImpact(Long packageId) {
        SoftwarePackage pkg = softwarePackageRepository.findById(packageId).orElse(null);
        if (pkg == null) {
            return new PackageImpactDTO();
        }

        // 获取该软件的所有订阅
        List<Subscription> subscriptions = subscriptionRepository.findAll().stream()
                .filter(s -> s.getPackageId().equals(packageId))
                .filter(s -> s.getStatus() == SubscriptionStatus.APPROVED)
                .toList();

        // 获取使用该软件包名的存量记录
        List<InventoryRecord> inventoryRecords = inventoryRecordRepository.findAll().stream()
                .filter(r -> r.getPackageName().equals(pkg.getPackageName()))
                .filter(r -> r.getStatus() == InventoryRecord.InventoryStatus.APPROVED)
                .toList();

        List<PackageImpactDTO.AffectedSystemInfo> affectedSystems = new ArrayList<>();
        Map<String, Integer> byDomain = new HashMap<>();
        Map<String, Integer> byVersion = new HashMap<>();
        Set<Long> addedSystemIds = new HashSet<>();

        // 处理订阅
        for (Subscription sub : subscriptions) {
            BusinessSystem system = businessSystemRepository.findById(sub.getBusinessSystemId()).orElse(null);
            if (system != null && !addedSystemIds.contains(system.getId())) {
                String verNo = getVersionNumber(sub.getVersionId());
                PackageImpactDTO.AffectedSystemInfo info = new PackageImpactDTO.AffectedSystemInfo();
                info.setSystemId(system.getId());
                info.setSystemName(system.getSystemName());
                info.setSystemCode(system.getSystemCode());
                info.setDomain(system.getDomain() != null ? system.getDomain().name() : null);
                info.setVersionNumber(verNo);
                affectedSystems.add(info);
                addedSystemIds.add(system.getId());

                // 统计
                String domain = system.getDomain() != null ? system.getDomain().name() : "UNKNOWN";
                byDomain.merge(domain, 1, Integer::sum);

                String version = verNo != null ? verNo : "UNKNOWN";
                byVersion.merge(version, 1, Integer::sum);
            }
        }

        // 处理存量记录
        for (InventoryRecord inv : inventoryRecords) {
            if (!addedSystemIds.contains(inv.getBusinessSystemId())) {
                BusinessSystem system = businessSystemRepository.findById(inv.getBusinessSystemId()).orElse(null);
                if (system != null) {
                    PackageImpactDTO.AffectedSystemInfo info = new PackageImpactDTO.AffectedSystemInfo();
                    info.setSystemId(system.getId());
                    info.setSystemName(system.getSystemName());
                    info.setSystemCode(system.getSystemCode());
                    info.setDomain(system.getDomain() != null ? system.getDomain().name() : null);
                    info.setVersionNumber(inv.getVersionNo());
                    affectedSystems.add(info);
                    addedSystemIds.add(system.getId());

                    // 统计
                    String domain = system.getDomain() != null ? system.getDomain().name() : "UNKNOWN";
                    byDomain.merge(domain, 1, Integer::sum);

                    String version = inv.getVersionNo() != null ? inv.getVersionNo() : "UNKNOWN";
                    byVersion.merge(version, 1, Integer::sum);
                }
            }
        }

        PackageImpactDTO result = new PackageImpactDTO();
        result.setPackageInfo(new PackageNode(
                "package:" + pkg.getId(),
                pkg.getPackageName(),
                pkg.getId(),
                pkg.getPackageKey(),
                pkg.getSoftwareType() != null ? pkg.getSoftwareType().name() : null,
                pkg.getStatus() != null ? pkg.getStatus().name() : null
        ));
        result.setAffectedSystems(affectedSystems);

        PackageImpactDTO.ImpactStatistics stats = new PackageImpactDTO.ImpactStatistics();
        stats.setTotalSystems(affectedSystems.size());
        stats.setByDomain(byDomain);
        stats.setByVersion(byVersion);
        result.setStatistics(stats);

        return result;
    }

    private String getVersionNumber(Long versionId) {
        if (versionId == null) return null;
        return softwareVersionRepository.findById(versionId)
                .map(v -> v.getVersionNo())
                .orElse(null);
    }
}
