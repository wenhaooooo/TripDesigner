package com.tripdesigner.ai.trip.workflow;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.Instant;

/**
 * 工作流会话数据库持久化对象（PO）。
 *
 * 映射至 workflow_sessions 表。
 * status 字段存储 WorkflowStatus 枚举的 name() 字符串值。
 * 使用 MyBatis Plus 的自动填充功能管理 createdAt/updatedAt。
 */
@Data
@TableName("workflow_sessions")
public class WorkflowSessionPO {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的对话 ID */
    private Long conversationId;

    /** 用户 ID */
    private Long userId;

    /** 工作流状态（WorkflowStatus 名称） */
    private String status;

    /** 错误信息（工作流失败时填充） */
    private String errorMessage;

    /** 创建时间（MyBatis Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    /** 更新时间（MyBatis Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;

    /** 完成时间 */
    private Instant completedAt;
}
