package com.tripdesigner.knowledge.infrastructure.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.knowledge.domain.City;
import com.tripdesigner.knowledge.domain.repository.CityRepository;
import com.tripdesigner.knowledge.infrastructure.mapper.CityMapper;
import com.tripdesigner.knowledge.infrastructure.po.CityPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 城市仓储实现。
 * 使用 MyBatis Plus 实现持久化，通过 ObjectMapper 在 JSONB 字符串与领域类型间转换。
 */
@Repository
@RequiredArgsConstructor
public class CityRepositoryImpl implements CityRepository {

    private final CityMapper mapper;
    private final ObjectMapper objectMapper;

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Override
    public Optional<City> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    @Override
    public List<City> findByCountryId(Long countryId) {
        return mapper.selectList(
                        Wrappers.<CityPO>lambdaQuery().eq(CityPO::getCountryId, countryId))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public List<City> findByName(String name) {
        return mapper.selectList(
                        Wrappers.<CityPO>lambdaQuery().like(CityPO::getName, name))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public Optional<City> findByNameAndCountryId(String name, Long countryId) {
        return Optional.ofNullable(mapper.selectOne(
                        Wrappers.<CityPO>lambdaQuery()
                                .eq(CityPO::getName, name)
                                .eq(CityPO::getCountryId, countryId)))
                .map(this::fromPO);
    }

    @Override
    public City save(City city) {
        CityPO po = toPO(city);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    @Override
    public Optional<City> findBySource(String source, String sourceId) {
        return Optional.ofNullable(mapper.selectOne(
                        Wrappers.<CityPO>lambdaQuery()
                                .eq(CityPO::getSource, source)
                                .eq(CityPO::getSourceId, sourceId)))
                .map(this::fromPO);
    }

    private CityPO toPO(City c) {
        CityPO po = new CityPO();
        po.setId(c.getId());
        po.setCountryId(c.getCountryId());
        po.setName(c.getName());
        po.setNameLocal(c.getNameLocal());
        po.setTimezone(c.getTimezone());
        po.setPopulation(c.getPopulation());
        po.setLatitude(c.getLatitude());
        po.setLongitude(c.getLongitude());
        po.setMetadata(serializeMap(c.getMetadata()));
        po.setSource(c.getSource());
        po.setSourceId(c.getSourceId());
        po.setContentHash(c.getContentHash());
        po.setLastSyncedAt(c.getLastSyncedAt());
        po.setVersion(c.getVersion());
        return po;
    }

    private City fromPO(CityPO po) {
        if (po == null) return null;
        return City.builder()
                .id(po.getId())
                .countryId(po.getCountryId())
                .name(po.getName())
                .nameLocal(po.getNameLocal())
                .timezone(po.getTimezone())
                .population(po.getPopulation())
                .latitude(po.getLatitude())
                .longitude(po.getLongitude())
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
