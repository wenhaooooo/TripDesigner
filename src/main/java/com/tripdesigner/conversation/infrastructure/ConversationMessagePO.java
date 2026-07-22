package com.tripdesigner.conversation.infrastructure;
/**
 * 对话消息数据库持久化对象。
 * 映射至 conversation_messages 表。
 * metadata 字段存储 JSON 格式的附加数据。
 */

import com.baomidou.mybatisplus.annotation.*;
import com.tripdesigner.common.typehandler.JsonbStringTypeHandler;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("conversation_messages")
public class ConversationMessagePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private Long userId;
    private String role;
    private String content;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String metadata;
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
    @Version
    private Integer version;
}
