package com.osrm.application.h5.service;

import com.osrm.application.h5.dto.request.H5InventorySubmitRequest;
import com.osrm.application.h5.dto.response.H5SubmitResponse;
import com.osrm.common.exception.BizException;
import com.osrm.common.util.HtmlSanitizer;
import com.osrm.domain.business.entity.BusinessSystem;
import com.osrm.domain.business.entity.BusinessSystemApplication;
import com.osrm.domain.business.repository.BusinessSystemApplicationRepository;
import com.osrm.domain.business.repository.BusinessSystemRepository;
import com.osrm.domain.inventory.entity.InventoryRecord;
import com.osrm.domain.inventory.repository.InventoryRecordRepository;
import com.osrm.domain.user.entity.Role;
import com.osrm.domain.user.entity.User;
import com.osrm.domain.user.repository.RoleRepository;
import com.osrm.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class H5InventoryAppService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    private final CaptchaService captchaService;
    private final BusinessSystemRepository businessSystemRepository;
    private final BusinessSystemApplicationRepository businessSystemApplicationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final InventoryRecordRepository inventoryRecordRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public H5InventoryAppService(CaptchaService captchaService,
                                 BusinessSystemRepository businessSystemRepository,
                                 BusinessSystemApplicationRepository businessSystemApplicationRepository,
                                 UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 InventoryRecordRepository inventoryRecordRepository,
                                 PasswordEncoder passwordEncoder) {
        this.captchaService = captchaService;
        this.businessSystemRepository = businessSystemRepository;
        this.businessSystemApplicationRepository = businessSystemApplicationRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.inventoryRecordRepository = inventoryRecordRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public H5SubmitResponse submit(H5InventorySubmitRequest req, String clientIp) {
        // 1. 校验验证码
        captchaService.verify(req.getCaptchaKey(), req.getCaptchaCode());

        // 2. 校验业务系统
        BusinessSystem system = businessSystemRepository.findById(req.getSystemCatalogId())
                .orElseThrow(() -> new BizException("所选系统不存在"));
        if (!Boolean.TRUE.equals(system.getEnabled())) {
            throw new BizException("所选系统已停用");
        }

        // 3. 校验子应用(可选)
        BusinessSystemApplication app = null;
        if (req.getApplicationCatalogId() != null) {
            app = businessSystemApplicationRepository.findById(req.getApplicationCatalogId())
                    .orElseThrow(() -> new BizException("所选应用不存在"));
            if (!app.getBusinessSystemId().equals(system.getId())) {
                throw new BizException("所选应用不属于该系统");
            }
            if (!Boolean.TRUE.equals(app.getEnabled())) {
                throw new BizException("所选应用已停用");
            }
        }

        // 4. 自动 upsert 用户(姓名 + 手机号去重)
        String initialPassword = null;
        User user = userRepository.findByRealNameAndPhone(req.getResponsiblePerson(), req.getPhone())
                .orElse(null);
        boolean isNewUser = false;
        if (user == null) {
            initialPassword = generateInitialPassword();
            user = createH5User(req, initialPassword);
            isNewUser = true;
        }

        // 5. 批量写入盘点记录
        List<InventoryRecord> records = new ArrayList<>();
        for (H5InventorySubmitRequest.SoftwareEntry entry : req.getSoftwareEntries()) {
            InventoryRecord record = new InventoryRecord();
            record.setUserId(user.getId());
            record.setRecordNo(generateRecordNo());
            record.setBusinessSystemId(system.getId());
            record.setBusinessSystemApplicationId(app != null ? app.getId() : null);
            record.setPackageId(entry.getPackageId());
            record.setPackageName(resolveH5PackageName(entry.getPackageId(), entry.getPackageName()));
            record.setVersionNo(entry.getVersionNo());
            record.setSoftwareType(entry.getSoftwareType());
            record.setResponsiblePerson(req.getResponsiblePerson());
            record.setDeployEnvironment(entry.getDeployEnvironment());
            record.setServerCount(entry.getServerCount() != null ? entry.getServerCount() : 1);
            record.setUsageScenario(HtmlSanitizer.sanitizeText(entry.getUsageScenario()));
            record.setRemarks(HtmlSanitizer.sanitizeText(entry.getRemarks()));
            record.setSourceType(InventoryRecord.SourceType.MANUAL);
            record.setSubmitSource(InventoryRecord.SubmitSource.H5);
            record.setStatus(InventoryRecord.InventoryStatus.PENDING);
            records.add(record);
        }

        List<InventoryRecord> saved = inventoryRecordRepository.saveAll(records);
        List<Long> ids = saved.stream().map(InventoryRecord::getId).collect(java.util.stream.Collectors.toList());

        if (isNewUser) {
            return new H5SubmitResponse(ids, user.getUsername(), initialPassword);
        }
        return new H5SubmitResponse(ids, user.getUsername(), null);
    }

    private String resolveH5PackageName(Long packageId, String packageName) {
        if (packageName != null && !packageName.trim().isEmpty()) {
            return HtmlSanitizer.sanitizeText(packageName);
        }
        if (packageId != null) {
            throw new BizException("请填写软件名称");
        }
        throw new BizException("软件名称不能为空");
    }

    private User createH5User(H5InventorySubmitRequest req, String rawPassword) {
        Role developer = roleRepository.findByRoleCode("ROLE_DEVELOPER")
                .orElseThrow(() -> new BizException("默认角色未初始化"));

        String username = generateUsername(req.getEmail());

        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setRealName(req.getResponsiblePerson());
        u.setPhone(req.getPhone());
        u.setEmail(req.getEmail());
        u.setDepartment(req.getDepartment());
        u.setSource("H5_SELF_REGISTER");
        u.setEnabled(true);
        u.setRoles(Set.of(developer));

        return userRepository.save(u);
    }

    private String generateUsername(String email) {
        String prefix = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
        String base = prefix.replaceAll("[^a-zA-Z0-9._-]", "");
        if (!userRepository.existsByUsername(base)) {
            return base;
        }
        for (int i = 1; i <= 999; i++) {
            String candidate = base + i;
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }
        return base + "_" + UUID.randomUUID().toString().substring(0, 6);
    }

    private String generateInitialPassword() {
        return generateRandomString(8);
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    private String generateRecordNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "INV-" + date + "-" + uuid;
    }
}
