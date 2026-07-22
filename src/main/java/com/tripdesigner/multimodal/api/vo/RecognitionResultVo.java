package com.tripdesigner.multimodal.api.vo;

import lombok.Builder;
import lombok.Getter;

/**
 * 图片识别结果 VO。
 */
@Getter
@Builder
public class RecognitionResultVo {
    private final String destination;
    private final String description;
    private final java.util.List<String> tags;
    private final java.util.List<String> landmarks;
    private final String suggestedTripTitle;
    private final Integer suggestedDays;
}
