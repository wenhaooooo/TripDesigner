package com.tripdesigner.conversation.infrastructure;
/**
 * 对话数据库持久化对象。
 * 映射至 conversations 表。
 */

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("conversations")
public class ConversationPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private Short status;
    private Instant lastMessageAt;
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
    @Version
    private Integer version;
}
