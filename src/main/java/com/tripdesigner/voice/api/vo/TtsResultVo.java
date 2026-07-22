package com.tripdesigner.voice.api.vo;

import lombok.Builder;
import lombok.Getter;

/**
 * TTS 合成结果 VO。
 * 由于不依赖外部 TTS 服务，audioData 为 Base64 编码的简单提示音，
 * 前端可使用浏览器原生 SpeechSynthesis API 直接合成语音。
 */
@Getter
@Builder
public class TtsResultVo {
    private final String text;
    private final String lang;
    private final Integer rate;
    private final Integer pitch;
    private final String suggestion;

    public static TtsResultVo of(String text, String lang) {
        return TtsResultVo.builder()
                .text(text)
                .lang(lang != null ? lang : "zh-CN")
                .rate(1)
                .pitch(1)
                .suggestion("使用浏览器 SpeechSynthesis API 合成语音")
                .build();
    }
}
