package com.tripdesigner.knowledge.rag;

import java.util.List;

/**
 * Strategy interface for retrieving relevant knowledge chunks from the Global
 * Travel Knowledge Base.
 *
 * <p>Three retrieval strategies are exposed as separate methods rather than a
 * single {@code search(strategy, ...)} call so that callers (and decorators
 * such as {@link HybridRagService} / {@link MmrRagService}) can explicitly
 * pick the algorithm without runtime string matching:
 *
 * <ul>
 *   <li>{@link #search(String, SearchFilters)} — pure semantic (vector)
 *       retrieval. Fast and the natural choice when the user's query is
 *       well-formed and self-contained.</li>
 *   <li>{@link #hybridSearch(String, SearchFilters)} — combines dense vector
 *       similarity with sparse keyword matching (PostgreSQL full-text search)
 *       and re-ranks with a weighted score. Better for queries that contain
 *       rare proper nouns (hotel names, dish names) that embeddings may not
 *       capture.</li>
 *   <li>{@link #mmrSearch(String, SearchFilters)} — Maximal Marginal
 *       Relevance. Returns a diverse subset of the top candidates to avoid
 *       feeding the LLM near-duplicate chunks (e.g. five paraphrases of the
 *       same attraction description). Especially useful when building a
 *       multi-prompt context window with a tight token budget.</li>
 * </ul>
 *
 * <p>All implementations MUST be thread-safe and MUST return an empty list
 * (never {@code null}) when no result matches.
 */
public interface RagRetrievalService {

    /**
     * Pure semantic retrieval: embed the query and run an ANN/brute-force
     * cosine search against the {@code knowledge_chunk} table.
     *
     * @param query   the user query (raw text)
     * @param filters metadata predicates and tuning parameters
     * @return the top-K results ordered by descending similarity
     */
    List<RagSearchResult> search(String query, SearchFilters filters);

    /**
     * Hybrid retrieval: merge vector similarity and keyword (FTS) matches,
     * then re-rank by {@code alpha * vectorScore + (1-alpha) * keywordScore}.
     *
     * @param query   the user query (raw text)
     * @param filters metadata predicates and tuning parameters
     * @return the top-K merged results ordered by descending hybrid score
     */
    List<RagSearchResult> hybridSearch(String query, SearchFilters filters);

    /**
     * Maximal Marginal Relevance retrieval: pull 2*K candidates via
     * {@link #search(String, SearchFilters)} and greedily select K items that
     * maximise relevance to the query while penalising similarity to already
     * selected items.
     *
     * @param query   the user query (raw text)
     * @param filters metadata predicates and tuning parameters; the
     *                {@code mmrLambda} field controls the relevance/diversity
     *                trade-off
     * @return K diverse results
     */
    List<RagSearchResult> mmrSearch(String query, SearchFilters filters);
}
