package com.osrm.interfaces.rest;

import com.osrm.application.category.dto.request.CreateTagRequest;
import com.osrm.application.category.dto.response.TagDTO;
import com.osrm.application.category.service.TagAppService;
import com.osrm.common.model.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    @Autowired
    private TagAppService tagAppService;

    @GetMapping
    @PreAuthorize("hasAuthority('package:read')")
    public ApiResponse<List<TagDTO>> findAll() {
        return ApiResponse.success(tagAppService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('package:read')")
    public ApiResponse<TagDTO> getById(@PathVariable Long id) {
        return ApiResponse.success(tagAppService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('package:create')")
    public ApiResponse<TagDTO> create(@Valid @RequestBody CreateTagRequest request) {
        return ApiResponse.success(tagAppService.create(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('package:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tagAppService.delete(id);
        return ApiResponse.success();
    }
}
