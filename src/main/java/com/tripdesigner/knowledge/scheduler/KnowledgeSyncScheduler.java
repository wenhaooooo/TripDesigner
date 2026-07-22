package com.tripdesigner.knowledge.scheduler;

import com.tripdesigner.knowledge.crawler.DataCrawler;
import com.tripdesigner.knowledge.domain.KnowledgeSource;
import com.tripdesigner.knowledge.domain.repository.KnowledgeSourceRepository;
import com.tripdesigner.knowledge.pipeline.KnowledgePipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 知识库同步调度器。
 *
 * <p>定时执行全球旅行知识库的数据同步，从配置的数据源（OSM、Wikivoyage、
 * Wikipedia、OpenTripMap）抓取最新数据，经过完整的流水线处理后存入向量库。
 *
 * <p>调度配置：
 * <pre>
 * cron: ${knowledge.scheduler.cron:0 0 2 * * ?}  # 默认每天凌晨 2:00 执行
 * </pre>
 *
 * <p>同步流程（每个知识源记录）：
 * <ol>
 *   <li>查找对应的 {@link DataCrawler}（按 sourceType 匹配）</li>
 *   <li>增量检查：调用 {@code crawlWithHash} 比较内容哈希，跳过未变更的数据</li>
 *   <li>全量抓取：调用 {@code crawl} 获取完整数据</li>
 *   <li>流水线处理：{@link KnowledgePipeline#processCrawlResult} 解析→清洗→分块→向量化→存储</li>
 *   <li>更新知识源记录的状态和内容哈希</li>
 * </ol>
 *
 * <p>容错机制：
 * <ul>
 *   <li>重试逻辑：最多 3 次重试，指数退避（1s → 2s → 4s）</li>
 *   <li>请求限速：每个知识源之间间隔 2 秒，避免 API 压力</li>
 *   <li>错误隔离：单个知识源失败不影响其他源的处理</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeSyncScheduler {

    /** 最大重试次数 */
    private static final int MAX_RETRIES = 3;

    /** 重试初始退避间隔（毫秒） */
    private static final long INITIAL_BACKOFF_MS = 1000L;

    /** 知识源之间的请求间隔（毫秒） */
    private static final long INTER_SOURCE_DELAY_MS = 2000L;

    /** 每批获取的待处理知识源数量 */
    private static final int BATCH_SIZE = 50;

    /** 默认抓取条数 */
    private static final int DEFAULT_CRAWL_LIMIT = 50;

    /** 同步状态常量 */
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private final KnowledgeSourceRepository sourceRepository;
    private final KnowledgePipeline pipeline;
    private final List<DataCrawler> crawlers;

    /** 数据源名称 → 爬虫实例的映射（懒加载） */
    private Map<String, DataCrawler> crawlerMap;

    /**
     * 获取按 sourceName 索引的爬虫映射（懒加载）。
     */
    private Map<String, DataCrawler> getCrawlerMap() {
        if (crawlerMap == null) {
            crawlerMap = crawlers.stream()
                    .collect(Collectors.toMap(
                            c -> c.getSourceName().toUpperCase(),
                            Function.identity()));
        }
        return crawlerMap;
    }

    /**
     * 定时同步知识库。
     *
     * <p>Cron 表达式可通过 {@code knowledge.scheduler.cron} 配置覆盖，
     * 默认每天凌晨 2:00 执行（{@code 0 0 2 * * ?}）。
     */
    @Scheduled(cron = "${knowledge.scheduler.cron:0 0 2 * * ?}")
    public void syncKnowledge() {
        log.info("[KnowledgeSyncScheduler] Starting knowledge sync");

        int totalProcessed = 0;
        int totalSkipped = 0;
        int totalFailed = 0;

        // 获取待同步的知识源
        List<KnowledgeSource> pendingSources = sourceRepository.findPending(BATCH_SIZE);

        log.info("[KnowledgeSyncScheduler] Found {} pending sources", pendingSources.size());

        for (KnowledgeSource source : pendingSources) {
            try {
                // 标记为处理中
                sourceRepository.save(source.withStatus(STATUS_PROCESSING));

                ProcessResult result = processWithRetry(source);

                if (result.success()) {
                    // 更新状态和内容哈希
                    KnowledgeSource completed = source.withStatus(STATUS_SUCCESS);
                    sourceRepository.save(completed);
                    totalProcessed += result.indexedCount();
                    if (result.skipped()) {
                        totalSkipped++;
                    }
                    log.info("[KnowledgeSyncScheduler] Source {} sync completed: {} chunks indexed",
                            source.getSourceType(), result.indexedCount());
                } else {
                    // 标记为失败，增加重试计数
                    int newRetryCount = (source.getRetryCount() != null ? source.getRetryCount() : 0) + 1;
                    sourceRepository.save(source.withStatus(STATUS_FAILED));
                    totalFailed++;
                    log.warn("[KnowledgeSyncScheduler] Source {} sync failed after {} retries",
                            source.getSourceType(), newRetryCount);
                }

                // 知识源之间限速
                rateLimit(INTER_SOURCE_DELAY_MS);
            } catch (Exception e) {
                log.error("[KnowledgeSyncScheduler] Unexpected error processing source {}: {}",
                        source.getSourceType(), e.getMessage(), e);
                try {
                    sourceRepository.save(source.withStatus(STATUS_FAILED));
                } catch (Exception ignored) {
                    // 仓储更新失败不影响主流程
                }
                totalFailed++;
            }
        }

        log.info("[KnowledgeSyncScheduler] Knowledge sync completed: processed={}, skipped={}, failed={}",
                totalProcessed, totalSkipped, totalFailed);
    }

    /**
     * 带重试逻辑的知识源处理。
     *
     * <p>重试策略：最多 {@link #MAX_RETRIES} 次，指数退避（1s → 2s → 4s）。
     *
     * @param source 知识源记录
     * @return 处理结果
     */
    private ProcessResult processWithRetry(KnowledgeSource source) {
        DataCrawler crawler = findCrawler(source.getSourceType());
        if (crawler == null) {
            log.warn("[KnowledgeSyncScheduler] No crawler found for source type: {}", source.getSourceType());
            return ProcessResult.failure();
        }

        String query = extractQuery(source);
        int limit = DEFAULT_CRAWL_LIMIT;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                // 增量检查：如果支持增量且哈希未变，跳过
                if (crawler.supportsIncremental() && source.getContentHash() != null) {
                    String currentHash = crawler.crawlWithHash(query, limit, source.getContentHash());
                    if (currentHash.equals(source.getContentHash())) {
                        log.debug("[KnowledgeSyncScheduler] Source {} unchanged (hash match), skipping",
                                source.getSourceType());
                        return ProcessResult.skipped(source.getContentHash());
                    }
                }

                // 全量抓取
                List<Map<String, Object>> rawData = crawler.crawl(query, limit);
                if (rawData == null || rawData.isEmpty()) {
                    log.debug("[KnowledgeSyncScheduler] No data crawled for source: {}", source.getSourceType());
                    return ProcessResult.success(computeHash(rawData), 0);
                }

                // 流水线处理
                int indexed = pipeline.processCrawlResult(source, rawData);
                String contentHash = computeHash(rawData);

                return ProcessResult.success(contentHash, indexed);
            } catch (Exception e) {
                log.warn("[KnowledgeSyncScheduler] Attempt {}/{} failed for source {}: {}",
                        attempt + 1, MAX_RETRIES + 1, source.getSourceType(), e.getMessage());

                if (attempt < MAX_RETRIES) {
                    // 指数退避等待
                    long backoff = INITIAL_BACKOFF_MS * (1L << attempt);
                    log.debug("[KnowledgeSyncScheduler] Retrying in {}ms", backoff);
                    rateLimit(backoff);
                }
            }
        }

        return ProcessResult.failure();
    }

    /**
     * 根据数据源类型查找爬虫（大小写不敏感匹配）。
     *
     * @param sourceType 数据源类型
     * @return 匹配的爬虫；未找到返回 null
     */
    private DataCrawler findCrawler(String sourceType) {
        if (sourceType == null) {
            return null;
        }
        return getCrawlerMap().get(sourceType.toUpperCase());
    }

    /**
     * 从知识源记录中提取查询条件。
     * 优先使用 sourceUrl，其次使用 sourceId。
     *
     * @param source 知识源记录
     * @return 查询条件字符串
     */
    private String extractQuery(KnowledgeSource source) {
        if (source.getSourceUrl() != null && !source.getSourceUrl().isBlank()) {
            return source.getSourceUrl();
        }
        if (source.getSourceId() != null && !source.getSourceId().isBlank()) {
            return source.getSourceId();
        }
        return "";
    }

    /**
     * 计算数据的哈希值（基于列表大小和内容）。
     */
    private String computeHash(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return "empty";
        }
        return String.valueOf(data.hashCode());
    }

    /**
     * 请求限速。
     *
     * @param delayMs 延迟毫秒数
     */
    private void rateLimit(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 处理结果内部记录。
     *
     * @param success      是否成功
     * @param contentHash  内容哈希值
     * @param indexedCount 索引的分块数
     * @param skipped      是否因哈希匹配而跳过
     */
    private record ProcessResult(boolean success, String contentHash, int indexedCount, boolean skipped) {
        static ProcessResult success(String hash, int indexed) {
            return new ProcessResult(true, hash, indexed, false);
        }

        static ProcessResult skipped(String hash) {
            return new ProcessResult(true, hash, 0, true);
        }

        static ProcessResult failure() {
            return new ProcessResult(false, null, 0, false);
        }
    }
}
