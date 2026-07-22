package com.tripdesigner.knowledge.rag;

import java.util.List;
import java.util.Map;

/**
 * RAG-layer search contract for the {@code knowledge_chunk} table.
 *
 * <p>This interface is intentionally kept separate from the domain
 * {@code com.tripdesigner.knowledge.domain.repository.KnowledgeChunkRepository}:
 * the domain repository is a CRUD gateway that returns {@code KnowledgeChunk}
 * entities (no score), whereas the RAG layer needs <em>scored</em> results
 * ({@link RagSearchResult}) and additional query modes (keyword / metadata /
 * embedding lookup) that the domain repository does not provide.
 *
 * <p>The implementation (MyBatis-based, created in the infrastructure layer)
 * is responsible for:
 * <ul>
 *   <li>Translating {@link SearchFilters} into SQL {@code WHERE} clauses.</li>
 *   <li>Computing cosine similarity scores from pgvector's {@code <=>} distance
 *       operator and normalising them into {@code [0,1]}.</li>
 *   <li>Running PostgreSQL full-text search ({@code tsvector @@ plainto_tsquery})
 *       for keyword-based retrieval.</li>
 *   <li>Returning raw embedding vectors for MMR pairwise similarity
 *       computation.</li>
 * </ul>
 *
 * <p>Consumed by {@link VectorRagService} (vector + metadata + MMR) and
 * {@link HybridRagService} (keyword FTS).
 */
public interface RagSearchRepository {

    /**
     * Run a cosine-similarity vector search against the {@code embedding}
     * column of {@code knowledge_chunk}.
     *
     * @param embedding the query embedding (length must match the column
     *                  width — 1536 by default)
     * @param filters   metadata predicates + tuning (topK, threshold)
     * @return matching chunks ordered by descending similarity, with
     *         {@link RagSearchResult#score()} set to the normalised cosine
     *         similarity in {@code [0,1]}
     */
    List<RagSearchResult> vectorSearch(float[] embedding, SearchFilters filters);

    /**
     * Run a PostgreSQL full-text search on the {@code content} column.
     *
     * @param query   raw user query; the implementation constructs a
     *                {@code plainto_tsquery} from it
     * @param filters metadata predicates + tuning (topK)
     * @return matching chunks ordered by descending FTS rank, with
     *         {@link RagSearchResult#score()} normalised into {@code [0,1]}
     */
    List<RagSearchResult> keywordSearch(String query, SearchFilters filters);

    /**
     * Run a pure metadata predicate search (no text similarity). Useful as a
     * low-cost fallback or as one arm of a hybrid merge.
     *
     * @param filters metadata predicates + tuning (topK)
     * @return matching chunks ordered by recency / id descending
     */
    List<RagSearchResult> metadataSearch(SearchFilters filters);

    /**
     * Fetch the stored embedding vectors for the given chunk ids. Used by the
     * MMR re-rankers ({@link VectorRagService#mmrSearch} and
     * {@link MmrRagService}) to compute pairwise document similarity without
     * re-embedding candidate content.
     *
     * @param chunkIds the chunk ids to look up (must not be {@code null})
     * @return a map from chunk id to its stored embedding; ids that are not
     *         found are simply absent from the map
     */
    Map<Long, float[]> findEmbeddingsByIds(List<Long> chunkIds);
}
