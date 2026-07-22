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
 * Wikivoyage wikitext 解析器。
 *
 * <p>将 Wikivoyage 页面的 wikitext 原文解析为结构化数据：
 * <ul>
 *   <li>title — 页面标题</li>
 *   <li>sections — 章节映射（章节名 → 章节内容），如 Introduction、Get in、See、Eat、Sleep</li>
 *   <li>categories — 分类列表</li>
 * </ul>
 *
 * <p>解析逻辑：
 * <ol>
 *   <li>按章节标题（{@code == Section ==}）拆分 wikitext</li>
 *   <li>清理 wikitext 标记：模板 {@code {{template}}}、链接 {@code [[link]]}、加粗 {@code '''bold'''}、斜体 {@code ''italic''}、引用 {@code <ref>...</ref>}</li>
 *   <li>提取分类标签 {@code [[Category:xxx]]}</li>
 * </ol>
 */
@Slf4j
@Component
public class WikivoyageParser implements DataParser {

    /** 章节标题正则：匹配 == Title == 或 === Subtitle === */
    private static final Pattern SECTION_PATTERN = Pattern.compile(
            "^(={2,6})\\s*(.+?)\\s*\\1\\s*$",
            Pattern.MULTILINE
    );

    /** 分类标签正则 */
    private static final Pattern CATEGORY_PATTERN = Pattern.compile(
            "\\[\\[Category:([^\\]]+)\\]\\]"
    );

    /** 模板正则：{{template}} 或 {{template|args}} */
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile(
            "\\{\\{[^}]*\\}\\}"
    );

    /** 内部链接正则：[[link]] 或 [[link|display]] */
    private static final Pattern INTERNAL_LINK_PATTERN = Pattern.compile(
            "\\[\\[([^\\]|\\]]+)(?:\\|([^\\]]+))?\\]\\]"
    );

    /** 外部链接正则：[http://example.com text] */
    private static final Pattern EXTERNAL_LINK_PATTERN = Pattern.compile(
            "\\[https?://[^\\s]+\\s([^\\]]+)\\]"
    );

    /** 加粗正则：'''bold''' */
    private static final Pattern BOLD_PATTERN = Pattern.compile("'''");

    /** 斜体正则：''italic'' */
    private static final Pattern ITALIC_PATTERN = Pattern.compile("''");

    /** HTML 标签正则 */
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    /** 引用标签正则：<ref>...</ref> */
    private static final Pattern REF_PATTERN = Pattern.compile(
            "<ref[^>]*>.*?</ref>",
            Pattern.DOTALL
    );

    /** 引用标记正则：[1], [2] 等 */
    private static final Pattern REFERENCE_MARKER_PATTERN = Pattern.compile(
            "\\[\\d+\\]"
    );

    @Override
    public Map<String, Object> parse(String rawContent, String sourceType) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (rawContent == null || rawContent.isBlank()) {
            log.debug("[WikivoyageParser] Empty content, returning empty result");
            return result;
        }

        // 提取分类
        List<String> categories = extractCategories(rawContent);
        result.put("categories", categories);

        // 移除分类标签后再解析章节
        String contentWithoutCategories = CATEGORY_PATTERN.matcher(rawContent).replaceAll("");

        // 按章节拆分
        Map<String, String> sections = extractSections(contentWithoutCategories);
        result.put("sections", sections);

        // 提取标题：如果第一个章节前有内容，将其作为 Introduction
        // 标题通常由爬虫提供，这里不额外提取

        // 合并所有章节内容作为全文
        StringBuilder fullText = new StringBuilder();
        for (Map.Entry<String, String> entry : sections.entrySet()) {
            fullText.append(entry.getKey()).append("\n").append(entry.getValue()).append("\n\n");
        }
        result.put("fullText", cleanWikitext(fullText.toString()));

        log.debug("[WikivoyageParser] Parsed {} sections, {} categories", sections.size(), categories.size());
        return result;
    }

    @Override
    public boolean supports(String sourceType) {
        return "WIKIVOYAGE".equalsIgnoreCase(sourceType);
    }

    /**
     * 按 wikitext 章节标题拆分内容。
     *
     * @param content wikitext 内容
     * @return 章节名 → 章节内容的有序 Map
     */
    private Map<String, String> extractSections(String content) {
        Map<String, String> sections = new LinkedHashMap<>();
        Matcher matcher = SECTION_PATTERN.matcher(content);

        int lastEnd = 0;
        String currentSection = "Introduction";
        StringBuilder currentContent = new StringBuilder();

        while (matcher.find()) {
            // 保存上一个章节的内容
            currentContent.append(content, lastEnd, matcher.start());
            sections.put(currentSection, cleanWikitext(currentContent.toString()));

            // 开始新章节
            currentSection = matcher.group(2).trim();
            currentContent = new StringBuilder();
            lastEnd = matcher.end();
        }

        // 保存最后一个章节
        currentContent.append(content, lastEnd, content.length());
        sections.put(currentSection, cleanWikitext(currentContent.toString()));

        return sections;
    }

    /**
     * 提取页面分类标签。
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
     * 清理 wikitext 标记，转换为纯文本。
     */
    private String cleanWikitext(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String result = text;

        // 移除引用标签
        result = REF_PATTERN.matcher(result).replaceAll("");
        // 移除 HTML 标签
        result = HTML_TAG_PATTERN.matcher(result).replaceAll("");
        // 移除模板 {{...}}
        result = TEMPLATE_PATTERN.matcher(result).replaceAll("");
        // 替换内部链接为显示文本
        result = replaceInternalLinks(result);
        // 替换外部链接为显示文本
        result = replaceExternalLinks(result);
        // 移除加粗标记
        result = BOLD_PATTERN.matcher(result).replaceAll("");
        // 移除斜体标记
        result = ITALIC_PATTERN.matcher(result).replaceAll("");
        // 移除引用标记 [1], [2]
        result = REFERENCE_MARKER_PATTERN.matcher(result).replaceAll("");
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

    /**
     * 将 [http://example.com text] 替换为显示文本。
     */
    private String replaceExternalLinks(String text) {
        Matcher matcher = EXTERNAL_LINK_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(1)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
