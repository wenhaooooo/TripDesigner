package com.tripdesigner.price.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 价格点值对象。
 * 记录某个时间点的价格快照，包含价格、记录时间和数据来源。
 * 作为 PriceMonitor 的 priceHistory 列表元素，以 JSONB 形式持久化。
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PricePoint {
    /** 价格 */
    private BigDecimal price;
    /** 记录时间 */
    private Instant recordedAt;
    /** 数据来源（如：simulated、ctrip、qunar） */
    private String source;
}
