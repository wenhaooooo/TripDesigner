package com.tripdesigner.trip.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 行程 MyBatis Mapper 接口。
 * 对应 trips 表。
 */
@Mapper
public interface TripMapper extends BaseMapper<TripPO> {
}
