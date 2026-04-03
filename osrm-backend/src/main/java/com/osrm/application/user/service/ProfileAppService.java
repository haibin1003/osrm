package com.osrm.application.user.service;

import com.osrm.application.user.dto.ProfileDTO;
import com.osrm.application.user.dto.request.ChangePasswordRequest;
import com.osrm.application.user.dto.request.UpdateProfileRequest;
import com.osrm.domain.user.entity.User;
import com.osrm.domain.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * 个人中心应用服务
 */
@Service
@Transactional(readOnly = true)
public class ProfileAppService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileAppService(UserRepository userRepository,
                             PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ProfileDTO getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return convertToDTO(user);
    }

    @Transactional
    public ProfileDTO updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        User saved = userRepository.save(user);
        return convertToDTO(saved);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 验证原密码
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        // 校验新密码强度
        com.osrm.infrastructure.config.PasswordValidator.validate(request.getNewPassword());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private ProfileDTO convertToDTO(User user) {
        ProfileDTO dto = new ProfileDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRealName(user.getRealName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setBio(user.getBio());
        dto.setAvatar(user.getAvatar());
        dto.setLastLoginTime(user.getLastLoginAt());
        dto.setCreateTime(user.getCreatedAt());

        if (user.getRoles() != null) {
            dto.setRoles(user.getRoles().stream()
                    .map(r -> r.getRoleName())
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}
