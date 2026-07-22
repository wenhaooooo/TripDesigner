package com.tripdesigner.multimodal.api.vo;

import com.tripdesigner.multimodal.domain.MultimodalUpload;
import com.tripdesigner.multimodal.domain.UploadStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * 多模态上传视图对象。
 */
@Getter
@Builder
public class MultimodalUploadVo {
    private final Long id;
    private final Long userId;
    private final String originalFilename;
    private final String contentType;
    private final Long fileSize;
    private final Map<String, Object> recognitionResult;
    private final Long generatedTripId;
    private final String status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static MultimodalUploadVo from(MultimodalUpload u) {
        return MultimodalUploadVo.builder()
                .id(u.getId())
                .userId(u.getUserId())
                .originalFilename(u.getOriginalFilename())
                .contentType(u.getContentType())
                .fileSize(u.getFileSize())
                .recognitionResult(u.getRecognitionResult())
                .generatedTripId(u.getGeneratedTripId())
                .status(u.getStatus() != null ? u.getStatus().name() : null)
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}
