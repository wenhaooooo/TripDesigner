package com.tripdesigner.knowledge.domain.repository;

import com.tripdesigner.knowledge.domain.KnowledgeSource;

import java.util.List;
import java.util.Optional;

/**
 * 知识数据源仓储接口。
 *
 * 定义数据源抓取记录的持久化操作，支持增量同步与失败重试。
 * 实现在 infrastructure 层（KnowledgeSourceRepositoryImpl），使用 MyBatis Plus。
 */
public interface KnowledgeSourceRepository {

    /** 按主键查询 */
    Optional<KnowledgeSource> findById(Long id);

    /** 按来源类型与源 ID 查询，用于去重 */
    Optional<KnowledgeSource> findBySourceTypeAndSourceId(String sourceType, String sourceId);

    /** 保存（新增或更新） */
    KnowledgeSource save(KnowledgeSource source);

    /** 查询待处理（PENDING）的数据源记录，按创建时间升序 */
    List<KnowledgeSource> findPending(int limit);
}
