package com.tripdesigner.memory.api.vo;
/**
 * 旅行记忆视图对象（VO）。
 */

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class TripMemoryVo {
    private Long id;
    private Long tripId;
    private String memoryType;
    private String content;
    private List<String> tags;
    private Instant createdAt;
}
