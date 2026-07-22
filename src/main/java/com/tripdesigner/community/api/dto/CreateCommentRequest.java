package com.tripdesigner.community.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCommentRequest {
    @NotBlank(message = "content is required")
    @Size(max = 2000, message = "content must be at most 2000 characters")
    private String content;

    /** 父评论 ID（回复评论时传入） */
    private Long parentId;
}
