package com.tripdesigner.price.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 价格监测定时任务。
 *
 * 每小时执行一次，扫描所有 ACTIVE 监测，刷新当前价格，
 * 并在达到目标价格时触发通知。需要 @EnableScheduling 支持
 * （由 PriceMonitorConfig 或主启动类启用）。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PriceMonitorScheduler {

    private final PriceMonitorAppService priceMonitorAppService;

    /**
     * 每小时执行一次价格检查与通知。
     * fixedRate = 3_600_000 ms = 1 小时。
     */
    @Scheduled(fixedRate = 3_600_000L)
    public void runHourlyCheck() {
        try {
            int triggered = priceMonitorAppService.checkAndNotifyPriceDrop();
            log.info("[PriceMonitorScheduler] Hourly check finished: triggered={}", triggered);
        } catch (Exception e) {
            log.error("[PriceMonitorScheduler] Hourly check failed: {}", e.getMessage(), e);
        }
    }
}
