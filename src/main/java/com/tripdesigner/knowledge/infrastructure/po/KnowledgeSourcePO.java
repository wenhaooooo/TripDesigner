package com.tripdesigner.knowledge.infrastructure.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.Instant;

/**
 * 知识数据源持久化对象，映射至 kb_knowledge_sources 表。
 * 记录抓取来源、状态与增量同步信息。
 */
@Data
@TableName("kb_knowledge_sources")
public class KnowledgeSourcePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sourceType;

    private String sourceUrl;

    private String sourceId;

    private String entityType;

    private Long entityId;

    private String rawContent;

    private String contentHash;

    private Instant fetchedAt;

    private String etag;

    private String lastModified;

    private String status;

    private String errorMessage;

    private Integer retryCount;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;

    @Version
    private Integer version;
}
