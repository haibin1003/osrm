package com.osrm.application.catalog.service;

import com.osrm.application.catalog.dto.response.ApplicationCatalogDTO;
import com.osrm.application.catalog.dto.response.SystemCatalogDTO;
import com.osrm.common.exception.BizException;
import com.osrm.common.model.PageResult;
import com.osrm.domain.business.entity.BusinessSystem;
import com.osrm.domain.business.repository.BusinessSystemApplicationRepository;
import com.osrm.domain.business.repository.BusinessSystemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SystemCatalogAppService {

    private final BusinessSystemRepository businessSystemRepository;
    private final BusinessSystemApplicationRepository businessSystemApplicationRepository;

    @Autowired
    public SystemCatalogAppService(BusinessSystemRepository businessSystemRepository,
                                   BusinessSystemApplicationRepository businessSystemApplicationRepository) {
        this.businessSystemRepository = businessSystemRepository;
        this.businessSystemApplicationRepository = businessSystemApplicationRepository;
    }

    /**
     * 分页查询业务系统。
     */
    public PageResult<SystemCatalogDTO> findByConditions(String keyword, String status, Boolean enabled, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "systemName"));
        BusinessSystem.SystemStatus statusEnum = (status != null && !status.isEmpty()) ? BusinessSystem.SystemStatus.valueOf(status) : null;
        Page<BusinessSystem> pageResult = businessSystemRepository.findByConditions(keyword, statusEnum, enabled, pageable);
        List<SystemCatalogDTO> content = pageResult.getContent().stream()
                .map(SystemCatalogDTO::from)
                .collect(Collectors.toList());
        return PageResult.of(content, pageResult.getTotalElements(), pageResult.getTotalPages(), pageResult.getSize(), pageResult.getNumber() + 1);
    }

    /**
     * 获取系统详情(含子应用列表)。
     */
    public SystemCatalogDTO findById(Long id) {
        BusinessSystem system = businessSystemRepository.findById(id)
                .orElseThrow(() -> new BizException("系统不存在"));
        var apps = businessSystemApplicationRepository
                .findByBusinessSystemIdOrderByApplicationName(system.getId());
        return SystemCatalogDTO.fromWithApplications(system, apps);
    }

    /**
     * 通过 systemCode 获取系统(供盘点等内部调用使用)。
     */
    public BusinessSystem requireBySystemCode(String systemCode) {
        return businessSystemRepository.findBySystemCode(systemCode)
                .orElseThrow(() -> new BizException("系统不存在: " + systemCode));
    }

    /**
     * 列出指定系统下启用的应用(供 H5 / 内部下拉)。
     */
    public List<ApplicationCatalogDTO> listEnabledApplicationsBySystem(Long systemId) {
        BusinessSystem system = businessSystemRepository.findById(systemId)
                .orElseThrow(() -> new BizException("系统不存在"));
        return businessSystemApplicationRepository
                .findByBusinessSystemIdAndEnabledTrueOrderByApplicationName(system.getId())
                .stream()
                .map(ApplicationCatalogDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 启用/停用系统。
     */
    @Transactional
    public SystemCatalogDTO setEnabled(Long id, boolean enabled) {
        BusinessSystem system = businessSystemRepository.findById(id)
                .orElseThrow(() -> new BizException("系统不存在"));
        system.setEnabled(enabled);
        BusinessSystem saved = businessSystemRepository.save(system);
        return SystemCatalogDTO.from(saved);
    }
}
