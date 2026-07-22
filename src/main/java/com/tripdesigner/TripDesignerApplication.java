package com.tripdesigner;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Trip Designer 应用主入口。
 *
 * 这是基于 Spring Boot 3 + Spring AI 的 AI 旅行规划系统，
 * 通过多 Agent 协作工作流自动为用户生成个性化旅行计划。
 *
 * 启动时会自动执行 Flyway 数据库迁移脚本（位于 src/main/resources/db/migration/），
 * 并扫描 com.tripdesigner.**.infrastructure 和 com.tripdesigner.ai.trip.workflow 包下的 MyBatis Mapper 接口。
 *
 * @EnableAsync 启用异步方法执行，用于多 Agent 工作流异步化，避免阻塞请求线程。
 * @EnableScheduling 启用定时任务，用于 RateLimiter 过期桶清理等周期性维护。
 */
@SpringBootApplication
@MapperScan({"com.tripdesigner.**.infrastructure", "com.tripdesigner.ai.trip.workflow.mapper"})
@EnableAsync
@EnableScheduling
public class TripDesignerApplication {

    public static void main(String[] args) {
        // 启动 Spring Boot 应用
        SpringApplication.run(TripDesignerApplication.class, args);
    }
}