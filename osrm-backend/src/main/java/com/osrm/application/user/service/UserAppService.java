package com.osrm.application.user.service;

import com.osrm.application.user.dto.UserDTO;
import com.osrm.application.user.dto.request.CreateUserRequest;
import com.osrm.application.user.dto.request.UpdateUserRequest;
import com.osrm.domain.user.entity.User;
import com.osrm.domain.user.repository.UserRepository;
import com.osrm.domain.user.repository.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * 用户应用服务
 */
@Service
@Transactional(readOnly = true)
public class UserAppService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAppService(UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 查询用户列表（分页）
     */
    public Page<UserDTO> listUsers(String username, String realName, Boolean enabled, Pageable pageable) {
        Page<User> userPage = userRepository.findByConditions(username, realName, enabled, pageable);
        return userPage.map(this::convertToDTO);
    }

    /**
     * 根据ID查询用户
     */
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return convertToDTO(user);
    }

    /**
     * 创建用户
     */
    @Transactional
    public UserDTO createUser(CreateUserRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 校验密码强度
        com.osrm.infrastructure.config.PasswordValidator.validate(request.getPassword());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(request.getEnabled());
        user.setLoginFailCount(0);

        // 设置角色
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            user.setRoles(new java.util.HashSet<>(roleRepository.findAllById(request.getRoleIds())));
        }

        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    /**
     * 更新用户
     */
    @Transactional
    public UserDTO updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setEnabled(request.getEnabled());

        // 更新角色
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            user.setRoles(new java.util.HashSet<>(roleRepository.findAllById(request.getRoleIds())));
        }

        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    /**
     * 删除用户
     */
    @Transactional
    public void deleteUser(Long id, Long currentUserId) {
        // 不能删除当前登录用户
        if (id.equals(currentUserId)) {
            throw new RuntimeException("不能删除当前登录用户");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        userRepository.delete(user);
    }

    /**
     * 重置密码
     */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        com.osrm.infrastructure.config.PasswordValidator.validate(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 切换用户状态
     */
    @Transactional
    public UserDTO toggleStatus(Long id, Boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        user.setEnabled(enabled);
        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    /**
     * 转换为DTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRealName(user.getRealName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setEnabled(user.getEnabled());
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
