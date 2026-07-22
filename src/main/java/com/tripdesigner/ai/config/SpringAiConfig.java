package com.tripdesigner.ai.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Spring AI 配置类。
 *
 * 配置 AI ChatClient Bean，所有 Agent 通过该 Client 与 LLM 交互。
 * ChatClient.Builder 由 spring-ai-starter-model-openai 自动注入，
 * 基于 application.yml 中的 spring.ai.openai.* 配置自动构建。
 *
 * 使用 Apache HttpClient 替代 JDK HttpURLConnection，
 * 避免 "cannot retry due to server authentication, in streaming mode" 错误。
 */
@Configuration
public class SpringAiConfig {

    @Bean
    @Primary
    public ObjectMapper openAiObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * 配置 Apache HttpClient 连接池。
     * 设置足够的最大连接数和超时时间，避免工作流执行时连接池耗尽导致 CancellationException。
     */
    @Bean
    public CloseableHttpClient httpClient() {
        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(Timeout.ofMinutes(5))
                .setTcpNoDelay(true)
                .build();

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMinutes(2))
                .build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(50);
        connectionManager.setDefaultMaxPerRoute(20);
        connectionManager.setDefaultConnectionConfig(connectionConfig);
        connectionManager.setDefaultSocketConfig(socketConfig);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMinutes(3))
                .setResponseTimeout(Timeout.ofMinutes(5))
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    /**
     * 为 Spring AI 提供自定义 RestClient.Builder，
     * 使用配置好的 Apache HttpClient 作为底层 HTTP 客户端。
     */
    @Bean
    @Primary
    public RestClient.Builder openAiRestClientBuilder(CloseableHttpClient httpClient) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return RestClient.builder().requestFactory(factory);
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
