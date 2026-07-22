package com.tripdesigner.knowledge.infrastructure.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.knowledge.domain.KnowledgeChunk;
import com.tripdesigner.knowledge.domain.repository.KnowledgeChunkRepository;
import com.tripdesigner.knowledge.infrastructure.mapper.KnowledgeChunkMapper;
import com.tripdesigner.knowledge.infrastructure.po.KnowledgeChunkPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 知识分块仓储实现。
 * 使用 MyBatis Plus 实现标准持久化，通过 ObjectMapper 在 JSONB 字符串与领域类型间转换。
 * 向量检索通过 KnowledgeChunkMapper 的自定义 SQL 调用 pgvector 余弦距离算子（&lt;=&gt;）。
 */
@Repository
@RequiredArgsConstructor
public class KnowledgeChunkRepositoryImpl implements KnowledgeChunkRepository {

    private final KnowledgeChunkMapper mapper;
    private final ObjectMapper objectMapper;

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Override
    public Optional<KnowledgeChunk> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    @Override
    public List<KnowledgeChunk> findByEntityTypeAndEntityId(String entityType, Long entityId) {
        return mapper.selectList(
                        Wrappers.<KnowledgeChunkPO>lambdaQuery()
                                .eq(KnowledgeChunkPO::getEntityType, entityType)
                                .eq(KnowledgeChunkPO::getEntityId, entityId)
                                .orderByAsc(KnowledgeChunkPO::getChunkIndex))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public KnowledgeChunk save(KnowledgeChunk chunk) {
        KnowledgeChunkPO po = toPO(chunk);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    @Override
    public List<KnowledgeChunk> saveAll(List<KnowledgeChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) return List.of();
        List<KnowledgeChunk> saved = new java.util.ArrayList<>(chunks.size());
        for (KnowledgeChunk chunk : chunks) {
            KnowledgeChunkPO po = toPO(chunk);
            if (po.getId() == null) {
                mapper.insert(po);
            } else {
                mapper.updateById(po);
            }
            saved.add(fromPO(po));
        }
        return saved;
    }

    @Override
    public List<KnowledgeChunk> vectorSearch(float[] queryVector, String entityType, int topK) {
        String pgVector = toPgVectorString(queryVector);
        return mapper.vectorSearch(pgVector, entityType, topK)
                .stream().map(this::fromPO).toList();
    }

    @Override
    public List<KnowledgeChunk> vectorSearchAll(float[] queryVector, int topK) {
        String pgVector = toPgVectorString(queryVector);
        return mapper.vectorSearchAll(pgVector, topK)
                .stream().map(this::fromPO).toList();
    }

    @Override
    public void deleteByEntity(String entityType, Long entityId) {
        mapper.delete(Wrappers.<KnowledgeChunkPO>lambdaQuery()
                .eq(KnowledgeChunkPO::getEntityType, entityType)
                .eq(KnowledgeChunkPO::getEntityId, entityId));
    }

    /**
     * 将 float[] 转换为 pgvector 字符串格式，如 "[0.1,0.2,0.3]"。
     */
    private String toPgVectorString(float[] vector) {
        if (vector == null || vector.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private KnowledgeChunkPO toPO(KnowledgeChunk c) {
        KnowledgeChunkPO po = new KnowledgeChunkPO();
        po.setId(c.getId());
        po.setEntityType(c.getEntityType());
        po.setEntityId(c.getEntityId());
        po.setChunkType(c.getChunkType());
        po.setChunkIndex(c.getChunkIndex());
        po.setTitle(c.getTitle());
        po.setContent(c.getContent());
        po.setContentHash(c.getContentHash());
        po.setMetadata(serializeMap(c.getMetadata()));
        po.setLanguage(c.getLanguage());
        po.setTokenCount(c.getTokenCount());
        po.setSource(c.getSource());
        po.setSourceId(c.getSourceId());
        po.setVersion(c.getVersion());
        return po;
    }

    private KnowledgeChunk fromPO(KnowledgeChunkPO po) {
        if (po == null) return null;
        return KnowledgeChunk.builder()
                .id(po.getId())
                .entityType(po.getEntityType())
                .entityId(po.getEntityId())
                .chunkType(po.getChunkType())
                .chunkIndex(po.getChunkIndex())
                .title(po.getTitle())
                .content(po.getContent())
                .contentHash(po.getContentHash())
                .metadata(deserializeMap(po.getMetadata()))
                .language(po.getLanguage())
                .tokenCount(po.getTokenCount())
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
