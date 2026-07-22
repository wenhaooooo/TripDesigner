package com.tripdesigner.knowledge.embedding;

import java.util.List;

/**
 * Pluggable embedding service abstraction.
 *
 * <p>The Global Travel Knowledge Base module needs to convert text into dense
 * vectors to support semantic retrieval (RAG). Different providers expose
 * different SDKs and REST APIs (Spring AI / OpenAI compatible, Alibaba
 * DashScope, local Ollama, etc.). This interface isolates the rest of the
 * module from any specific vendor so that the active backend can be switched
 * through the {@code knowledge.embedding.provider} configuration property
 * without touching the RAG layer.
 *
 * <p>Implementations MUST be thread-safe. The contract deliberately mirrors
 * the most common subset of Spring AI's {@code EmbeddingModel} so that the
 * default {@link SpringAiEmbeddingService} can delegate without conversion.
 */
public interface EmbeddingService {

    /**
     * Embed a single piece of text into a dense float vector.
     *
     * @param text the text to embed; must not be {@code null}
     * @return the embedding vector of length {@link #getDimensions()}
     */
    float[] embed(String text);

    /**
     * Embed a batch of texts in a single round-trip when the underlying
     * provider supports it. Implementations that do not support native
     * batching MUST fall back to invoking {@link #embed(String)} per item.
     *
     * @param texts the texts to embed; must not be {@code null} or empty
     * @return a list of embedding vectors in the same order as the input
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * @return the dimensionality of the vectors produced by this service.
     *         Must match the database column definition (1536 by default).
     */
    int getDimensions();

    /**
     * @return a stable, lowercase identifier of the active provider
     *         (e.g. {@code "spring-ai"}, {@code "dashscope"}). Used by
     *         {@link EmbeddingServiceFactory} to route requests and by
     *         monitoring/logging to disambiguate providers.
     */
    String getProviderName();
}
