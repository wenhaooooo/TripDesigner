package com.tripdesigner.trip.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 活动 MyBatis Mapper 接口。
 * 对应 trip_activities 表。
 */
@Mapper
public interface TripActivityMapper extends BaseMapper<TripActivityPO> {
}
