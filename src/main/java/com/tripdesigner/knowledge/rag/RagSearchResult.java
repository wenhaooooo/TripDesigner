package com.tripdesigner.knowledge.rag;

import java.util.Map;

/**
 * A single retrieval hit produced by the Global Travel Knowledge Base RAG
 * layer.
 *
 * <p>This is a carrier record (immutable, value-based) used uniformly by every
 * {@link RagRetrievalService} implementation so that downstream consumers
 * (prompt builders, REST controllers, evaluators) never need to know whether
 * a result came from vector search, hybrid search, or MMR re-ranking.
 *
 * <p>Field semantics:
 * <ul>
 *   <li>{@code chunkId} — primary key of the {@code knowledge_chunk} row.</li>
 *   <li>{@code entityType} — discriminator such as {@code "ATTRACTION"},
 *       {@code "RESTAURANT"}, {@code "CITY"}, used for filtering and routing.</li>
 *   <li>{@code entityId} — FK to the canonical entity (e.g. attraction id);
 *       nullable when the chunk is free-form content.</li>
 *   <li>{@code title} — short human-readable label, useful for prompt
 *       citations and debug UIs.</li>
 *   <li>{@code content} — the actual text chunk fed to the LLM context.</li>
 *   <li>{@code score} — relevance score in {@code [0,1]}; comparable across
 *       results of the same retrieval call but NOT across strategies.</li>
 *   <li>{@code metadata} — additional structured fields (city_id, country_id,
 *       language, source, ...) used for filtering and audit.</li>
 * </ul>
 *
 * @param chunkId    primary key of the source {@code knowledge_chunk} row
 * @param entityType discriminator (ATTRACTION / RESTAURANT / CITY / ...)
 * @param entityId   optional FK to a canonical entity
 * @param title      short label
 * @param content    the text chunk
 * @param score      relevance score, higher is better
 * @param metadata   additional structured fields (never {@code null})
 */
public record RagSearchResult(
        Long chunkId,
        String entityType,
        Long entityId,
        String title,
        String content,
        double score,
        Map<String, Object> metadata
) {

    /**
     * Static factory that defends against a {@code null} metadata map and
     * produces an immutable snapshot. Prefer this over the canonical
     * constructor to guarantee the non-null contract documented above.
     *
     * @param chunkId    primary key of the source row
     * @param entityType discriminator
     * @param entityId   optional FK
     * @param title      short label
     * @param content    text chunk
     * @param score      relevance score
     * @param metadata   additional fields ({@code null} tolerated)
     * @return an immutable {@link RagSearchResult}
     */
    public static RagSearchResult of(Long chunkId, String entityType, Long entityId,
                                     String title, String content, double score,
                                     Map<String, Object> metadata) {
        return new RagSearchResult(
                chunkId,
                entityType,
                entityId,
                title,
                content,
                score,
                metadata == null ? Map.of() : Map.copyOf(metadata)
        );
    }

    /**
     * Canonical constructor: normalises {@code metadata} to an empty map when
     * callers bypass {@link #of}.
     */
    public RagSearchResult {
        if (metadata == null) {
            metadata = Map.of();
        }
    }
}
