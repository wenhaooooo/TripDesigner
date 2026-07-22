package com.tripdesigner.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务执行器配置。
 *
 * 为多 Agent 工作流提供专用线程池，避免阻塞 Tomcat 请求线程。
 * 工作流是 IO 密集型任务（LLM 调用），线程数可大于 CPU 核数。
 */
@Configuration
public class AsyncConfig {

    /**
     * 工作流异步执行器。
     * - 核心线程数：8（同时处理的并发工作流数量）
     * - 最大线程数：32（突发流量上限）
     * - 队列容量：64（超过后拒绝新请求，快速失败）
     * - 线程名前缀：workflow-（便于日志排查）
     */
    @Bean("workflowExecutor")
    public Executor workflowExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(64);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("workflow-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
