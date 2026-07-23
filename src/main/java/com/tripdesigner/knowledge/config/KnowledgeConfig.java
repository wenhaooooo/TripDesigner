package com.tripdesigner.knowledge.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for the Global Travel Knowledge Base module.
 *
 * <p>Registers the {@link KnowledgeProperties} bean (bound from
 * {@code knowledge.*}) and a dedicated {@link RestTemplate} for the
 * knowledge-source crawlers. The crawler {@link RestTemplate} uses an Apache
 * HttpClient 5 connection pool with conservative timeouts so that a single
 * misbehaving source cannot exhaust the servlet container's threads.
 *
 * <p>The bean is named {@code knowledgeRestTemplate} to avoid colliding with
 * any other {@link RestTemplate} / {@link org.springframework.web.client.RestClient}
 * in the application (e.g. the Spring AI one configured in
 * {@code SpringAiConfig}).
 */
@Configuration
public class KnowledgeConfig {

    /**
     * Bind {@code knowledge.*} properties to a {@link KnowledgeProperties}
     * instance. Using {@code @Bean @ConfigurationProperties} (rather than
     * {@code @Component} on the class) keeps the property class free of Spring
     * stereotype annotations and makes it trivially constructable in unit
     * tests.
     *
     * @return the bound properties bean
     */
    @Bean
    @ConfigurationProperties(prefix = "knowledge")
    public KnowledgeProperties knowledgeProperties() {
        return new KnowledgeProperties();
    }

    /**
     * Dedicated {@link RestTemplate} for knowledge crawlers.
     *
     * <p>Connection pool: 20 max total, 10 per route. Timeouts:
     * connect=10s, request=30s. These are deliberately tighter than the
     * Spring AI client because crawlers hit third-party sites that should
     * fail fast rather than block the scheduler thread.
     *
     * @param builder     the auto-configured {@link RestTemplateBuilder}
     * @param properties  knowledge properties (used for future tuning hooks)
     * @return a pooled {@link RestTemplate} named {@code knowledgeRestTemplate}
     */
    @Bean("knowledgeRestTemplate")
    public RestTemplate knowledgeRestTemplate(RestTemplateBuilder builder,
                                              KnowledgeProperties properties) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(20);
        connectionManager.setDefaultMaxPerRoute(10);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))
                .setConnectionRequestTimeout(Timeout.ofSeconds(5))
                .setResponseTimeout(Timeout.ofSeconds(30))
                .build();

        CloseableHttpClient httpClient = org.apache.hc.client5.http.impl.classic.HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return builder
                .requestFactory(() -> factory)
                .build();
    }
}
