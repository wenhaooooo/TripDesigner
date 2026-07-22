package com.tripdesigner.ai.trip;
/**
 * AI 行程生成结果 DTO。
 * 包含生成的行程详情和关联的对话 ID。
 */

import com.tripdesigner.trip.api.vo.TripDetailVo;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TripGenerationResult {
    private final Long conversationId;
    private final TripDetailVo trip;
}
