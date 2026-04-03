package com.osrm.application.category.service;

import com.osrm.application.category.dto.request.CreateTagRequest;
import com.osrm.application.category.dto.response.TagDTO;
import com.osrm.common.exception.BizException;
import com.osrm.domain.category.entity.Tag;
import com.osrm.domain.category.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagAppServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagAppService tagAppService;

    private Tag mockTag;

    @BeforeEach
    void setUp() {
        mockTag = new Tag();
        mockTag.setId(1L);
        mockTag.setTagName("开源");
        mockTag.setTagCode("open-source");
        mockTag.setDescription("开源软件");
    }

    @Test
    void findAll_shouldReturnAllTags() {
        when(tagRepository.findAll()).thenReturn(List.of(mockTag));
        List<TagDTO> tags = tagAppService.findAll();
        assertEquals(1, tags.size());
    }

    @Test
    void findById_withExistingId_shouldReturn() {
        when(tagRepository.findById(1L)).thenReturn(Optional.of(mockTag));
        TagDTO dto = tagAppService.findById(1L);
        assertEquals("开源", dto.getTagName());
    }

    @Test
    void findById_withNonExistingId_shouldThrow() {
        when(tagRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> tagAppService.findById(999L));
    }

    @Test
    void create_withValidRequest_shouldCreate() {
        CreateTagRequest req = new CreateTagRequest();
        req.setTagName("数据库");
        req.setTagCode("database");

        when(tagRepository.existsByTagCode("database")).thenReturn(false);
        when(tagRepository.existsByTagName("数据库")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> {
            Tag t = inv.getArgument(0);
            t.setId(2L);
            return t;
        });

        TagDTO dto = tagAppService.create(req);
        assertEquals("数据库", dto.getTagName());
    }

    @Test
    void create_withDuplicateCode_shouldThrow() {
        CreateTagRequest req = new CreateTagRequest();
        req.setTagName("新标签");
        req.setTagCode("open-source");

        when(tagRepository.existsByTagCode("open-source")).thenReturn(true);
        assertThrows(BizException.class, () -> tagAppService.create(req));
    }

    @Test
    void create_withDuplicateName_shouldThrow() {
        CreateTagRequest req = new CreateTagRequest();
        req.setTagName("开源");
        req.setTagCode("new-tag");

        when(tagRepository.existsByTagCode("new-tag")).thenReturn(false);
        when(tagRepository.existsByTagName("开源")).thenReturn(true);
        assertThrows(BizException.class, () -> tagAppService.create(req));
    }

    @Test
    void delete_withExistingId_shouldDelete() {
        when(tagRepository.findById(1L)).thenReturn(Optional.of(mockTag));
        tagAppService.delete(1L);
        verify(tagRepository).delete(mockTag);
    }

    @Test
    void delete_withNonExistingId_shouldThrow() {
        when(tagRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> tagAppService.delete(999L));
    }
}
