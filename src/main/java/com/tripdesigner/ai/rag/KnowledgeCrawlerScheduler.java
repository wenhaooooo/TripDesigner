package com.tripdesigner.ai.rag;

import com.tripdesigner.crawler.CrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeCrawlerScheduler {

    private final CrawlerService crawlerService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void crawlAndIndexKnowledge() {
        log.info("[KnowledgeCrawlerScheduler] Starting daily knowledge crawl");
        Map<String, Object> result = crawlerService.crawlAllDestinations();
        log.info("[KnowledgeCrawlerScheduler] Daily crawl completed: {}", result);
    }
}