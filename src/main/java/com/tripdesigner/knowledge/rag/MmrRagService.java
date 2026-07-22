package com.tripdesigner.knowledge.rag;

import com.tripdesigner.knowledge.config.KnowledgeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG retriever that applies Maximal Marginal Relevance (MMR) for diverse
 * context selection.
 *
 * <h2>Why MMR?</h2>
 * <p>When building the LLM prompt context for a travel-planning request, we
 * typically retrieve the top-K most similar knowledge chunks. The problem:
 * cosine similarity retrieval over a densely indexed knowledge base often
 * returns near-duplicate chunks — five slight paraphrases of the same
 * attraction description, or three blog posts that all mention the same
 * restaurant. Feeding all of them to the LLM wastes tokens and biases the
 * output toward a single fact.
 *
 * <p>MMR (Carbonell &amp; Goldstein, 1998) solves this by selecting a subset
 * that is simultaneously relevant to the query and diverse among itself:
 *
 * <pre>
 *   mmr(d) = λ · sim(query, d) − (1 − λ) · max_{s ∈ S} sim(d, s)
 * </pre>
 *
 * where {@code S} is the set of already-selected chunks. The first term
 * rewards relevance to the query; the second term penalises redundancy with
 * what's already been selected. At each step we greedily pick the candidate
 * that maximises {@code mmr(d)}, so the output is a Pareto-style trade-off
 * along the relevance/diversity frontier.
 *
 * <ul>
 *   <li>{@code λ = 1.0} → pure relevance (equivalent to plain vector search).</li>
 *   <li>{@code λ = 0.0} → pure diversity (returns the most mutually-dissimilar
 *       chunks, ignoring the query — rarely useful on its own).</li>
 *   <li>{@code λ = 0.5} (default) → balanced; recommended for travel
 *       knowledge where we want the most relevant facts plus some variety
 *       (a mix of attractions, food, and tips rather than three paragraphs
 *       about the same temple).</li>
 * </ul>
 *
 * <h2>Decorator pattern</h2>
 * <p>This class decorates {@link VectorRagService}. It delegates
 * {@link #search} and {@link #hybridSearch} unchanged, and overrides
 * {@link #mmrSearch} to run the MMR algorithm using the delegate's
 * {@link VectorRagService#search} as the candidate source. The actual MMR
 * selection logic lives in
 * {@link VectorRagService#selectMmr(List, double, int)} and is reused here
 * to guarantee identical behaviour between the {@code @Primary} service and
 * this decorator.
 *
 * <h2>Algorithm</h2>
 * <ol>
 *   <li>Retrieve {@code 2 * K} candidates via vector search (double the
 *       desired output size so the diversity step has room to discard
 *       duplicates without running out of candidates).</li>
 *   <li>Fetch the stored embeddings for all candidates via
 *       {@link RagSearchRepository#findEmbeddingsByIds(List)}.</li>
 *   <li>Seed the selected set with the single most relevant candidate.</li>
 *   <li>Iteratively add the candidate that maximises the MMR score until
 *       {@code |selected| = K}.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MmrRagService implements RagRetrievalService {

    private final VectorRagService delegate;
    private final KnowledgeProperties knowledgeProperties;

    @Override
    public List<RagSearchResult> search(String query, SearchFilters filters) {
        return delegate.search(query, filters);
    }

    @Override
    public List<RagSearchResult> hybridSearch(String query, SearchFilters filters) {
        return delegate.hybridSearch(query, filters);
    }

    @Override
    public List<RagSearchResult> mmrSearch(String query, SearchFilters filters) {
        SearchFilters effective = (filters != null) ? filters : defaultFilters();
        int topK = effective.topK();
        double lambda = effective.mmrLambda();

        // Retrieve 2*K candidates via vector search through the delegate.
        int candidateK = Math.max(topK * 2, topK + 1);
        SearchFilters candidateFilters = effective.toBuilder()
                .topK(candidateK)
                .build();
        List<RagSearchResult> candidates = delegate.search(query, candidateFilters);

        if (candidates.size() <= topK) {
            log.debug("[MmrRag] candidates={} ≤ topK={}, skipping MMR", candidates.size(), topK);
            return candidates;
        }

        // Reuse the shared MMR selection routine from the primary service so
        // that both entry points produce identical selections.
        List<RagSearchResult> selected = delegate.selectMmr(candidates, lambda, topK);
        log.debug("[MmrRag] mmrSearch '{}' → {} selected from {} candidates (λ={})",
                truncate(query), selected.size(), candidates.size(), lambda);
        return selected;
    }

    private SearchFilters defaultFilters() {
        KnowledgeProperties.Rag rag = knowledgeProperties.getRag();
        return SearchFilters.builder()
                .topK(rag.getTopK())
                .similarityThreshold(rag.getSimilarityThreshold())
                .mmrLambda(rag.getMmrLambda())
                .useMmr(true)
                .build();
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() <= 80 ? s : s.substring(0, 80) + "...";
    }
}
