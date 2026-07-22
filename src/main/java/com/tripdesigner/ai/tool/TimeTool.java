package com.tripdesigner.ai.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 时间工具 —— 提供给 LLM 调用的 Function Calling 工具。
 *
 * 使用 Spring AI 的 @Tool 注解注册为 AI 可调用的工具函数。
 * LLM 在回答用户关于时间/日期的问题时，会自动调用此工具获取准确时间。
 *
 * 这是一个最小的工具示例，展示了 Spring AI Function Calling 的实现模式。
 */
@Component
public class TimeTool {

    /**
     * 获取当前 UTC 时间。
     * @Tool 注解的 description 字段会被作为工具描述注入 LLM 的 System Prompt 中，
     * LLM 根据描述决定何时调用此工具。
     *
     * @return ISO-8601 格式的当前 UTC 时间字符串
     */
    @Tool(description = "Get the current UTC time in ISO-8601 format. Use when the user asks for the current time or date.")
    public String getCurrentTime() {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now().atZone(ZoneId.of("UTC")));
    }
}
