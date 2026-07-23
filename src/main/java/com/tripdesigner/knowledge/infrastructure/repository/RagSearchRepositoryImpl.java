package com.tripdesigner.knowledge.infrastructure.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.knowledge.infrastructure.mapper.KnowledgeChunkMapper;
import com.tripdesigner.knowledge.infrastructure.po.KnowledgeChunkPO;
import com.tripdesigner.knowledge.rag.RagSearchRepository;
import com.tripdesigner.knowledge.rag.RagSearchResult;
import com.tripdesigner.knowledge.rag.SearchFilters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RagSearchRepositoryImpl implements RagSearchRepository {

    private final KnowledgeChunkMapper mapper;
    private final ObjectMapper objectMapper;

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Override
    public List<RagSearchResult> vectorSearch(float[] embedding, SearchFilters filters) {
        String pgVector = toPgVectorString(embedding);
        List<KnowledgeChunkPO> results = mapper.vectorSearchWithFilters(pgVector, filters);
        List<RagSearchResult> ragResults = new ArrayList<>();
        for (KnowledgeChunkPO po : results) {
            double score = computeScore(po.getDistance());
            if (score >= filters.similarityThreshold()) {
                ragResults.add(toRagSearchResult(po, score));
            }
            if (ragResults.size() >= filters.topK()) {
                break;
            }
        }
        return ragResults;
    }

    @Override
    public List<RagSearchResult> keywordSearch(String query, SearchFilters filters) {
        List<KnowledgeChunkPO> results = mapper.keywordSearch(query, filters);
        List<RagSearchResult> ragResults = new ArrayList<>();
        for (KnowledgeChunkPO po : results) {
            ragResults.add(toRagSearchResult(po, computeScore(po.getDistance())));
            if (ragResults.size() >= filters.topK()) {
                break;
            }
        }
        return ragResults;
    }

    @Override
    public List<RagSearchResult> metadataSearch(SearchFilters filters) {
        List<KnowledgeChunkPO> results = mapper.metadataSearch(filters);
        List<RagSearchResult> ragResults = new ArrayList<>();
        for (KnowledgeChunkPO po : results) {
            ragResults.add(toRagSearchResult(po, 1.0));
            if (ragResults.size() >= filters.topK()) {
                break;
            }
        }
        return ragResults;
    }

    @Override
    public Map<Long, float[]> findEmbeddingsByIds(List<Long> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return Map.of();
        }
        List<Map<String, Object>> results = mapper.findEmbeddingsByIds(chunkIds);
        Map<Long, float[]> embeddings = new LinkedHashMap<>();
        for (Map<String, Object> row : results) {
            Long id = ((Number) row.get("id")).longValue();
            String embeddingStr = (String) row.get("embedding");
            float[] embedding = parsePgVector(embeddingStr);
            embeddings.put(id, embedding);
        }
        return embeddings;
    }

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

    private float[] parsePgVector(String vectorStr) {
        if (vectorStr == null || vectorStr.isEmpty() || vectorStr.equals("[]")) {
            return new float[0];
        }
        String content = vectorStr.substring(1, vectorStr.length() - 1);
        String[] parts = content.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

    private double computeScore(Double distance) {
        if (distance == null) return 0.0;
        double score = 1.0 - distance;
        return Math.max(0.0, Math.min(1.0, score));
    }

    private RagSearchResult toRagSearchResult(KnowledgeChunkPO po, double score) {
        return RagSearchResult.of(
                po.getId(),
                po.getEntityType(),
                po.getEntityId(),
                po.getTitle(),
                po.getContent(),
                score,
                deserializeMap(po.getMetadata())
        );
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
