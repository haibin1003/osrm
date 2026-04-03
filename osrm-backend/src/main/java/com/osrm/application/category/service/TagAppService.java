package com.osrm.application.category.service;

import com.osrm.application.category.dto.request.CreateTagRequest;
import com.osrm.application.category.dto.response.TagDTO;
import com.osrm.common.exception.BizException;
import com.osrm.domain.category.entity.Tag;
import com.osrm.domain.category.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TagAppService {

    private final TagRepository tagRepository;

    @Autowired
    public TagAppService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<TagDTO> findAll() {
        return tagRepository.findAll().stream().map(TagDTO::from).collect(Collectors.toList());
    }

    public TagDTO findById(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new BizException("标签不存在"));
        return TagDTO.from(tag);
    }

    @Transactional
    public TagDTO create(CreateTagRequest request) {
        if (tagRepository.existsByTagCode(request.getTagCode())) {
            throw new BizException("标签编码已存在");
        }
        if (tagRepository.existsByTagName(request.getTagName())) {
            throw new BizException("标签名称已存在");
        }

        Tag tag = new Tag();
        tag.setTagName(request.getTagName());
        tag.setTagCode(request.getTagCode());
        tag.setDescription(request.getDescription());

        Tag saved = tagRepository.save(tag);
        return TagDTO.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new BizException("标签不存在"));
        tagRepository.delete(tag);
    }
}
