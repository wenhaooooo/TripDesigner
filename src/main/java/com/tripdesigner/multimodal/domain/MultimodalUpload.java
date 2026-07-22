package com.tripdesigner.multimodal.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * 多模态上传领域实体（聚合根）。
 *
 * 代表用户上传的一张图片，用于 AI 识别并生成行程。
 * 不可变模式：所有变更通过 withXxx 返回新实例。
 */
@Getter
@Builder
public class MultimodalUpload {

    private Long id;
    private Long userId;
    private String originalFilename;
    private String storedFilename;
    private String contentType;
    private Long fileSize;
    private String storagePath;

    /** 识别结果 JSONB：destination、tags、description、landmarks 等 */
    private Map<String, Object> recognitionResult;

    /** 关联的生成行程 ID */
    private Long generatedTripId;

    private UploadStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer version;

    public static MultimodalUpload create(Long userId, String originalFilename, String storedFilename,
                                            String contentType, Long fileSize, String storagePath) {
        return MultimodalUpload.builder()
                .userId(userId)
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .contentType(contentType)
                .fileSize(fileSize)
                .storagePath(storagePath)
                .status(UploadStatus.PENDING)
                .version(0)
                .build();
    }

    public MultimodalUpload withRecognition(Map<String, Object> result) {
        return MultimodalUpload.builder()
                .id(id).userId(userId).originalFilename(originalFilename).storedFilename(storedFilename)
                .contentType(contentType).fileSize(fileSize).storagePath(storagePath)
                .recognitionResult(result).generatedTripId(generatedTripId)
                .status(UploadStatus.RECOGNIZED).createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }

    public MultimodalUpload withGeneratedTrip(Long tripId) {
        return MultimodalUpload.builder()
                .id(id).userId(userId).originalFilename(originalFilename).storedFilename(storedFilename)
                .contentType(contentType).fileSize(fileSize).storagePath(storagePath)
                .recognitionResult(recognitionResult).generatedTripId(tripId)
                .status(UploadStatus.TRIP_GENERATED).createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }

    public MultimodalUpload withStatus(UploadStatus newStatus) {
        return MultimodalUpload.builder()
                .id(id).userId(userId).originalFilename(originalFilename).storedFilename(storedFilename)
                .contentType(contentType).fileSize(fileSize).storagePath(storagePath)
                .recognitionResult(recognitionResult).generatedTripId(generatedTripId)
                .status(newStatus).createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }
}
