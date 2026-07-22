package com.tripdesigner.team.infrastructure;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.tripdesigner.common.typehandler.JsonbStringTypeHandler;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@TableName(value = "travel_teams", autoResultMap = true)
public class TravelTeamPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long creatorId;
    private String title;
    private String description;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private String teamType;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String interests;
    private Integer maxMembers;
    private Integer currentMembers;
    private String genderRequirement;
    private Integer minAge;
    private Integer maxAge;
    private String contact;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
    @Version
    private Integer version;
}
