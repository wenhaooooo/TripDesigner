package com.tripdesigner.trip.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 行程日 MyBatis Mapper 接口。
 * 对应 trip_days 表。
 */
@Mapper
public interface TripDayMapper extends BaseMapper<TripDayPO> {
}
