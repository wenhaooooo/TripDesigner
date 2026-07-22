package com.tripdesigner.knowledge.infrastructure.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.knowledge.domain.Country;
import com.tripdesigner.knowledge.domain.repository.CountryRepository;
import com.tripdesigner.knowledge.infrastructure.mapper.CountryMapper;
import com.tripdesigner.knowledge.infrastructure.po.CountryPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 国家仓储实现。
 * 使用 MyBatis Plus 实现持久化，通过 ObjectMapper 在 JSONB 字符串与领域类型间转换。
 */
@Repository
@RequiredArgsConstructor
public class CountryRepositoryImpl implements CountryRepository {

    private final CountryMapper mapper;
    private final ObjectMapper objectMapper;

    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Override
    public Optional<Country> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    @Override
    public Optional<Country> findByIsoCode2(String code) {
        return Optional.ofNullable(mapper.selectOne(
                Wrappers.<CountryPO>lambdaQuery().eq(CountryPO::getIsoCode2, code))).map(this::fromPO);
    }

    @Override
    public List<Country> findByName(String name) {
        return mapper.selectList(
                        Wrappers.<CountryPO>lambdaQuery().like(CountryPO::getName, name))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public List<Country> findAll() {
        return mapper.selectList(null).stream().map(this::fromPO).toList();
    }

    @Override
    public Country save(Country country) {
        CountryPO po = toPO(country);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    @Override
    public Optional<Country> findBySource(String source, String sourceId) {
        return Optional.ofNullable(mapper.selectOne(
                        Wrappers.<CountryPO>lambdaQuery()
                                .eq(CountryPO::getSource, source)
                                .eq(CountryPO::getSourceId, sourceId)))
                .map(this::fromPO);
    }

    private CountryPO toPO(Country c) {
        CountryPO po = new CountryPO();
        po.setId(c.getId());
        po.setName(c.getName());
        po.setIsoCode2(c.getIsoCode2());
        po.setIsoCode3(c.getIsoCode3());
        po.setContinent(c.getContinent());
        po.setCapital(c.getCapital());
        po.setCurrencyCode(c.getCurrencyCode());
        po.setLanguages(serializeList(c.getLanguages()));
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

    private Country fromPO(CountryPO po) {
        if (po == null) return null;
        return Country.builder()
                .id(po.getId())
                .name(po.getName())
                .isoCode2(po.getIsoCode2())
                .isoCode3(po.getIsoCode3())
                .continent(po.getContinent())
                .capital(po.getCapital())
                .currencyCode(po.getCurrencyCode())
                .languages(deserializeList(po.getLanguages()))
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

    private String serializeList(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<String> deserializeList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<String> list = objectMapper.readValue(json, LIST_TYPE);
            return list != null ? list : List.of();
        } catch (JsonProcessingException e) {
            return List.of();
        }
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
