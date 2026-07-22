package com.tripdesigner.knowledge.chunker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 语义分块器实现。
 *
 * <p>将长文本按语义边界切分为适合向量化的分块，每个分块控制在 600-800 token 以内。
 *
 * <p>分块策略：
 * <ol>
 *   <li>按章节标题切分（Markdown {@code ##} 或 wikitext {@code ==}）</li>
 *   <li>如果单个章节超过 {@link #MAX_TOKENS}（800 token），再按段落（双换行）切分</li>
 *   <li>如果单个段落仍超过上限，按句子切分并合并到上限</li>
 * </ol>
 *
 * <p>Token 估算：{@code tokens ≈ characters / 4}（英文近似值）。
 *
 * <p>每个分块包含：
 * <ul>
 *   <li>chunkType — 章节名称（如 "Introduction"、"See"、"Eat"）</li>
 *   <li>chunkIndex — 序号（从 0 开始）</li>
 *   <li>title — 分块标题（页面标题 + 章节名）</li>
 *   <li>content — 分块文本内容</li>
 *   <li>tokenCount — 预估 token 数</li>
 * </ul>
 */
@Slf4j
@Component
public class SemanticChunker implements Chunker {

    /** 单个分块的最大 token 数 */
    private static final int MAX_TOKENS = 800;

    /** 单个分块的最小 token 数（避免产生过小的碎片） */
    private static final int MIN_TOKENS = 100;

    /** Token 估算系数：1 token ≈ 4 字符 */
    private static final int CHARS_PER_TOKEN = 4;

    /** Markdown 章节标题正则：## Title */
    private static final Pattern MARKDOWN_HEADING_PATTERN = Pattern.compile(
            "^(#{1,6})\\s+(.+)$",
            Pattern.MULTILINE
    );

    /** wikitext 章节标题正则：== Title == */
    private static final Pattern WIKITEXT_HEADING_PATTERN = Pattern.compile(
            "^(={2,6})\\s*(.+?)\\s*\\1\\s*$",
            Pattern.MULTILINE
    );

    /** 句子切分正则 */
    private static final Pattern SENTENCE_PATTERN = Pattern.compile(
            "[^.!?]+[.!?]+\\s*|[^.!?]+$"
    );

    @Override
    public List<ChunkResult> chunk(String content, String title, String entityType, Long entityId) {
        List<ChunkResult> chunks = new ArrayList<>();

        if (content == null || content.isBlank()) {
            log.debug("[SemanticChunker] Empty content, returning empty chunks");
            return chunks;
        }

        log.debug("[SemanticChunker] Chunking content for title: {}, entityType: {}", title, entityType);

        // 按章节切分
        List<Section> sections = splitBySections(content);

        int chunkIndex = 0;
        for (Section section : sections) {
            int sectionTokens = estimateTokens(section.content);

            if (sectionTokens <= MAX_TOKENS) {
                // 章节未超限，直接作为一个分块
                if (sectionTokens >= MIN_TOKENS || chunks.isEmpty()) {
                    chunks.add(createChunk(section, chunkIndex++, title));
                }
            } else {
                // 章节超限，按段落切分
                List<String> paragraphChunks = splitByParagraphs(section.content, MAX_TOKENS);
                for (String paragraphChunk : paragraphChunks) {
                    if (!paragraphChunk.isBlank()) {
                        Section subSection = new Section(section.name, paragraphChunk.trim());
                        chunks.add(createChunk(subSection, chunkIndex++, title));
                    }
                }
            }
        }

        log.info("[SemanticChunker] Created {} chunks for title: {}", chunks.size(), title);
        return chunks;
    }

    @Override
    public int getMaxTokens() {
        return MAX_TOKENS;
    }

    /**
     * 按章节标题切分内容。
     * 支持 Markdown（##）和 wikitext（==）两种标题格式。
     *
     * @param content 原始内容
     * @return 章节列表
     */
    private List<Section> splitBySections(String content) {
        List<Section> sections = new ArrayList<>();

        // 尝试 Markdown 标题
        Pattern headingPattern = MARKDOWN_HEADING_PATTERN;
        Matcher mdMatcher = MARKDOWN_HEADING_PATTERN.matcher(content);
        Matcher wikiMatcher = WIKITEXT_HEADING_PATTERN.matcher(content);

        // 选择匹配数更多的标题格式
        int mdCount = countMatches(mdMatcher);
        int wikiCount = countMatches(wikiMatcher);
        if (wikiCount > mdCount) {
            headingPattern = WIKITEXT_HEADING_PATTERN;
        }

        Matcher matcher = headingPattern.matcher(content);
        int lastEnd = 0;
        String currentName = "Introduction";
        StringBuilder currentContent = new StringBuilder();

        while (matcher.find()) {
            currentContent.append(content, lastEnd, matcher.start());
            String trimmed = currentContent.toString().trim();
            if (!trimmed.isEmpty()) {
                sections.add(new Section(currentName, trimmed));
            }

            currentName = matcher.group(2).trim();
            currentContent = new StringBuilder();
            lastEnd = matcher.end();
        }

        // 最后一个章节
        currentContent.append(content, lastEnd, content.length());
        String trimmed = currentContent.toString().trim();
        if (!trimmed.isEmpty()) {
            sections.add(new Section(currentName, trimmed));
        }

        // 如果没有找到任何标题，整个内容作为一个章节
        if (sections.isEmpty() && !content.isBlank()) {
            sections.add(new Section("Content", content.trim()));
        }

        return sections;
    }

    /**
     * 按段落切分超长章节，合并段落直到接近 token 上限。
     *
     * @param content  章节内容
     * @param maxTokens 每个分块的最大 token 数
     * @return 分块文本列表
     */
    private List<String> splitByParagraphs(String content, int maxTokens) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = content.split("\\n\\n+");
        StringBuilder currentChunk = new StringBuilder();
        int currentTokens = 0;

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            int paraTokens = estimateTokens(trimmed);

            if (paraTokens > maxTokens) {
                // 单个段落就超限，按句子切分
                if (currentTokens > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                    currentTokens = 0;
                }
                chunks.addAll(splitBySentences(trimmed, maxTokens));
            } else if (currentTokens + paraTokens > maxTokens) {
                // 当前分块加上该段落会超限，先保存当前分块
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder(trimmed).append("\n\n");
                currentTokens = paraTokens;
            } else {
                currentChunk.append(trimmed).append("\n\n");
                currentTokens += paraTokens;
            }
        }

        if (currentTokens > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * 按句子切分超长段落，合并句子直到接近 token 上限。
     *
     * @param paragraph 段落文本
     * @param maxTokens 每个分块的最大 token 数
     * @return 分块文本列表
     */
    private List<String> splitBySentences(String paragraph, int maxTokens) {
        List<String> chunks = new ArrayList<>();
        Matcher matcher = SENTENCE_PATTERN.matcher(paragraph);
        StringBuilder currentChunk = new StringBuilder();
        int currentTokens = 0;

        while (matcher.find()) {
            String sentence = matcher.group().trim();
            if (sentence.isEmpty()) {
                continue;
            }

            int sentenceTokens = estimateTokens(sentence);

            if (currentTokens + sentenceTokens > maxTokens && currentTokens > 0) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder(sentence);
                currentTokens = sentenceTokens;
            } else {
                currentChunk.append(sentence);
                currentTokens += sentenceTokens;
            }
        }

        if (currentTokens > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * 创建分块结果。
     */
    private ChunkResult createChunk(Section section, int chunkIndex, String pageTitle) {
        String chunkTitle = pageTitle != null ? pageTitle + " - " + section.name : section.name;
        int tokenCount = estimateTokens(section.content);
        return new ChunkResult(section.name, chunkIndex, chunkTitle, section.content, tokenCount);
    }

    /**
     * 估算文本的 token 数（tokens ≈ characters / 4）。
     *
     * @param text 文本内容
     * @return 预估 token 数
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Math.max(1, text.length() / CHARS_PER_TOKEN);
    }

    /**
     * 计算正则匹配次数。
     */
    private int countMatches(Matcher matcher) {
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        matcher.reset();
        return count;
    }

    /**
     * 章节内部表示。
     *
     * @param name    章节名称
     * @param content 章节内容
     */
    private record Section(String name, String content) {}
}
