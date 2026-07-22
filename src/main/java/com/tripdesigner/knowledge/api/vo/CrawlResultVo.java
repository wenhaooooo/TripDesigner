package com.tripdesigner.knowledge.api.vo;

import java.time.Instant;

/**
 * 知识库抓取结果视图对象。
 *
 * 用于 API 响应中返回一次抓取任务的执行结果统计，包括抓取数、解析数、索引数等。
 * 通过 failed 和 skipped 字段可快速识别抓取过程中的异常情况。
 *
 * @param source      数据源标识（OSM, WIKIVOYAGE, WIKIPEDIA, OPENTRIPMAP）
 * @param crawled     已抓取的原始条目数
 * @param parsed      已解析的结构化条目数
 * @param indexed     已成功向量化并索引的条目数
 * @param skipped     跳过的条目数（如增量更新时未变更的条目）
 * @param failed      失败的条目数
 * @param message     结果消息（成功/失败的原因说明）
 * @param completedAt 完成时间
 */
public record CrawlResultVo(
        String source,
        int crawled,
        int parsed,
        int indexed,
        int skipped,
        int failed,
        String message,
        Instant completedAt
) {
}
