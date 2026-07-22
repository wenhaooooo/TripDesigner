package com.tripdesigner.crawler;

import com.tripdesigner.ai.rag.DestinationKnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerService {

    private final List<TravelCrawler> crawlers;
    private final DestinationKnowledgeService knowledgeService;

    private static final List<String> POPULAR_DESTINATIONS = List.of(
            "东京", "大阪", "京都", "北海道",
            "首尔", "济州岛",
            "曼谷", "普吉岛", "清迈",
            "新加坡", "巴厘岛",
            "巴黎", "伦敦", "罗马", "巴塞罗那",
            "纽约", "洛杉矶", "旧金山",
            "悉尼", "墨尔本",
            "北京", "上海", "杭州", "成都", "重庆",
            "西安", "苏州", "厦门", "青岛", "三亚"
    );

    public Map<String, Object> crawlAllDestinations() {
        return crawlDestinations(POPULAR_DESTINATIONS, 5);
    }

    public Map<String, Object> crawlDestinations(List<String> destinations, int limitPerSource) {
        AtomicInteger totalIndexed = new AtomicInteger(0);
        List<String> succeeded = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        for (String destination : destinations) {
            try {
                int indexed = crawlDestination(destination, limitPerSource);
                totalIndexed.addAndGet(indexed);
                succeeded.add(destination);
                Thread.sleep(2000);
            } catch (Exception e) {
                log.warn("[CrawlerService] Failed to crawl destination: {}, error: {}", destination, e.getMessage());
                failed.add(destination);
            }
        }

        return Map.of(
                "totalIndexed", totalIndexed.get(),
                "succeeded", succeeded,
                "failed", failed,
                "totalDestinations", destinations.size()
        );
    }

    public int crawlDestination(String destination, int limitPerSource) {
        log.info("[CrawlerService] Crawling destination: {}", destination);
        int totalIndexed = 0;

        for (TravelCrawler crawler : crawlers) {
            try {
                List<String> posts = crawler.crawl(destination, limitPerSource);
                if (!posts.isEmpty()) {
                    knowledgeService.indexDestinationKnowledge(destination, "TIPS", crawler.getSourceName(), posts);
                    totalIndexed += posts.size();
                    log.info("[CrawlerService] Indexed {} posts from {} for destination: {}",
                            posts.size(), crawler.getSourceName(), destination);
                }
                Thread.sleep(500);
            } catch (Exception e) {
                log.warn("[CrawlerService] Crawler {} failed for {}: {}",
                        crawler.getSourceName(), destination, e.getMessage());
            }
        }

        return totalIndexed;
    }

    public List<String> getAvailableSources() {
        return crawlers.stream().map(TravelCrawler::getSourceName).toList();
    }
}