package com.tripdesigner.knowledge.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wikipedia 内容解析器。
 *
 * <p>将 Wikipedia 页面的 wikitext 或 HTML 内容解析为结构化数据：
 * <ul>
 *   <li>summary — 页面摘要</li>
 *   <li>fullText — 清洗后的完整文本</li>
 *   <li>coordinates — 坐标（latitude, longitude）</li>
 *   <li>categories — 分类列表</li>
 * </ul>
 *
 * <p>解析逻辑：
 * <ol>
 *   <li>提取首段作为摘要</li>
 *   <li>清理 wikitext/HTML 标记</li>
 *   <li>提取坐标信息（{@code {{coord|lat|lon}}} 模板）</li>
 *   <li>提取分类标签</li>
 * </ol>
 */
@Slf4j
@Component
public class WikipediaParser implements DataParser {

    /** 坐标模板正则：{{coord|lat|lon|...}} */
    private static final Pattern COORD_PATTERN = Pattern.compile(
            "\\{\\{coord\\|([\\d.]+)\\|([\\d.]+)",
            Pattern.CASE_INSENSITIVE
    );

    /** 分类标签正则 */
    private static final Pattern CATEGORY_PATTERN = Pattern.compile(
            "\\[\\[Category:([^\\]]+)\\]\\]"
    );

    /** 模板正则 */
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile(
            "\\{\\{[^}]*\\}\\}"
    );

    /** 内部链接正则 */
    private static final Pattern INTERNAL_LINK_PATTERN = Pattern.compile(
            "\\[\\[([^\\]|\\]]+)(?:\\|([^\\]]+))?\\]\\]"
    );

    /** HTML 标签正则 */
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    /** 引用标签正则 */
    private static final Pattern REF_PATTERN = Pattern.compile(
            "<ref[^>]*>.*?</ref>",
            Pattern.DOTALL
    );

    /** 引用标记正则 */
    private static final Pattern REFERENCE_MARKER_PATTERN = Pattern.compile(
            "\\[\\d+\\]"
    );

    /** URL 正则 */
    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[^\\s\\]]+"
    );

    @Override
    public Map<String, Object> parse(String rawContent, String sourceType) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (rawContent == null || rawContent.isBlank()) {
            log.debug("[WikipediaParser] Empty content, returning empty result");
            return result;
        }

        // 提取坐标
        double[] coords = extractCoordinates(rawContent);
        if (coords != null) {
            result.put("latitude", coords[0]);
            result.put("longitude", coords[1]);
        }

        // 提取分类
        List<String> categories = extractCategories(rawContent);
        result.put("categories", categories);

        // 清理并提取全文
        String cleanedText = cleanContent(rawContent);
        result.put("fullText", cleanedText);

        // 提取摘要（第一个段落）
        String summary = extractSummary(cleanedText);
        result.put("summary", summary);

        log.debug("[WikipediaParser] Parsed content, summary length: {}, categories: {}",
                summary.length(), categories.size());
        return result;
    }

    @Override
    public boolean supports(String sourceType) {
        return "WIKIPEDIA".equalsIgnoreCase(sourceType)
                || "WIKIPEDIA_SUMMARY".equalsIgnoreCase(sourceType)
                || "WIKIPEDIA_FULL".equalsIgnoreCase(sourceType);
    }

    /**
     * 从 wikitext 中提取坐标信息。
     *
     * @param content wikitext 内容
     * @return [latitude, longitude]；未找到时返回 null
     */
    private double[] extractCoordinates(String content) {
        Matcher matcher = COORD_PATTERN.matcher(content);
        if (matcher.find()) {
            try {
                double lat = Double.parseDouble(matcher.group(1));
                double lon = Double.parseDouble(matcher.group(2));
                return new double[]{lat, lon};
            } catch (NumberFormatException e) {
                log.debug("[WikipediaParser] Failed to parse coordinates: {}", e.getMessage());
            }
        }
        return null;
    }

    /**
     * 提取分类标签。
     */
    private List<String> extractCategories(String content) {
        List<String> categories = new ArrayList<>();
        Matcher matcher = CATEGORY_PATTERN.matcher(content);
        while (matcher.find()) {
            categories.add(matcher.group(1).trim());
        }
        return categories;
    }

    /**
     * 从清洗后的文本中提取第一段作为摘要。
     */
    private String extractSummary(String cleanedText) {
        if (cleanedText == null || cleanedText.isBlank()) {
            return "";
        }

        // 按双换行分段，取第一个非空段落
        String[] paragraphs = cleanedText.split("\\n\\n+");
        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (!trimmed.isEmpty() && trimmed.length() > 20) {
                return trimmed;
            }
        }

        // 如果没有找到合适的段落，返回前 500 字符
        return cleanedText.length() > 500 ? cleanedText.substring(0, 500) : cleanedText;
    }

    /**
     * 清理 wikitext/HTML 内容为纯文本。
     */
    private String cleanContent(String content) {
        String result = content;

        // 移除引用标签
        result = REF_PATTERN.matcher(result).replaceAll("");
        // 移除 HTML 标签
        result = HTML_TAG_PATTERN.matcher(result).replaceAll("");
        // 移除模板
        result = TEMPLATE_PATTERN.matcher(result).replaceAll("");
        // 替换内部链接
        result = replaceInternalLinks(result);
        // 移除 URL
        result = URL_PATTERN.matcher(result).replaceAll("");
        // 移除引用标记
        result = REFERENCE_MARKER_PATTERN.matcher(result).replaceAll("");
        // 移除分类标签
        result = CATEGORY_PATTERN.matcher(result).replaceAll("");
        // 规范化空白
        result = result.replaceAll("\\s+", " ").trim();

        return result;
    }

    /**
     * 将 [[link]] 或 [[link|display]] 替换为显示文本。
     */
    private String replaceInternalLinks(String text) {
        Matcher matcher = INTERNAL_LINK_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String display = matcher.group(2);
            String link = matcher.group(1);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(display != null ? display : link));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
