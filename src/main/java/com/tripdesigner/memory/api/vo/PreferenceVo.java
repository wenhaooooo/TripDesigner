package com.tripdesigner.memory.api.vo;
/**
 * 用户偏好视图对象（VO）。
 */

import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class PreferenceVo {
    private Long id;
    private String category;
    private Map<String, Object> data;
    private String source;
    private Instant createdAt;
    private Instant updatedAt;
}
