package com.tripdesigner.knowledge.cleaner;

import java.util.Map;

/**
 * 数据清洗器接口。
 *
 * 负责将解析后的原始文本清洗为纯净的文本内容，去除 HTML 标签、
 * Wiki 标记、引用标记、URL 等噪声，同时清洗元数据字段。
 *
 * <p>清洗后的文本将传递给 {@link com.tripdesigner.knowledge.chunker.Chunker} 进行语义分块。
 */
public interface DataCleaner {

    /**
     * 清洗原始文本，去除标记和噪声。
     *
     * @param rawText 原始文本（可能包含 HTML、Wiki 标记等）
     * @return 清洗后的纯文本
     */
    String clean(String rawText);

    /**
     * 清洗元数据 Map，标准化字段值。
     *
     * @param metadata 原始元数据
     * @return 清洗后的元数据
     */
    Map<String, Object> cleanMetadata(Map<String, Object> metadata);
}
