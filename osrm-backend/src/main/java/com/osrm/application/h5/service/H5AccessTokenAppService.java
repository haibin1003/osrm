package com.osrm.application.h5.service;

import com.osrm.application.h5.dto.ExtendTokenRequest;
import com.osrm.application.h5.dto.GenerateTokenRequest;
import com.osrm.application.h5.dto.H5AccessTokenDTO;
import com.osrm.common.exception.BizException;
import com.osrm.domain.h5.entity.H5AccessToken;
import com.osrm.domain.h5.repository.H5AccessTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class H5AccessTokenAppService {

    private final H5AccessTokenRepository h5AccessTokenRepository;

    @Autowired
    public H5AccessTokenAppService(H5AccessTokenRepository h5AccessTokenRepository) {
        this.h5AccessTokenRepository = h5AccessTokenRepository;
    }

    @Transactional
    public H5AccessTokenDTO generate(GenerateTokenRequest req, Long adminId, String adminName) {
        H5AccessToken t = new H5AccessToken();
        t.setToken(UUID.randomUUID().toString().replace("-", ""));
        t.setExpireAt(LocalDateTime.now().plusDays(req.getValidDays()));
        t.setEnabled(true);
        t.setCreatedBy(adminId);
        t.setCreatedByName(adminName);
        t.setRemark(req.getRemark());
        t.setAccessCount(0L);
        return H5AccessTokenDTO.from(h5AccessTokenRepository.save(t));
    }

    public Page<H5AccessTokenDTO> list(Pageable pageable) {
        return h5AccessTokenRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(H5AccessTokenDTO::from);
    }

    @Transactional
    public H5AccessTokenDTO revoke(Long id) {
        H5AccessToken t = h5AccessTokenRepository.findById(id)
                .orElseThrow(() -> new BizException("令牌不存在"));
        t.setEnabled(false);
        return H5AccessTokenDTO.from(h5AccessTokenRepository.save(t));
    }

    @Transactional
    public H5AccessTokenDTO extend(Long id, ExtendTokenRequest req) {
        H5AccessToken t = h5AccessTokenRepository.findById(id)
                .orElseThrow(() -> new BizException("令牌不存在"));
        t.setExpireAt(t.getExpireAt().plusDays(req.getExtraDays()));
        t.setEnabled(true);
        return H5AccessTokenDTO.from(h5AccessTokenRepository.save(t));
    }

    @Transactional
    public void validate(String token) {
        if (token == null || token.isBlank()) {
            throw new BizException(403, "缺少访问令牌");
        }
        H5AccessToken t = h5AccessTokenRepository.findByToken(token)
                .orElseThrow(() -> new BizException(403, "链接已失效"));

        if (!Boolean.TRUE.equals(t.getEnabled())) {
            throw new BizException(403, "链接已失效");
        }
        if (t.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BizException(403, "链接已失效");
        }

        t.setAccessCount(t.getAccessCount() + 1);
        t.setLastAccessAt(LocalDateTime.now());
        h5AccessTokenRepository.save(t);
    }
}
