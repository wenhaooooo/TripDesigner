package com.tripdesigner.knowledge.embedding;

import com.tripdesigner.knowledge.config.KnowledgeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Selects the active {@link EmbeddingService} based on the
 * {@code knowledge.embedding.provider} configuration property.
 *
 * <p>The factory is the single entry point for code that needs to be
 * provider-agnostic (e.g. scheduled indexers, REST endpoints). It collects
 * every {@link EmbeddingService} bean registered in the context and routes
 * by {@link EmbeddingService#getProviderName()}. When the requested provider
 * is not available, it falls back to the {@code @Primary} bean (the Spring AI
 * implementation) so that a misconfigured property never breaks startup.
 *
 * <p>Default provider is {@code "spring-ai"}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingServiceFactory {

    private final KnowledgeProperties knowledgeProperties;
    private final List<EmbeddingService> availableServices;
    /** The bean annotated with {@link org.springframework.context.annotation.Primary}. */
    private final EmbeddingService primaryService;

    /**
     * @return the {@link EmbeddingService} matching
     *         {@code knowledge.embedding.provider}; falls back to the
     *         {@code @Primary} service when the requested provider is missing.
     */
    public EmbeddingService getEmbeddingService() {
        String requested = knowledgeProperties.getEmbedding().getProvider();
        Map<String, EmbeddingService> byName = availableServices.stream()
                .collect(Collectors.toMap(
                        EmbeddingService::getProviderName,
                        Function.identity(),
                        (a, b) -> a));
        EmbeddingService resolved = byName.get(requested);
        if (resolved == null) {
            log.warn("[EmbeddingServiceFactory] Provider '{}' not available (found {}). " +
                    "Falling back to primary '{}'.", requested, byName.keySet(),
                    primaryService.getProviderName());
            return primaryService;
        }
        if (!resolved.getProviderName().equals(primaryService.getProviderName())) {
            log.debug("[EmbeddingServiceFactory] Active embedding provider: {}",
                    resolved.getProviderName());
        }
        return resolved;
    }

    /**
     * @return the bean annotated {@link org.springframework.context.annotation.Primary}
     *         (always Spring AI in the current setup). Useful for callers that
     *         explicitly want the default behaviour regardless of the
     *         configured provider.
     */
    public EmbeddingService getPrimaryService() {
        return primaryService;
    }
}
