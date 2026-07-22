package com.tripdesigner.ai.trip.infrastructure;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("trip_generation_tasks")
public class TripGenerationTaskPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String prompt;
    private String status;
    private Integer progress;
    private String progressMessage;
    private Long conversationId;
    private Long tripId;
    private String errorMessage;
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
}