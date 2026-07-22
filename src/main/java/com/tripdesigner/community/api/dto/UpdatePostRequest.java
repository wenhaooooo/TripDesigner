package com.tripdesigner.community.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePostRequest {
    @Size(max = 200, message = "title must be at most 200 characters")
    private String title;
    @Size(max = 10000, message = "content must be at most 10000 characters")
    private String content;
    @Size(max = 100)
    private String destination;
    private java.util.List<String> tags;
    private java.util.List<String> mediaUrls;
}
