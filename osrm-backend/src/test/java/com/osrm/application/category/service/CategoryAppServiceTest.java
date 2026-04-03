package com.osrm.application.category.service;

import com.osrm.application.category.dto.request.CreateCategoryRequest;
import com.osrm.application.category.dto.request.UpdateCategoryRequest;
import com.osrm.application.category.dto.response.CategoryDTO;
import com.osrm.common.exception.BizException;
import com.osrm.domain.category.entity.Category;
import com.osrm.domain.category.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryAppServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryAppService categoryAppService;

    private Category mockRoot;

    @BeforeEach
    void setUp() {
        mockRoot = new Category();
        mockRoot.setId(1L);
        mockRoot.setCategoryName("编程语言");
        mockRoot.setCategoryCode("languages");
        mockRoot.setSortOrder(1);
        mockRoot.setChildren(new ArrayList<>());
    }

    @Test
    void getTree_shouldReturnRootCategories() {
        when(categoryRepository.findByParentIsNullOrderBySortOrderAsc()).thenReturn(List.of(mockRoot));
        List<CategoryDTO> tree = categoryAppService.getTree();
        assertEquals(1, tree.size());
        assertEquals("languages", tree.get(0).getCategoryCode());
    }

    @Test
    void findById_withExistingId_shouldReturn() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockRoot));
        CategoryDTO dto = categoryAppService.findById(1L);
        assertEquals("编程语言", dto.getCategoryName());
    }

    @Test
    void findById_withNonExistingId_shouldThrow() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> categoryAppService.findById(999L));
    }

    @Test
    void create_withValidRequest_shouldCreate() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setCategoryName("Java");
        req.setCategoryCode("java");

        when(categoryRepository.existsByCategoryCode("java")).thenReturn(false);
        when(categoryRepository.findMaxSortOrderForRoot()).thenReturn(1);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(2L);
            return c;
        });

        CategoryDTO dto = categoryAppService.create(req);
        assertEquals("Java", dto.getCategoryName());
    }

    @Test
    void create_withDuplicateCode_shouldThrow() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setCategoryName("Java");
        req.setCategoryCode("languages");

        when(categoryRepository.existsByCategoryCode("languages")).thenReturn(true);
        assertThrows(BizException.class, () -> categoryAppService.create(req));
    }

    @Test
    void create_withParent_shouldCreateChild() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setCategoryName("Java");
        req.setCategoryCode("java");
        req.setParentId(1L);

        when(categoryRepository.existsByCategoryCode("java")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockRoot));
        when(categoryRepository.findMaxSortOrderByParentId(1L)).thenReturn(0);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(2L);
            return c;
        });

        CategoryDTO dto = categoryAppService.create(req);
        assertEquals(1L, dto.getParentId());
    }

    @Test
    void create_withNonExistingParent_shouldThrow() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setCategoryName("Java");
        req.setCategoryCode("java");
        req.setParentId(999L);

        when(categoryRepository.existsByCategoryCode("java")).thenReturn(false);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> categoryAppService.create(req));
    }

    @Test
    void update_shouldUpdateCategory() {
        UpdateCategoryRequest req = new UpdateCategoryRequest();
        req.setCategoryName("编程语言与框架");
        req.setDescription("更新描述");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockRoot));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryDTO dto = categoryAppService.update(1L, req);
        assertEquals("编程语言与框架", dto.getCategoryName());
    }

    @Test
    void update_withSelfParent_shouldThrow() {
        UpdateCategoryRequest req = new UpdateCategoryRequest();
        req.setCategoryName("名称");
        req.setParentId(1L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockRoot));
        assertThrows(BizException.class, () -> categoryAppService.update(1L, req));
    }

    @Test
    void delete_withNoChildren_shouldDelete() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockRoot));
        categoryAppService.delete(1L);
        verify(categoryRepository).delete(mockRoot);
    }

    @Test
    void delete_withChildren_shouldThrow() {
        Category child = new Category();
        child.setId(2L);
        mockRoot.setChildren(List.of(child));

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockRoot));
        assertThrows(BizException.class, () -> categoryAppService.delete(1L));
    }

    @Test
    void delete_withNonExistingId_shouldThrow() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> categoryAppService.delete(999L));
    }
}
