package com.osrm.interfaces.rest.admin;

import com.osrm.application.h5.dto.ExtendTokenRequest;
import com.osrm.application.h5.dto.GenerateTokenRequest;
import com.osrm.application.h5.dto.H5AccessTokenDTO;
import com.osrm.application.h5.service.H5AccessTokenAppService;
import com.osrm.common.model.ApiResponse;
import com.osrm.common.model.PageResult;
import com.osrm.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/h5-tokens")
public class H5AccessTokenController {

    @Autowired
    private H5AccessTokenAppService h5AccessTokenAppService;

    @PostMapping
    @PreAuthorize("hasAuthority('system:manage')")
    public ApiResponse<H5AccessTokenDTO> generate(@Valid @RequestBody GenerateTokenRequest request) {
        UserPrincipal user = getCurrentUser();
        return ApiResponse.success(h5AccessTokenAppService.generate(request, user.getId(), user.getUsername()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('system:manage')")
    public ApiResponse<PageResult<H5AccessTokenDTO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<H5AccessTokenDTO> pageResult = h5AccessTokenAppService.list(
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.success(PageResult.of(
                pageResult.getContent(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.getSize(),
                pageResult.getNumber() + 1));
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('system:manage')")
    public ApiResponse<H5AccessTokenDTO> revoke(@PathVariable Long id) {
        return ApiResponse.success(h5AccessTokenAppService.revoke(id));
    }

    @PostMapping("/{id}/extend")
    @PreAuthorize("hasAuthority('system:manage')")
    public ApiResponse<H5AccessTokenDTO> extend(@PathVariable Long id, @Valid @RequestBody ExtendTokenRequest request) {
        return ApiResponse.success(h5AccessTokenAppService.extend(id, request));
    }

    private UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (UserPrincipal) auth.getPrincipal();
    }
}
