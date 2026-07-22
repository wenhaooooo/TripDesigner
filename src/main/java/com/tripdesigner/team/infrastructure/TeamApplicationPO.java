package com.tripdesigner.team.infrastructure;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("team_applications")
public class TeamApplicationPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long teamId;
    private Long applicantId;
    private String message;
    private String status;
    private Instant processedAt;
    private Long processedBy;
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
    @Version
    private Integer version;
}
