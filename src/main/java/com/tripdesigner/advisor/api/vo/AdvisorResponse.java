package com.tripdesigner.advisor.api.vo;

import lombok.Builder;
import lombok.Getter;

/**
 * AI 旅行顾问回答视图对象（VO）。
 */
@Getter
@Builder
public class AdvisorResponse {

    private final String answer;
    private final Long conversationId;
}
