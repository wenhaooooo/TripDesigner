package com.tripdesigner.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Global Travel Knowledge Base module,
 * bound from the {@code knowledge.*} namespace in {@code application.yml}.
 *
 * <p>Grouped into four concerns:
 * <ul>
 *   <li>{@link Embedding} — pluggable embedding provider, vector dimensions
 *       and batch size.</li>
 *   <li>{@link Rag} — retrieval tuning: top-K, similarity threshold, MMR
 *       lambda, hybrid alpha, cache TTL.</li>
 *   <li>{@link Crawler} — knowledge-source crawler switches, rate-limit and
 *       retry policy.</li>
 *   <li>{@link Scheduler} — incremental / full re-index scheduler.</li>
 * </ul>
 *
 * <p>The bean is registered explicitly by {@link KnowledgeConfig} via
 * {@code @Bean @ConfigurationProperties} so this class itself does not carry
 * {@code @Component}; this matches the project convention used by
 * {@code MultimodalProperties} / {@code WorkflowProperties} of co-locating
 * the {@code @ConfigurationProperties} annotation with the binding target.
 *
 * <p>Example YAML:
 * <pre>{@code
 * knowledge:
 *   embedding:
 *     provider: spring-ai
 *     dimensions: 1536
 *     batch-size: 100
 *   rag:
 *     top-k: 5
 *     similarity-threshold: 0.5
 *     mmr-lambda: 0.5
 *     hybrid-alpha: 0.7
 *     cache-ttl-minutes: 60
 *   crawler:
 *     enabled: true
 *     rate-limit-ms: 1000
 *     retry-count: 3
 *     retry-delay-ms: 5000
 *   scheduler:
 *     enabled: true
 *     cron: "0 0 2 * * ?"
 *     incremental: true
 * }</pre>
 */
@Data
@ConfigurationProperties(prefix = "knowledge")
public class KnowledgeProperties {

    private Embedding embedding = new Embedding();
    private Rag rag = new Rag();
    private Crawler crawler = new Crawler();
    private Scheduler scheduler = new Scheduler();

    /**
     * Embedding provider configuration.
     *
     * <p>{@code dimensions} MUST match the {@code pgvector} column width
     * configured under {@code spring.ai.vectorstore.pgvector.dimensions} and
     * the {@code knowledge_chunk.embedding} column DDL. Default
     * {@code 1536} matches OpenAI {@code text-embedding-3-small}.
     */
    @Data
    public static class Embedding {
        /** Active provider id: {@code "spring-ai"} (default) or {@code "dashscope"}. */
        private String provider = "spring-ai";
        /** Embedding vector dimensionality; must match the DB column. */
        private int dimensions = 1536;
        /** Max texts per batch call to the embedding API. */
        private int batchSize = 100;
    }

    /**
     * Retrieval (RAG) tuning parameters. These are used as defaults when a
     * caller constructs a {@link com.tripdesigner.knowledge.rag.SearchFilters}
     * without explicit values.
     */
    @Data
    public static class Rag {
        /** Default number of results returned by a single retrieval call. */
        private int topK = 5;
        /** Minimum cosine similarity in {@code [0,1]}; results below are dropped. */
        private double similarityThreshold = 0.5;
        /** MMR trade-off: 1.0 = pure relevance, 0.0 = pure diversity. */
        private double mmrLambda = 0.5;
        /** Hybrid search weight: {@code finalScore = alpha * vector + (1-alpha) * keyword}. */
        private double hybridAlpha = 0.7;
        /** TTL of Redis RAG cache entries, in minutes. */
        private int cacheTtlMinutes = 60;
    }

    /**
     * Knowledge-source crawler configuration. The crawlers (Mafengwo, Ctrip,
     * Xiaohongshu, ...) populate the {@code knowledge_chunk} table.
     */
    @Data
    public static class Crawler {
        /** Master switch; when false, no crawling runs are scheduled. */
        private boolean enabled = true;
        /** Minimum delay between two HTTP requests to the same host, in millis. */
        private long rateLimitMs = 1000L;
        /** Number of retry attempts for a failed fetch. */
        private int retryCount = 3;
        /** Delay between retry attempts, in millis. */
        private long retryDelayMs = 5000L;
    }

    /**
     * Re-index scheduler configuration.
     */
    @Data
    public static class Scheduler {
        /** Master switch for the scheduled re-index job. */
        private boolean enabled = true;
        /** Quartz-style cron expression; default runs daily at 02:00. */
        private String cron = "0 0 2 * * ?";
        /** When true, only chunks updated since the last run are re-embedded. */
        private boolean incremental = true;
    }
}
