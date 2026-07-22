package com.tripdesigner.community.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreatePostRequest {
    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title must be at most 200 characters")
    private String title;

    @NotBlank(message = "content is required")
    @Size(max = 10000, message = "content must be at most 10000 characters")
    private String content;

    @Size(max = 100, message = "destination must be at most 100 characters")
    private String destination;

    private List<String> tags;
    private List<String> mediaUrls;
}
