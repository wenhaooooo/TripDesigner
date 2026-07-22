package com.tripdesigner.ai.trip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateRequest {

    @NotBlank(message = "prompt is required")
    @Size(max = 2000, message = "prompt must be at most 2000 characters")
    private String prompt;

    /** 可选：传入已有对话 ID，使历史消息参与本次生成；为 null 则新建对话 */
    private Long conversationId;
}
