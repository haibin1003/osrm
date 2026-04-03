package com.osrm.application.business.service;

import com.osrm.application.business.dto.request.CreateBusinessSystemRequest;
import com.osrm.application.business.dto.request.UpdateBusinessSystemRequest;
import com.osrm.application.business.dto.response.BusinessSystemDTO;
import com.osrm.common.exception.BizException;
import com.osrm.common.model.PageResult;
import com.osrm.domain.business.entity.BusinessDomain;
import com.osrm.domain.business.entity.BusinessSystem;
import com.osrm.domain.business.repository.BusinessSystemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BusinessSystemAppService {

    private final BusinessSystemRepository businessSystemRepository;

    @Autowired
    public BusinessSystemAppService(BusinessSystemRepository businessSystemRepository) {
        this.businessSystemRepository = businessSystemRepository;
    }

    /**
     * 分页查询业务系统
     */
    public PageResult<BusinessSystemDTO> findByConditions(String keyword, String domain, Boolean enabled, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        BusinessDomain domainEnum = domain != null && !domain.isEmpty() ? BusinessDomain.valueOf(domain) : null;
        Page<BusinessSystem> pageResult = businessSystemRepository.findByConditions(keyword, domainEnum, enabled, pageable);
        List<BusinessSystemDTO> content = pageResult.getContent().stream()
                .map(BusinessSystemDTO::from)
                .collect(Collectors.toList());
        return PageResult.of(content, pageResult.getTotalElements(), pageResult.getTotalPages(), pageResult.getSize(), pageResult.getNumber() + 1);
    }

    /**
     * 获取业务系统详情
     */
    public BusinessSystemDTO findById(Long id) {
        BusinessSystem system = businessSystemRepository.findById(id)
                .orElseThrow(() -> new BizException("业务系统不存在"));
        return BusinessSystemDTO.from(system);
    }

    /**
     * 创建业务系统
     */
    @Transactional
    public BusinessSystemDTO create(CreateBusinessSystemRequest request, Long createdBy) {
        if (businessSystemRepository.existsBySystemCode(request.getSystemCode())) {
            throw new BizException("系统编码已存在");
        }
        if (businessSystemRepository.existsBySystemName(request.getSystemName())) {
            throw new BizException("系统名称已存在");
        }

        BusinessSystem system = new BusinessSystem();
        system.setSystemCode(request.getSystemCode());
        system.setSystemName(request.getSystemName());
        system.setDomain(request.getDomain());
        system.setResponsiblePerson(request.getResponsiblePerson());
        system.setDescription(request.getDescription());
        system.setCreatedBy(createdBy);
        system.setEnabled(true);

        BusinessSystem saved = businessSystemRepository.save(system);
        return BusinessSystemDTO.from(saved);
    }

    /**
     * 更新业务系统
     */
    @Transactional
    public BusinessSystemDTO update(Long id, UpdateBusinessSystemRequest request) {
        BusinessSystem system = businessSystemRepository.findById(id)
                .orElseThrow(() -> new BizException("业务系统不存在"));

        // 检查名称唯一性
        businessSystemRepository.findBySystemName(request.getSystemName())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BizException("系统名称已存在");
                    }
                });

        system.setSystemName(request.getSystemName());
        system.setDomain(request.getDomain());
        system.setResponsiblePerson(request.getResponsiblePerson());
        system.setDescription(request.getDescription());

        BusinessSystem saved = businessSystemRepository.save(system);
        return BusinessSystemDTO.from(saved);
    }

    /**
     * 启用/停用业务系统
     */
    @Transactional
    public BusinessSystemDTO setEnabled(Long id, boolean enabled) {
        BusinessSystem system = businessSystemRepository.findById(id)
                .orElseThrow(() -> new BizException("业务系统不存在"));

        if (enabled) {
            system.enable();
        } else {
            system.disable();
        }

        BusinessSystem saved = businessSystemRepository.save(system);
        return BusinessSystemDTO.from(saved);
    }

    /**
     * 删除业务系统
     */
    @Transactional
    public void delete(Long id) {
        BusinessSystem system = businessSystemRepository.findById(id)
                .orElseThrow(() -> new BizException("业务系统不存在"));

        if (Boolean.TRUE.equals(system.getEnabled())) {
            throw new BizException("已启用的业务系统不能删除，请先停用后再删除");
        }

        businessSystemRepository.delete(system);
    }

    /**
     * 获取所有业务域
     */
    public List<BusinessDomain> getAllDomains() {
        return Arrays.asList(BusinessDomain.values());
    }
}
