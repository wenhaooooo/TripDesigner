package com.tripdesigner.knowledge.infrastructure.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.knowledge.domain.Poi;
import com.tripdesigner.knowledge.domain.repository.PoiRepository;
import com.tripdesigner.knowledge.infrastructure.mapper.PoiMapper;
import com.tripdesigner.knowledge.infrastructure.po.PoiPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 景点（POI）仓储实现。
 * 使用 MyBatis Plus 实现持久化，通过 ObjectMapper 在 JSONB 字符串与领域类型间转换。
 */
@Repository
@RequiredArgsConstructor
public class PoiRepositoryImpl implements PoiRepository {

    private final PoiMapper mapper;
    private final ObjectMapper objectMapper;

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Override
    public Optional<Poi> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    @Override
    public List<Poi> findByCityId(Long cityId) {
        return mapper.selectList(
                        Wrappers.<PoiPO>lambdaQuery().eq(PoiPO::getCityId, cityId))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public List<Poi> findByCategory(String category) {
        return mapper.selectList(
                        Wrappers.<PoiPO>lambdaQuery().eq(PoiPO::getCategory, category))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public Poi save(Poi poi) {
        PoiPO po = toPO(poi);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    @Override
    public Optional<Poi> findBySource(String source, String sourceId) {
        return Optional.ofNullable(mapper.selectOne(
                        Wrappers.<PoiPO>lambdaQuery()
                                .eq(PoiPO::getSource, source)
                                .eq(PoiPO::getSourceId, sourceId)))
                .map(this::fromPO);
    }

    private PoiPO toPO(Poi p) {
        PoiPO po = new PoiPO();
        po.setId(p.getId());
        po.setCityId(p.getCityId());
        po.setName(p.getName());
        po.setNameLocal(p.getNameLocal());
        po.setCategory(p.getCategory());
        po.setSubcategory(p.getSubcategory());
        po.setDescription(p.getDescription());
        po.setLatitude(p.getLatitude());
        po.setLongitude(p.getLongitude());
        po.setAddress(p.getAddress());
        po.setOpeningHours(p.getOpeningHours());
        po.setPriceInfo(p.getPriceInfo());
        po.setContactInfo(p.getContactInfo());
        po.setRating(p.getRating());
        po.setReviewCount(p.getReviewCount());
        po.setMetadata(serializeMap(p.getMetadata()));
        po.setSource(p.getSource());
        po.setSourceId(p.getSourceId());
        po.setContentHash(p.getContentHash());
        po.setLastSyncedAt(p.getLastSyncedAt());
        po.setVersion(p.getVersion());
        return po;
    }

    private Poi fromPO(PoiPO po) {
        if (po == null) return null;
        return Poi.builder()
                .id(po.getId())
                .cityId(po.getCityId())
                .name(po.getName())
                .nameLocal(po.getNameLocal())
                .category(po.getCategory())
                .subcategory(po.getSubcategory())
                .description(po.getDescription())
                .latitude(po.getLatitude())
                .longitude(po.getLongitude())
                .address(po.getAddress())
                .openingHours(po.getOpeningHours())
                .priceInfo(po.getPriceInfo())
                .contactInfo(po.getContactInfo())
                .rating(po.getRating())
                .reviewCount(po.getReviewCount())
                .metadata(deserializeMap(po.getMetadata()))
                .source(po.getSource())
                .sourceId(po.getSourceId())
                .contentHash(po.getContentHash())
                .lastSyncedAt(po.getLastSyncedAt())
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
