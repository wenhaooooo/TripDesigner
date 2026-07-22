package com.tripdesigner.knowledge.domain;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * 知识关系领域实体 —— 描述知识库实体之间的关联。
 *
 * 例如：城市→国家的 BELONGS_TO 关系、景点→城市的 LOCATED_IN 关系。
 * metadata 字段在持久层以 JSONB 存储。
 * 不可变对象，通过 Builder 构造。
 */
@Getter
@Builder
public class KnowledgeRelation {

    /** 主键 ID */
    private Long id;

    /** 起始实体类型 */
    private String fromEntityType;

    /** 起始实体 ID */
    private Long fromEntityId;

    /** 目标实体类型 */
    private String toEntityType;

    /** 目标实体 ID */
    private Long toEntityId;

    /** 关系类型，如 BELONGS_TO、LOCATED_IN、SIMILAR_TO */
    private String relationType;

    /** 关系权重（0-1） */
    private BigDecimal weight;

    /** 附加元数据（JSONB） */
    private Map<String, Object> metadata;

    /** 数据来源标识 */
    private String source;

    /** 创建时间 */
    private Instant createdAt;

    /** 更新时间 */
    private Instant updatedAt;

    /** 乐观锁版本号 */
    private Integer version;
}
