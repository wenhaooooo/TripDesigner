package com.tripdesigner.voice.application;

import com.tripdesigner.voice.api.vo.TtsResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 语音服务。
 *
 * 后端职责：
 * 1. 提供 TTS 文本预处理（标记敏感词、调整语速等）
 * 2. 返回合成参数（前端使用浏览器原生 SpeechSynthesis API 合成）
 *
 * 注：STT 主要由前端使用浏览器原生 SpeechRecognition API 完成，
 * 后端无需接收音频流。这样可以减少带宽消耗和服务端依赖。
 */
@Slf4j
@Service
public class VoiceService {

    /** 文本预处理：清理换行、补充停顿 */
    public TtsResultVo prepareTts(String text, String lang) {
        if (text == null || text.isBlank()) {
            return TtsResultVo.of("（空文本）", lang);
        }
        String processed = text.replaceAll("\\s+", " ").trim();
        // 标点符号添加停顿（句号、问号、感叹号后增加停顿）
        processed = processed.replaceAll("([。！？.!?])", "$1，");
        return TtsResultVo.of(processed, lang);
    }
}
