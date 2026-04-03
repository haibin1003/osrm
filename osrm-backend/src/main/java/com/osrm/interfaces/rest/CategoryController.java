package com.osrm.interfaces.rest;

import com.osrm.application.category.dto.request.CreateCategoryRequest;
import com.osrm.application.category.dto.request.UpdateCategoryRequest;
import com.osrm.application.category.dto.response.CategoryDTO;
import com.osrm.application.category.service.CategoryAppService;
import com.osrm.common.model.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    @Autowired
    private CategoryAppService categoryAppService;

    @GetMapping
    @PreAuthorize("hasAuthority('package:read')")
    public ApiResponse<List<CategoryDTO>> getTree() {
        return ApiResponse.success(categoryAppService.getTree());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('package:read')")
    public ApiResponse<CategoryDTO> getById(@PathVariable Long id) {
        return ApiResponse.success(categoryAppService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('package:create')")
    public ApiResponse<CategoryDTO> create(@Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.success(categoryAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('package:update')")
    public ApiResponse<CategoryDTO> update(@PathVariable Long id, @Valid @RequestBody UpdateCategoryRequest request) {
        return ApiResponse.success(categoryAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('package:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryAppService.delete(id);
        return ApiResponse.success();
    }
}
