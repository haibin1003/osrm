package com.osrm.interfaces.rest;

import com.osrm.application.subscription.dto.request.CreateSubscriptionRequest;
import com.osrm.application.subscription.dto.response.DownloadTokenDTO;
import com.osrm.application.subscription.dto.response.SubscriptionDTO;
import com.osrm.application.subscription.service.SubscriptionAppService;
import com.osrm.common.model.ApiResponse;
import com.osrm.common.model.PageResult;
import com.osrm.domain.subscription.entity.SubscriptionStatus;
import com.osrm.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionAppService subscriptionAppService;

    @Autowired
    public SubscriptionController(SubscriptionAppService subscriptionAppService) {
        this.subscriptionAppService = subscriptionAppService;
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('subscription:read')")
    public ApiResponse<PageResult<SubscriptionDTO>> mySubscriptions(
            @CurrentUser Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(subscriptionAppService.findByUserId(userId, page, size));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('subscription:create')")
    public ApiResponse<SubscriptionDTO> apply(
            @Valid @RequestBody CreateSubscriptionRequest request,
            @CurrentUser Long userId) {
        return ApiResponse.success(subscriptionAppService.create(request, userId));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('subscription:approve')")
    public ApiResponse<PageResult<SubscriptionDTO>> pendingApproval(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(subscriptionAppService.findByStatus(SubscriptionStatus.PENDING, page, size));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('subscription:approve')")
    public ApiResponse<SubscriptionDTO> approve(
            @PathVariable Long id,
            @CurrentUser Long userId) {
        return ApiResponse.success(subscriptionAppService.approve(id, userId));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('subscription:approve')")
    public ApiResponse<SubscriptionDTO> reject(
            @PathVariable Long id,
            @CurrentUser Long userId,
            @RequestParam(required = false, defaultValue = "") String reason) {
        return ApiResponse.success(subscriptionAppService.reject(id, userId, reason));
    }

    @GetMapping("/{id}/token")
    @PreAuthorize("hasAuthority('subscription:read')")
    public ApiResponse<DownloadTokenDTO> getToken(@PathVariable Long id) {
        return ApiResponse.success(subscriptionAppService.getToken(id));
    }
}
