package com.tripdesigner.knowledge.infrastructure.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.tripdesigner.common.typehandler.JsonbStringTypeHandler;
import lombok.Data;

import java.time.Instant;

/**
 * 知识分块持久化对象，映射至 kb_knowledge_chunks 表。
 * metadata 为 JSONB 字段。
 * <p>
 * 注意：embedding 字段（pgvector 向量）不在此 PO 中声明，
 * 由 Mapper 通过自定义 SQL 单独处理写入与检索。
 */
@Data
@TableName(value = "kb_knowledge_chunks", autoResultMap = true)
public class KnowledgeChunkPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String entityType;

    private Long entityId;

    private String chunkType;

    private Integer chunkIndex;

    private String title;

    private String content;

    private String contentHash;

    /** JSONB：附加元数据 */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String metadata;

    private String language;

    private Integer tokenCount;

    private String source;

    private String sourceId;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;

    @Version
    private Integer version;
}
