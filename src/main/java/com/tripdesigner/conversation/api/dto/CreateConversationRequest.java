package com.tripdesigner.conversation.api.dto;
/**
 * 创建对话请求 DTO。
 */

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateConversationRequest {
    @Size(max = 128)
    private String title;
}
