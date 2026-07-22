package com.tripdesigner.knowledge.embedding;

import com.tripdesigner.knowledge.config.KnowledgeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default {@link EmbeddingService} backed by Spring AI's auto-configured
 * {@link EmbeddingModel}.
 *
 * <p>The {@link EmbeddingModel} bean is created by
 * {@code spring-ai-starter-model-openai} based on the
 * {@code spring.ai.openai.embedding.*} properties in {@code application.yml}.
 * Because this implementation is annotated {@link Primary @Primary}, it is the
 * fallback when the {@code knowledge.embedding.provider} property is unset or
 * equals {@code "spring-ai"}.
 *
 * <p>Vector dimension defaults to {@code 1536} (OpenAI
 * {@code text-embedding-3-small}) and can be overridden through
 * {@code knowledge.embedding.dimensions}. The value MUST match the
 * {@code pgvector} column width configured in
 * {@code spring.ai.vectorstore.pgvector.dimensions}.
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class SpringAiEmbeddingService implements EmbeddingService {

    private static final String PROVIDER_NAME = "spring-ai";

    private final EmbeddingModel embeddingModel;
    private final KnowledgeProperties knowledgeProperties;

    @Override
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        return embeddingModel.embed(texts);
    }

    /**
     * Convenience overload that preserves the {@link Document} metadata for
     * callers that already operate on Spring AI documents. Not part of the
     * {@link EmbeddingService} contract.
     */
    public float[] embed(Document document) {
        return embeddingModel.embed(document);
    }

    @Override
    public int getDimensions() {
        return knowledgeProperties.getEmbedding().getDimensions();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
