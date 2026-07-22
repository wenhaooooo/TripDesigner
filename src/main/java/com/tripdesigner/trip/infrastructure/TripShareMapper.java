package com.tripdesigner.trip.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 行程分享 MyBatis Mapper 接口。
 * 对应 trip_shares 表。
 */
@Mapper
public interface TripShareMapper extends BaseMapper<TripSharePO> {
}
