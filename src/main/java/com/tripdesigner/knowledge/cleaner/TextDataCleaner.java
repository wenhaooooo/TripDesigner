package com.tripdesigner.knowledge.cleaner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 文本数据清洗器实现。
 *
 * <p>清洗原始文本中的各种标记和噪声，输出纯净文本：
 * <ul>
 *   <li>移除 HTML 标签（{@code <tag>...</tag>}）</li>
 *   <li>移除 Wiki 模板（{@code {{template}}}）</li>
 *   <li>移除 Wiki 链接标记，保留显示文本（{@code [[link|display]] → display}）</li>
 *   <li>移除加粗（{@code '''bold'''}）和斜体（{@code ''italic''}）标记</li>
 *   <li>移除引用标记（{@code [1]}, {@code [2]} 等）</li>
 *   <li>移除 URL（{@code http://...}）</li>
 *   <li>规范化空白字符（多个空格/换行压缩为单个）</li>
 * </ul>
 *
 * <p>同时提供元数据清洗：对元数据 Map 中的字符串值执行 trim 和空值过滤。
 */
@Slf4j
@Component
public class TextDataCleaner implements DataCleaner {

    /** HTML 标签正则 */
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    /** HTML 实体正则 */
    private static final Pattern HTML_ENTITY_PATTERN = Pattern.compile("&[a-zA-Z]+;|&#\\d+;");

    /** Wiki 模板正则：{{...}} */
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{[^}]*\\}\\}");

    /** Wiki 内部链接正则：[[link]] 或 [[link|display]] */
    private static final Pattern INTERNAL_LINK_PATTERN = Pattern.compile(
            "\\[\\[([^\\]|\\]]+)(?:\\|([^\\]]+))?\\]\\]"
    );

    /** Wiki 外部链接正则：[http://example.com text] */
    private static final Pattern EXTERNAL_LINK_PATTERN = Pattern.compile(
            "\\[https?://[^\\s]+\\s([^\\]]+)\\]"
    );

    /** 裸 URL 正则 */
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s\\]]+");

    /** 加粗标记正则 */
    private static final Pattern BOLD_PATTERN = Pattern.compile("'''");

    /** 斜体标记正则 */
    private static final Pattern ITALIC_PATTERN = Pattern.compile("''");

    /** 引用标签正则：<ref>...</ref> */
    private static final Pattern REF_TAG_PATTERN = Pattern.compile(
            "<ref[^>]*>.*?</ref>",
            Pattern.DOTALL
    );

    /** 自闭合引用正则：<ref .../> */
    private static final Pattern REF_SELF_CLOSE_PATTERN = Pattern.compile(
            "<ref[^>]*/>"
    );

    /** 引用标记正则：[1], [2], [citation needed] 等 */
    private static final Pattern REFERENCE_MARKER_PATTERN = Pattern.compile(
            "\\[(?:\\d+|citation needed)\\]"
    );

    /** 多余空白正则 */
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /** 标题标记正则：= Title = */
    private static final Pattern HEADING_PATTERN = Pattern.compile(
            "^={1,6}\\s*(.+?)\\s*={1,6}$",
            Pattern.MULTILINE
    );

    @Override
    public String clean(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        String result = rawText;

        // 移除引用标签
        result = REF_TAG_PATTERN.matcher(result).replaceAll("");
        result = REF_SELF_CLOSE_PATTERN.matcher(result).replaceAll("");

        // 移除 HTML 标签
        result = HTML_TAG_PATTERN.matcher(result).replaceAll("");

        // 移除 HTML 实体
        result = replaceHtmlEntities(result);

        // 移除 Wiki 模板
        result = TEMPLATE_PATTERN.matcher(result).replaceAll("");

        // 替换内部链接为显示文本
        result = replaceInternalLinks(result);

        // 替换外部链接为显示文本
        result = replaceExternalLinks(result);

        // 移除裸 URL
        result = URL_PATTERN.matcher(result).replaceAll("");

        // 移除加粗和斜体标记
        result = BOLD_PATTERN.matcher(result).replaceAll("");
        result = ITALIC_PATTERN.matcher(result).replaceAll("");

        // 移除引用标记
        result = REFERENCE_MARKER_PATTERN.matcher(result).replaceAll("");

        // 移除标题标记（保留标题文本）
        result = HEADING_PATTERN.matcher(result).replaceAll("$1");

        // 规范化空白
        result = WHITESPACE_PATTERN.matcher(result).replaceAll(" ").trim();

        return result;
    }

    @Override
    public Map<String, Object> cleanMetadata(Map<String, Object> metadata) {
        if (metadata == null) {
            return new HashMap<>();
        }

        Map<String, Object> cleaned = new HashMap<>();
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = entry.getKey() != null ? entry.getKey().trim() : null;
            Object value = entry.getValue();

            if (value instanceof String s) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    cleaned.put(key, trimmed);
                }
            } else if (value != null) {
                cleaned.put(key, value);
            }
        }

        return cleaned;
    }

    /**
     * 将 [[link]] 或 [[link|display]] 替换为显示文本。
     */
    private String replaceInternalLinks(String text) {
        var matcher = INTERNAL_LINK_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String display = matcher.group(2);
            String link = matcher.group(1);
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(
                    display != null ? display : link));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 将 [http://example.com text] 替换为显示文本。
     */
    private String replaceExternalLinks(String text) {
        var matcher = EXTERNAL_LINK_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(matcher.group(1)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 替换常见 HTML 实体为对应字符。
     */
    private String replaceHtmlEntities(String text) {
        String result = HTML_ENTITY_PATTERN.matcher(text).replaceAll(match -> {
            String entity = match.group();
            return switch (entity) {
                case "&amp;" -> "&";
                case "&lt;" -> "<";
                case "&gt;" -> ">";
                case "&quot;" -> "\"";
                case "&apos;", "&#39;" -> "'";
                case "&nbsp;" -> " ";
                default -> "";
            };
        });
        return result;
    }
}
