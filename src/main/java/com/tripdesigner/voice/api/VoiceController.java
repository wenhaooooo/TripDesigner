package com.tripdesigner.voice.api;

import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.Result;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.voice.api.vo.TtsResultVo;
import com.tripdesigner.voice.application.VoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 语音 REST API。
 *
 * - POST /voice/tts   文本预处理并返回 TTS 合成参数（前端使用 SpeechSynthesis 合成）
 *
 * STT 由前端使用浏览器原生 SpeechRecognition 完成，无需后端接口。
 */
@Tag(name = "Voice", description = "语音交互（TTS/STT）")
@RestController
@RequestMapping("/voice")
@RequiredArgsConstructor
public class VoiceController {

    private final VoiceService voiceService;

    @Operation(summary = "文本转语音预处理", description = "返回前端使用 SpeechSynthesis API 合成所需的参数")
    @PostMapping("/tts")
    public Result<TtsResultVo> prepareTts(@RequestBody Map<String, String> body) {
        requireAuth();
        String text = body.get("text");
        String lang = body.getOrDefault("lang", "zh-CN");
        return Result.success(voiceService.prepareTts(text, lang));
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }
}
