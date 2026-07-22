package com.tripdesigner.ai.trip;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder(toBuilder = true)
public class TripGenerationTask {
    private Long id;
    private Long userId;
    private String prompt;
    private TripGenerationStatus status;
    private int progress;
    private String progressMessage;
    private Long conversationId;
    private Long tripId;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;

    public static TripGenerationTask create(Long userId, String prompt) {
        return TripGenerationTask.builder()
                .userId(userId)
                .prompt(prompt)
                .status(TripGenerationStatus.PENDING)
                .progress(0)
                .build();
    }

    public TripGenerationTask withStatus(TripGenerationStatus status) {
        return this.toBuilder().status(status).updatedAt(Instant.now()).build();
    }

    public TripGenerationTask withProgress(int progress, String message) {
        return this.toBuilder().progress(progress).progressMessage(message).updatedAt(Instant.now()).build();
    }

    public TripGenerationTask withResult(Long conversationId, Long tripId) {
        return this.toBuilder()
                .conversationId(conversationId)
                .tripId(tripId)
                .status(TripGenerationStatus.COMPLETED)
                .progress(100)
                .progressMessage("行程生成完成")
                .updatedAt(Instant.now())
                .build();
    }

    public TripGenerationTask withError(String errorMessage) {
        return this.toBuilder()
                .status(TripGenerationStatus.FAILED)
                .errorMessage(errorMessage)
                .updatedAt(Instant.now())
                .build();
    }
}