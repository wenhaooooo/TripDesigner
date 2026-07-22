package com.tripdesigner.community.infrastructure;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("community_favorites")
public class CommunityFavoritePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long postId;
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
}
