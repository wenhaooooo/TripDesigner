package com.tripdesigner.price.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 价格监测模块配置。
 *
 * 显式启用 @EnableScheduling 以支持 PriceMonitorScheduler 的定时任务。
 * （主启动类 TripDesignerApplication 已全局启用，此处为模块自包含配置。）
 */
@Configuration
@EnableScheduling
public class PriceMonitorConfig {
}
