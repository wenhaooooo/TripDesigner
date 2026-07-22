package com.tripdesigner.knowledge.infrastructure.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.knowledge.domain.KnowledgeRelation;
import com.tripdesigner.knowledge.domain.repository.KnowledgeRelationRepository;
import com.tripdesigner.knowledge.infrastructure.mapper.KnowledgeRelationMapper;
import com.tripdesigner.knowledge.infrastructure.po.KnowledgeRelationPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 知识关系仓储实现。
 * 使用 MyBatis Plus 实现持久化，通过 ObjectMapper 在 JSONB 字符串与领域类型间转换。
 */
@Repository
@RequiredArgsConstructor
public class KnowledgeRelationRepositoryImpl implements KnowledgeRelationRepository {

    private final KnowledgeRelationMapper mapper;
    private final ObjectMapper objectMapper;

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Override
    public List<KnowledgeRelation> findByFromEntity(String fromEntityType, Long fromEntityId) {
        return mapper.selectList(
                        Wrappers.<KnowledgeRelationPO>lambdaQuery()
                                .eq(KnowledgeRelationPO::getFromEntityType, fromEntityType)
                                .eq(KnowledgeRelationPO::getFromEntityId, fromEntityId))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public List<KnowledgeRelation> findByToEntity(String toEntityType, Long toEntityId) {
        return mapper.selectList(
                        Wrappers.<KnowledgeRelationPO>lambdaQuery()
                                .eq(KnowledgeRelationPO::getToEntityType, toEntityType)
                                .eq(KnowledgeRelationPO::getToEntityId, toEntityId))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public List<KnowledgeRelation> findByRelationType(String relationType) {
        return mapper.selectList(
                        Wrappers.<KnowledgeRelationPO>lambdaQuery()
                                .eq(KnowledgeRelationPO::getRelationType, relationType))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public KnowledgeRelation save(KnowledgeRelation relation) {
        KnowledgeRelationPO po = toPO(relation);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    private KnowledgeRelationPO toPO(KnowledgeRelation r) {
        KnowledgeRelationPO po = new KnowledgeRelationPO();
        po.setId(r.getId());
        po.setFromEntityType(r.getFromEntityType());
        po.setFromEntityId(r.getFromEntityId());
        po.setToEntityType(r.getToEntityType());
        po.setToEntityId(r.getToEntityId());
        po.setRelationType(r.getRelationType());
        po.setWeight(r.getWeight());
        po.setMetadata(serializeMap(r.getMetadata()));
        po.setSource(r.getSource());
        po.setVersion(r.getVersion());
        return po;
    }

    private KnowledgeRelation fromPO(KnowledgeRelationPO po) {
        if (po == null) return null;
        return KnowledgeRelation.builder()
                .id(po.getId())
                .fromEntityType(po.getFromEntityType())
                .fromEntityId(po.getFromEntityId())
                .toEntityType(po.getToEntityType())
                .toEntityId(po.getToEntityId())
                .relationType(po.getRelationType())
                .weight(po.getWeight())
                .metadata(deserializeMap(po.getMetadata()))
                .source(po.getSource())
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
