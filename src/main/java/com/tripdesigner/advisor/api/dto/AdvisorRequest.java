package com.tripdesigner.advisor.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 旅行顾问提问请求 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvisorRequest {

    @NotBlank(message = "question is required")
    @Size(max = 2000, message = "question must be at most 2000 characters")
    private String question;

    private Long conversationId;
}
