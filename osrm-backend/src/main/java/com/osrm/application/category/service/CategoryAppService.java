package com.osrm.application.category.service;

import com.osrm.application.category.dto.request.CreateCategoryRequest;
import com.osrm.application.category.dto.request.UpdateCategoryRequest;
import com.osrm.application.category.dto.response.CategoryDTO;
import com.osrm.common.exception.BizException;
import com.osrm.domain.category.entity.Category;
import com.osrm.domain.category.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CategoryAppService {

    private final CategoryRepository categoryRepository;

    @Autowired
    public CategoryAppService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryDTO> getTree() {
        List<Category> roots = categoryRepository.findByParentIsNullOrderBySortOrderAsc();
        return roots.stream().map(CategoryDTO::from).collect(Collectors.toList());
    }

    public CategoryDTO findById(Long id) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new BizException("分类不存在"));
        return CategoryDTO.from(cat);
    }

    @Transactional
    public CategoryDTO create(CreateCategoryRequest request) {
        if (categoryRepository.existsByCategoryCode(request.getCategoryCode())) {
            throw new BizException("分类编码已存在");
        }

        Category cat = new Category();
        cat.setCategoryName(request.getCategoryName());
        cat.setCategoryCode(request.getCategoryCode());
        cat.setDescription(request.getDescription());

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BizException("父分类不存在"));
            cat.setParent(parent);
            int maxOrder = categoryRepository.findMaxSortOrderByParentId(request.getParentId());
            cat.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : maxOrder + 1);
        } else {
            int maxOrder = categoryRepository.findMaxSortOrderForRoot();
            cat.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : maxOrder + 1);
        }

        Category saved = categoryRepository.save(cat);
        return CategoryDTO.from(saved);
    }

    @Transactional
    public CategoryDTO update(Long id, UpdateCategoryRequest request) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new BizException("分类不存在"));

        cat.setCategoryName(request.getCategoryName());
        cat.setDescription(request.getDescription());

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BizException("分类不能设置自己为父分类");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BizException("父分类不存在"));
            cat.setParent(parent);
        } else {
            cat.setParent(null);
        }

        if (request.getSortOrder() != null) {
            cat.setSortOrder(request.getSortOrder());
        }

        Category saved = categoryRepository.save(cat);
        return CategoryDTO.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new BizException("分类不存在"));

        if (!cat.getChildren().isEmpty()) {
            throw new BizException("该分类下有子分类，不能删除");
        }

        categoryRepository.delete(cat);
    }
}
