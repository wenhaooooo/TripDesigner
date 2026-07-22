package com.tripdesigner.ai.api;

import com.tripdesigner.ai.tool.TimeTool;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 冒烟测试控制器。
 *
 * 提供两个测试端点用于验证 AI 基础设施是否正常：
 * - /ai/smoke: 验证 LLM 模型连接（基础通信）
 * - /ai/tool-smoke: 验证 Function Calling 工具注册和执行链路
 *
 * 这两个接口无需认证，方便开发阶段快速排查 AI 配置问题。
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiSmokeController {

    private final ChatClient chatClient;
    private final TimeTool timeTool;

    /**
     * 基础冒烟测试。
     * 直接调用配置好的 LLM 模型，验证 API Key、网络连接、模型响应是否正常。
     *
     * @param prompt 用户输入提示（默认 "Say hello in one short sentence."）
     * @return LLM 模型的文本回复
     */
    @GetMapping("/smoke")
    public Map<String, String> smoke(@RequestParam(defaultValue = "Say hello in one short sentence.") String prompt) {
        String reply = chatClient.prompt(prompt).call().content();
        return Map.of("reply", reply == null ? "" : reply);
    }

    /**
     * Function Calling 冒烟测试。
     * 让 LLM 调用 TimeTool 工具获取当前时间，
     * 验证 Spring AI 的 Tool 注册和调用链路是否完整。
     *
     * @return LLM 的回复（应包含当前时间信息）
     */
    @GetMapping("/tool-smoke")
    public Map<String, String> toolSmoke() {
        String reply = chatClient.prompt("What time is it right now? Use the provided tool.")
                .tools(timeTool)
                .call()
                .content();
        return Map.of("reply", reply == null ? "" : reply);
    }
}
