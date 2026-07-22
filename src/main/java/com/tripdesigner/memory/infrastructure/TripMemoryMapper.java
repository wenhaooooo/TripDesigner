package com.tripdesigner.memory.infrastructure;
/**
 * 旅行记忆 MyBatis Mapper 接口。
 * 对应 trip_memories 表。
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TripMemoryMapper extends BaseMapper<TripMemoryPO> {
}
