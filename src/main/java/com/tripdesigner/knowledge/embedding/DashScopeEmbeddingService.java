package com.tripdesigner.knowledge.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.knowledge.config.KnowledgeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link EmbeddingService} backed by Alibaba Cloud DashScope's text-embedding
 * HTTP API.
 *
 * <p>Activated only when {@code knowledge.embedding.provider=dashscope} so
 * that the default deployment (which uses Spring AI's OpenAI-compatible
 * client) does not require any DashScope credentials. The implementation is
 * intentionally dependency-free: it posts a small JSON payload to
 * {@code https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding}
 * and parses the response with the shared {@link ObjectMapper}.
 *
 * <p>Configuration:
 * <ul>
 *   <li>{@code knowledge.embedding.dashscope.api-key} — DashScope API key (required when active)</li>
 *   <li>{@code knowledge.embedding.dashscope.model} — model id, default {@code text-embedding-v3}</li>
 *   <li>{@code knowledge.embedding.dashscope.base-url} — override endpoint (e.g. for VPC)</li>
 * </ul>
 *
 * <p>DashScope's {@code text-embedding-v3} produces 1024-dimensional vectors
 * by default; the {@code dimensions} request parameter is forwarded so that
 * the output matches {@link KnowledgeProperties.Embedding#getDimensions()}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "knowledge.embedding", name = "provider", havingValue = "dashscope")
public class DashScopeEmbeddingService implements EmbeddingService {

    private static final String PROVIDER_NAME = "dashscope";
    private static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com";
    private static final String DEFAULT_MODEL = "text-embedding-v3";
    private static final String EMBEDDING_PATH =
            "/api/v1/services/embeddings/text-embedding/text-embedding";

    private final KnowledgeProperties knowledgeProperties;
    private final ObjectMapper objectMapper;

    @Value("${knowledge.embedding.dashscope.api-key:}")
    private String apiKey;

    @Value("${knowledge.embedding.dashscope.model:" + DEFAULT_MODEL + "}")
    private String model;

    @Value("${knowledge.embedding.dashscope.base-url:" + DEFAULT_BASE_URL + "}")
    private String baseUrl;

    private RestClient restClient;

    /**
     * Lazy-init the {@link RestClient} so that the bean can be constructed
     * even before configuration is finalised (e.g. in unit tests).
     */
    private RestClient client() {
        if (restClient == null) {
            restClient = RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();
        }
        return restClient;
    }

    @Override
    public float[] embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        int batchSize = knowledgeProperties.getEmbedding().getBatchSize();
        List<float[]> aggregated = new ArrayList<>(texts.size());
        for (int from = 0; from < texts.size(); from += batchSize) {
            int to = Math.min(from + batchSize, texts.size());
            List<String> slice = texts.subList(from, to);
            aggregated.addAll(callDashScope(slice));
        }
        return aggregated;
    }

    private List<float[]> callDashScope(List<String> texts) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("model", model);
        parameters.put("input", Map.of("texts", texts));
        parameters.put("parameters", Map.of(
                "dimension", knowledgeProperties.getEmbedding().getDimensions(),
                "text_type", "document"
        ));

        try {
            String responseJson = client().post()
                    .uri(EMBEDDING_PATH)
                    .body(parameters)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode embeddings = root.path("output").path("embeddings");
            List<float[]> result = new ArrayList<>(embeddings.size());
            for (JsonNode node : embeddings) {
                JsonNode vector = node.path("embedding");
                float[] vec = new float[vector.size()];
                for (int i = 0; i < vector.size(); i++) {
                    vec[i] = (float) vector.get(i).asDouble();
                }
                result.add(vec);
            }
            if (result.size() != texts.size()) {
                log.warn("[DashScopeEmbedding] Response vector count {} != request count {}",
                        result.size(), texts.size());
            }
            return result;
        } catch (Exception e) {
            log.error("[DashScopeEmbedding] Batch embed failed for {} texts: {}",
                    texts.size(), e.getMessage());
            throw new IllegalStateException("DashScope embedding call failed", e);
        }
    }

    @Override
    public int getDimensions() {
        return knowledgeProperties.getEmbedding().getDimensions();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    /**
     * Exposed for tests / health-checks that want to assert the configured
     * endpoint without going through the network.
     */
    Duration requestTimeoutFallback() {
        return Duration.ofSeconds(30);
    }
}
