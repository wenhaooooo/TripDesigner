package com.tripdesigner.knowledge.infrastructure.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.tripdesigner.common.typehandler.JsonbStringTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 知识关系持久化对象，映射至 kb_knowledge_relations 表。
 * 描述知识实体之间的有向关系。metadata 为 JSONB 字段。
 */
@Data
@TableName(value = "kb_knowledge_relations", autoResultMap = true)
public class KnowledgeRelationPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fromEntityType;

    private Long fromEntityId;

    private String toEntityType;

    private Long toEntityId;

    private String relationType;

    private BigDecimal weight;

    /** JSONB：附加元数据 */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String metadata;

    private String source;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;

    @Version
    private Integer version;
}
