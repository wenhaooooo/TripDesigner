package com.tripdesigner.knowledge.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.knowledge.config.KnowledgeProperties;
import com.tripdesigner.knowledge.embedding.EmbeddingServiceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Primary {@link RagRetrievalService} backed by pgvector cosine similarity
 * search.
 *
 * <h2>Retrieval strategy</h2>
 * <p>This service is the default ({@link Primary}) implementation of the RAG
 * retrieval contract. All three methods share a common pipeline:
 * <ol>
 *   <li>Check the Redis cache for a previous result with the same
 *       {@code (method, query, filters)} signature.</li>
 *   <li>Embed the query using the active {@link EmbeddingServiceFactory}
 *       provider.</li>
 *   <li>Delegate to {@link RagSearchRepository} for the actual SQL /
 *       pgvector work.</li>
 *   <li>Write the result back to the cache with a 1-hour TTL.</li>
 * </ol>
 *
 * <h3>search()</h3>
 * Pure semantic retrieval. The query embedding is compared against the
 * {@code embedding} column of {@code knowledge_chunk} using cosine distance,
 * filtered by the metadata predicates in {@link SearchFilters}, and truncated
 * to {@code topK}. This is the right default when the user's query is
 * well-formed and self-contained.
 *
 * <h3>hybridSearch()</h3>
 * Combines semantic vector search with a pure metadata predicate search
 * (executed via MyBatis on indexed columns). The two result sets are merged
 * by {@code chunkId} (keeping the higher score for duplicates) and re-ranked
 * by descending score. This recovers chunks that are metadata-relevant but
 * whose text does not embed similarly to the query — useful for navigational
 * queries like "things in Tokyo".
 *
 * <h3>mmrSearch() — Maximal Marginal Relevance</h3>
 * Pure vector retrieval tends to return near-duplicate chunks (e.g. five
 * paraphrases of the same attraction description), which wastes the LLM
 * context window. MMR addresses this by trading off relevance against
 * diversity:
 * <pre>
 *   mmr(d) = λ · sim(query, d) − (1 − λ) · max_{s ∈ selected} sim(d, s)
 * </pre>
 * At each step we pick the candidate that maximises this score, so
 * {@code λ=1} degenerates to pure relevance and {@code λ=0} to pure
 * diversity. The default {@code λ=0.5} works well for travel knowledge
 * where we want a mix of the most relevant facts plus some variety
 * (different attractions, different categories) in the prompt context.
 *
 * <p>Implementation: retrieve {@code 2 * topK} candidates via vector search,
 * fetch their stored embeddings through
 * {@link RagSearchRepository#findEmbeddingsByIds(List)}, then greedily
 * select {@code topK} items using the formula above.
 *
 * <h2>Caching</h2>
 * <p>Every method caches its result in Redis under
 * {@code kb:rag:search:<method>:<sha256(query+filters)>} with a TTL of
 * {@code knowledge.rag.cache-ttl-minutes} (default 60 min). Cache read/write
 * failures are swallowed (logged at DEBUG) so that a Redis outage never
 * breaks retrieval.
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class VectorRagService implements RagRetrievalService {

    private static final String CACHE_PREFIX = "kb:rag:search:";

    private final RagSearchRepository ragSearchRepository;
    private final EmbeddingServiceFactory embeddingServiceFactory;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final KnowledgeProperties knowledgeProperties;

    @Override
    public List<RagSearchResult> search(String query, SearchFilters filters) {
        SearchFilters effective = effectiveFilters(filters);
        String cacheKey = buildCacheKey("search", query, effective);
        List<RagSearchResult> cached = readCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            float[] embedding = embeddingServiceFactory.getEmbeddingService().embed(query);
            List<RagSearchResult> results = ragSearchRepository.vectorSearch(embedding, effective);
            log.debug("[VectorRag] search '{}' → {} results (provider={})",
                    truncate(query), results.size(),
                    embeddingServiceFactory.getEmbeddingService().getProviderName());
            writeCache(cacheKey, results);
            return results;
        } catch (Exception e) {
            log.warn("[VectorRag] search failed for '{}': {}", truncate(query), e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<RagSearchResult> hybridSearch(String query, SearchFilters filters) {
        SearchFilters effective = effectiveFilters(filters);
        String cacheKey = buildCacheKey("hybrid", query, effective);
        List<RagSearchResult> cached = readCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            float[] embedding = embeddingServiceFactory.getEmbeddingService().embed(query);

            List<RagSearchResult> vectorResults =
                    ragSearchRepository.vectorSearch(embedding, effective);
            List<RagSearchResult> metadataResults =
                    ragSearchRepository.metadataSearch(effective);

            // Merge by chunkId, keeping the higher score for duplicates.
            Map<Long, RagSearchResult> merged = new LinkedHashMap<>();
            for (RagSearchResult r : vectorResults) {
                merged.put(r.chunkId(), r);
            }
            for (RagSearchResult r : metadataResults) {
                merged.merge(r.chunkId(), r,
                        (a, b) -> a.score() >= b.score() ? a : b);
            }

            List<RagSearchResult> ranked = merged.values().stream()
                    .sorted(Comparator.comparingDouble(RagSearchResult::score).reversed())
                    .limit(effective.topK())
                    .toList();

            log.debug("[VectorRag] hybridSearch '{}' → {} merged (vector={}, metadata={})",
                    truncate(query), ranked.size(), vectorResults.size(), metadataResults.size());
            writeCache(cacheKey, ranked);
            return ranked;
        } catch (Exception e) {
            log.warn("[VectorRag] hybridSearch failed for '{}': {}", truncate(query), e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<RagSearchResult> mmrSearch(String query, SearchFilters filters) {
        SearchFilters effective = effectiveFilters(filters);
        String cacheKey = buildCacheKey("mmr", query, effective);
        List<RagSearchResult> cached = readCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            int topK = effective.topK();
            int candidateK = Math.max(topK * 2, topK + 1);

            SearchFilters candidateFilters = effective.toBuilder()
                    .topK(candidateK)
                    .build();
            float[] queryEmbedding = embeddingServiceFactory.getEmbeddingService().embed(query);
            List<RagSearchResult> candidates =
                    ragSearchRepository.vectorSearch(queryEmbedding, candidateFilters);

            if (candidates.size() <= topK) {
                writeCache(cacheKey, candidates);
                return candidates;
            }

            List<RagSearchResult> selected = selectMmr(candidates, effective.mmrLambda(), topK);
            log.debug("[VectorRag] mmrSearch '{}' → {} selected from {} candidates (λ={})",
                    truncate(query), selected.size(), candidates.size(), effective.mmrLambda());
            writeCache(cacheKey, selected);
            return selected;
        } catch (Exception e) {
            log.warn("[VectorRag] mmrSearch failed for '{}': {}", truncate(query), e.getMessage());
            return List.of();
        }
    }

    // ────────────────── MMR core ──────────────────

    /**
     * Greedy MMR selection. Exposed package-private so that
     * {@link MmrRagService} can reuse the exact same algorithm when it
     * decorates this service.
     *
     * @param candidates the candidate pool (already ordered by relevance)
     * @param lambda     relevance/diversity trade-off in {@code [0,1]}
     * @param topK       how many items to select
     * @return the selected subset, preserving selection order
     */
    List<RagSearchResult> selectMmr(List<RagSearchResult> candidates, double lambda, int topK) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<Long> chunkIds = candidates.stream()
                .map(RagSearchResult::chunkId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, float[]> embeddings = chunkIds.isEmpty()
                ? Map.of()
                : ragSearchRepository.findEmbeddingsByIds(chunkIds);

        List<RagSearchResult> selected = new ArrayList<>(topK);
        Set<Long> selectedIds = new HashSet<>();

        // Seed: highest-scoring candidate (pure relevance).
        RagSearchResult first = candidates.stream()
                .max(Comparator.comparingDouble(RagSearchResult::score))
                .orElse(candidates.get(0));
        selected.add(first);
        if (first.chunkId() != null) {
            selectedIds.add(first.chunkId());
        }

        while (selected.size() < topK && selected.size() < candidates.size()) {
            RagSearchResult best = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (RagSearchResult candidate : candidates) {
                if (selectedIds.contains(candidate.chunkId())) {
                    continue;
                }
                float[] candEmb = candidate.chunkId() == null
                        ? null : embeddings.get(candidate.chunkId());
                double maxSimToSelected = 0.0;
                for (RagSearchResult sel : selected) {
                    float[] selEmb = sel.chunkId() == null
                            ? null : embeddings.get(sel.chunkId());
                    double sim = cosineSimilarity(candEmb, selEmb);
                    if (sim > maxSimToSelected) {
                        maxSimToSelected = sim;
                    }
                }
                double mmrScore = lambda * candidate.score() - (1 - lambda) * maxSimToSelected;
                if (mmrScore > bestScore) {
                    bestScore = mmrScore;
                    best = candidate;
                }
            }

            if (best == null) {
                break;
            }
            selected.add(best);
            if (best.chunkId() != null) {
                selectedIds.add(best.chunkId());
            }
        }
        return selected;
    }

    // ────────────────── helpers ──────────────────

    private SearchFilters effectiveFilters(SearchFilters filters) {
        if (filters == null) {
            KnowledgeProperties.Rag rag = knowledgeProperties.getRag();
            return SearchFilters.builder()
                    .topK(rag.getTopK())
                    .similarityThreshold(rag.getSimilarityThreshold())
                    .mmrLambda(rag.getMmrLambda())
                    .build();
        }
        return filters;
    }

    private String buildCacheKey(String method, String query, SearchFilters filters) {
        String signature = method + "|" + query + "|" + filters;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(signature.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return CACHE_PREFIX + method + ":" + hex;
        } catch (Exception e) {
            // Fallback: integer hash (higher collision probability, but still correct)
            return CACHE_PREFIX + method + ":" + Integer.toHexString(signature.hashCode());
        }
    }

    private List<RagSearchResult> readCache(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, new TypeReference<List<RagSearchResult>>() {});
        } catch (Exception e) {
            log.debug("[VectorRag] Cache read failed for {}: {}", key, e.getMessage());
            return null;
        }
    }

    private void writeCache(String key, List<RagSearchResult> results) {
        try {
            Duration ttl = Duration.ofMinutes(knowledgeProperties.getRag().getCacheTtlMinutes());
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(results), ttl);
        } catch (Exception e) {
            log.debug("[VectorRag] Cache write failed for {}: {}", key, e.getMessage());
        }
    }

    static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0.0 ? 0.0 : dot / denom;
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() <= 80 ? s : s.substring(0, 80) + "...";
    }
}
