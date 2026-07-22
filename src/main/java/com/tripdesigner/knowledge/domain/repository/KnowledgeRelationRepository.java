package com.tripdesigner.knowledge.domain.repository;

import com.tripdesigner.knowledge.domain.KnowledgeRelation;

import java.util.List;

/**
 * 知识关系仓储接口。
 *
 * 定义知识实体间关系的持久化操作。
 * 实现在 infrastructure 层（KnowledgeRelationRepositoryImpl），使用 MyBatis Plus。
 */
public interface KnowledgeRelationRepository {

    /** 按起始实体查询关系列表 */
    List<KnowledgeRelation> findByFromEntity(String fromEntityType, Long fromEntityId);

    /** 按目标实体查询关系列表 */
    List<KnowledgeRelation> findByToEntity(String toEntityType, Long toEntityId);

    /** 按关系类型查询 */
    List<KnowledgeRelation> findByRelationType(String relationType);

    /** 保存（新增或更新） */
    KnowledgeRelation save(KnowledgeRelation relation);
}
