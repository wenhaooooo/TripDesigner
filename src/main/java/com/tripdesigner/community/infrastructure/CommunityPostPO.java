package com.tripdesigner.community.infrastructure;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.tripdesigner.common.typehandler.JsonbStringTypeHandler;
import lombok.Data;

import java.time.Instant;

@Data
@TableName(value = "community_posts", autoResultMap = true)
public class CommunityPostPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String destination;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String tags;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String mediaUrls;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
    @Version
    private Integer version;
}
