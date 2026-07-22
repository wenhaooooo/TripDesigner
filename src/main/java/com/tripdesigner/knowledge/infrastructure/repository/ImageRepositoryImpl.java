package com.tripdesigner.knowledge.infrastructure.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.knowledge.domain.Image;
import com.tripdesigner.knowledge.domain.repository.ImageRepository;
import com.tripdesigner.knowledge.infrastructure.mapper.ImageMapper;
import com.tripdesigner.knowledge.infrastructure.po.ImagePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 图片仓储实现。
 * 使用 MyBatis Plus 实现持久化，通过 ObjectMapper 在 JSONB 字符串与领域类型间转换。
 */
@Repository
@RequiredArgsConstructor
public class ImageRepositoryImpl implements ImageRepository {

    private final ImageMapper mapper;
    private final ObjectMapper objectMapper;

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Override
    public List<Image> findByEntityTypeAndEntityId(String entityType, Long entityId) {
        return mapper.selectList(
                        Wrappers.<ImagePO>lambdaQuery()
                                .eq(ImagePO::getEntityType, entityType)
                                .eq(ImagePO::getEntityId, entityId))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public Image save(Image image) {
        ImagePO po = toPO(image);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    private ImagePO toPO(Image img) {
        ImagePO po = new ImagePO();
        po.setId(img.getId());
        po.setEntityType(img.getEntityType());
        po.setEntityId(img.getEntityId());
        po.setUrl(img.getUrl());
        po.setUrlThumb(img.getUrlThumb());
        po.setCaption(img.getCaption());
        po.setAltText(img.getAltText());
        po.setWidth(img.getWidth());
        po.setHeight(img.getHeight());
        po.setMetadata(serializeMap(img.getMetadata()));
        po.setSource(img.getSource());
        po.setSourceId(img.getSourceId());
        po.setVersion(img.getVersion());
        return po;
    }

    private Image fromPO(ImagePO po) {
        if (po == null) return null;
        return Image.builder()
                .id(po.getId())
                .entityType(po.getEntityType())
                .entityId(po.getEntityId())
                .url(po.getUrl())
                .urlThumb(po.getUrlThumb())
                .caption(po.getCaption())
                .altText(po.getAltText())
                .width(po.getWidth())
                .height(po.getHeight())
                .metadata(deserializeMap(po.getMetadata()))
                .source(po.getSource())
                .sourceId(po.getSourceId())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .version(po.getVersion())
                .build();
    }

    private String serializeMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Map<String, Object> deserializeMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            Map<String, Object> map = objectMapper.readValue(json, MAP_TYPE);
            return map != null ? map : Map.of();
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}
