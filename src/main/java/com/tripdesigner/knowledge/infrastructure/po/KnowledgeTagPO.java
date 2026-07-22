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
 * 知识标签持久化对象，映射至 kb_knowledge_tags 表。
 * 通过 entityType + entityId 多态关联到任意知识实体。
 */
@Data
@TableName("kb_knowledge_tags")
public class KnowledgeTagPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String entityType;

    private Long entityId;

    private String tag;

    private String tagType;

    private String language;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;

    @Version
    private Integer version;
}
