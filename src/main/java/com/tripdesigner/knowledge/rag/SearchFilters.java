package com.tripdesigner.knowledge.rag;

import java.util.Objects;

/**
 * Filters and tuning parameters applied to a RAG retrieval request.
 *
 * <p>The record carries two distinct kinds of information:
 * <ul>
 *   <li><b>Metadata predicates</b> ({@code entityType}, {@code cityId},
 *       {@code countryId}, {@code category}, {@code language}) — translated
 *       into SQL {@code WHERE} clauses or Spring AI filter expressions before
 *       the underlying vector / keyword search runs. All are nullable, which
 *       means "do not filter on this dimension".</li>
 *   <li><b>Retrieval tuning</b> ({@code similarityThreshold}, {@code topK},
 *       {@code useMmr}, {@code mmrLambda}) — control how many results come
 *       back and whether the MMR re-ranker is engaged.</li>
 * </ul>
 *
 * <p>Instances are immutable; use {@link #builder()} for construction. Default
 * values mirror {@code KnowledgeProperties.rag} so that callers can build a
 * filter without referencing the config bean.
 *
 * @param entityType         optional entity-type filter (ATTRACTION, ...)
 * @param cityId             optional city FK
 * @param countryId          optional country FK
 * @param category           optional category filter (e.g. "food")
 * @param language           optional ISO-639-1 language code
 * @param similarityThreshold minimum cosine similarity in {@code [0,1]}, default {@code 0.5}
 * @param topK               number of results to return, default {@code 5}
 * @param useMmr             whether to apply MMR re-ranking, default {@code false}
 * @param mmrLambda          MMR trade-off in {@code [0,1]} (1 = pure relevance,
 *                           0 = pure diversity), default {@code 0.5}
 */
public record SearchFilters(
        String entityType,
        Long cityId,
        Long countryId,
        String category,
        String language,
        Double similarityThreshold,
        Integer topK,
        boolean useMmr,
        double mmrLambda
) {

    public static final double DEFAULT_SIMILARITY_THRESHOLD = 0.5;
    public static final int DEFAULT_TOP_K = 5;
    public static final boolean DEFAULT_USE_MMR = false;
    public static final double DEFAULT_MMR_LAMBDA = 0.5;

    /**
     * Canonical constructor that fills in defaults for any tuning field left
     * unset, so callers can pass partial values through the builder without
     * worrying about nulls propagating into arithmetic.
     */
    public SearchFilters {
        if (similarityThreshold == null) {
            similarityThreshold = DEFAULT_SIMILARITY_THRESHOLD;
        }
        if (topK == null) {
            topK = DEFAULT_TOP_K;
        }
        if (mmrLambda < 0 || mmrLambda > 1) {
            throw new IllegalArgumentException(
                    "mmrLambda must be in [0,1], got " + mmrLambda);
        }
        if (similarityThreshold < 0 || similarityThreshold > 1) {
            throw new IllegalArgumentException(
                    "similarityThreshold must be in [0,1], got " + similarityThreshold);
        }
        if (topK < 1) {
            throw new IllegalArgumentException("topK must be >= 1, got " + topK);
        }
    }

    /**
     * @return a fresh mutable builder pre-populated with the documented defaults.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return a builder seeded with the values of this instance, useful for
     *         creating tweaked copies ({@code filters.toBuilder().topK(10).build()}).
     */
    public Builder toBuilder() {
        return new Builder()
                .entityType(entityType)
                .cityId(cityId)
                .countryId(countryId)
                .category(category)
                .language(language)
                .similarityThreshold(similarityThreshold)
                .topK(topK)
                .useMmr(useMmr)
                .mmrLambda(mmrLambda);
    }

    /**
     * Mutable builder for {@link SearchFilters}. All setters return
     * {@code this} for chaining.
     */
    public static final class Builder {
        private String entityType;
        private Long cityId;
        private Long countryId;
        private String category;
        private String language;
        private Double similarityThreshold = DEFAULT_SIMILARITY_THRESHOLD;
        private Integer topK = DEFAULT_TOP_K;
        private boolean useMmr = DEFAULT_USE_MMR;
        private double mmrLambda = DEFAULT_MMR_LAMBDA;

        public Builder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder cityId(Long cityId) {
            this.cityId = cityId;
            return this;
        }

        public Builder countryId(Long countryId) {
            this.countryId = countryId;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder similarityThreshold(Double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
            return this;
        }

        public Builder topK(Integer topK) {
            this.topK = topK;
            return this;
        }

        public Builder useMmr(boolean useMmr) {
            this.useMmr = useMmr;
            return this;
        }

        public Builder mmrLambda(double mmrLambda) {
            this.mmrLambda = mmrLambda;
            return this;
        }

        public SearchFilters build() {
            return new SearchFilters(
                    entityType, cityId, countryId, category, language,
                    similarityThreshold, topK, useMmr, mmrLambda);
        }
    }

    /**
     * @return a stable hash useful as part of a Redis cache key. Only depends
     *         on the filter values, not on tuning parameters, so that two
     *         queries that differ only in {@code topK} can still share a cache
     *         entry when the caller intentionally wants that behaviour.
     */
    public int filterHash() {
        return Objects.hash(entityType, cityId, countryId, category, language);
    }
}
