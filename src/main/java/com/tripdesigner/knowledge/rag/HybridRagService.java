package com.tripdesigner.knowledge.rag;

import com.tripdesigner.knowledge.config.KnowledgeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hybrid RAG retriever that fuses dense vector similarity with sparse keyword
 * matching.
 *
 * <h2>Why hybrid?</h2>
 * <p>Pure vector search captures semantic intent but can miss chunks that
 * contain rare proper nouns (a specific hotel name, a regional dish) whose
 * embeddings are not close to the query's. PostgreSQL full-text search
 * ({@code tsvector @@ plainto_tsquery}) is the exact opposite: it is
 * razor-sharp on rare terms but blind to paraphrase. Combining the two via
 * a weighted score gives the best of both worlds:
 *
 * <pre>
 *   finalScore(d) = α · vectorScore(d) + (1 − α) · keywordScore(d)
 * </pre>
 *
 * <p>Default {@code α = 0.7} (configured via
 * {@code knowledge.rag.hybrid-alpha}) leans towards semantic relevance while
 * still letting strong keyword matches surface. Lower α when the user query
 * is dominated by entity names; raise α when the query is conversational.
 *
 * <h2>Decorator pattern</h2>
 * <p>This class decorates the {@link VectorRagService} (injected by concrete
 * type to avoid the self-injection ambiguity that would arise if we injected
 * the {@link RagRetrievalService} interface — both beans implement it).
 * <ul>
 *   <li>{@link #search(String, SearchFilters)} — runs the hybrid pipeline
 *       described above and is the main entry point.</li>
 *   <li>{@link #hybridSearch(String, SearchFilters)} — delegates to
 *       {@link #search} since this service IS the hybrid strategy.</li>
 *   <li>{@link #mmrSearch(String, SearchFilters)} — delegates to
 *       {@link VectorRagService#mmrSearch} for diversity re-ranking.</li>
 * </ul>
 *
 * <h2>Score normalisation</h2>
 * <p>Vector scores returned by pgvector cosine distance are already in
 * {@code [0,1]}. FTS ranks from
 * {@code ts_rank} are unbounded, so the repository implementation is
 * expected to normalise them into {@code [0,1]} (e.g. via
 * {@code rank / (1 + rank)}) before they reach this class. The weighted sum
 * therefore stays in {@code [0,1]} and remains comparable across queries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRagService implements RagRetrievalService {

    private final VectorRagService delegate;
    private final RagSearchRepository ragSearchRepository;
    private final KnowledgeProperties knowledgeProperties;

    @Override
    public List<RagSearchResult> search(String query, SearchFilters filters) {
        SearchFilters effective = (filters != null) ? filters : defaultFilters();
        double alpha = knowledgeProperties.getRag().getHybridAlpha();

        List<RagSearchResult> vectorResults = delegate.search(query, effective);
        List<RagSearchResult> keywordResults;
        try {
            keywordResults = ragSearchRepository.keywordSearch(query, effective);
        } catch (Exception e) {
            log.warn("[HybridRag] keyword search failed, falling back to vector only: {}",
                    e.getMessage());
            keywordResults = List.of();
        }

        List<RagSearchResult> merged = mergeAndRank(vectorResults, keywordResults, alpha, effective.topK());
        log.debug("[HybridRag] search '{}' → {} merged (vector={}, keyword={}, α={})",
                truncate(query), merged.size(), vectorResults.size(), keywordResults.size(), alpha);
        return merged;
    }

    @Override
    public List<RagSearchResult> hybridSearch(String query, SearchFilters filters) {
        // This service is itself the hybrid strategy; reuse search().
        return search(query, filters);
    }

    @Override
    public List<RagSearchResult> mmrSearch(String query, SearchFilters filters) {
        return delegate.mmrSearch(query, filters);
    }

    // ────────────────── fusion ──────────────────

    /**
     * Merge vector and keyword result lists by {@code chunkId}, applying the
     * weighted score {@code α · vScore + (1-α) · kScore}. When a chunk
     * appears in only one list, the missing score is treated as 0 so that a
     * keyword-only or vector-only hit can still surface when its single-arm
     * score is high enough.
     */
    private List<RagSearchResult> mergeAndRank(List<RagSearchResult> vectorResults,
                                               List<RagSearchResult> keywordResults,
                                               double alpha, int topK) {
        // chunkId → cosine similarity score from the vector arm
        Map<Long, Double> vectorScores = new LinkedHashMap<>();
        for (RagSearchResult r : vectorResults) {
            if (r.chunkId() != null) {
                vectorScores.put(r.chunkId(), r.score());
            }
        }
        // chunkId → FTS rank score from the keyword arm
        Map<Long, Double> keywordScores = new LinkedHashMap<>();
        for (RagSearchResult r : keywordResults) {
            if (r.chunkId() != null) {
                keywordScores.put(r.chunkId(), r.score());
            }
        }
        // chunkId → representative RagSearchResult (prefer the vector arm so
        // that the canonical title/content/metadata come from the semantically
        // retrieved chunk when available).
        Map<Long, RagSearchResult> byChunk = new LinkedHashMap<>();
        for (RagSearchResult r : vectorResults) {
            if (r.chunkId() != null) {
                byChunk.put(r.chunkId(), r);
            }
        }
        for (RagSearchResult r : keywordResults) {
            if (r.chunkId() != null) {
                byChunk.putIfAbsent(r.chunkId(), r);
            }
        }

        return byChunk.values().stream()
                .map(r -> {
                    double vScore = vectorScores.getOrDefault(r.chunkId(), 0.0);
                    double kScore = keywordScores.getOrDefault(r.chunkId(), 0.0);
                    double finalScore = alpha * vScore + (1 - alpha) * kScore;
                    return RagSearchResult.of(
                            r.chunkId(), r.entityType(), r.entityId(),
                            r.title(), r.content(), finalScore, r.metadata());
                })
                .sorted(Comparator.comparingDouble(RagSearchResult::score).reversed())
                .limit(topK)
                .toList();
    }

    private SearchFilters defaultFilters() {
        KnowledgeProperties.Rag rag = knowledgeProperties.getRag();
        return SearchFilters.builder()
                .topK(rag.getTopK())
                .similarityThreshold(rag.getSimilarityThreshold())
                .build();
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() <= 80 ? s : s.substring(0, 80) + "...";
    }
}
