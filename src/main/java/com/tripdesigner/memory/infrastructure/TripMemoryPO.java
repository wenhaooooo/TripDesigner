package com.tripdesigner.memory.infrastructure;
/**
 * 旅行记忆数据库持久化对象。
 * 映射至 trip_memories 表。
 */

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("trip_memories")
public class TripMemoryPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long tripId;
    private String memoryType;
    private String content;
    private String tags;
    private Instant createdAt;
}
