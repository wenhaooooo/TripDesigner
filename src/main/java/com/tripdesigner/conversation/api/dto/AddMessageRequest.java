package com.tripdesigner.conversation.api.dto;
/**
 * 添加消息请求 DTO。
 */

import com.tripdesigner.conversation.domain.ConversationRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMessageRequest {
    @NotNull(message = "role is required")
    private ConversationRole role;

    @NotBlank(message = "content is required")
    private String content;

    private String metadata;
}
