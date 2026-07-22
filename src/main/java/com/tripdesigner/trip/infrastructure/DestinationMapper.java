package com.tripdesigner.trip.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 目的地 MyBatis Mapper 接口。
 * 对应 destinations 表。
 */
@Mapper
public interface DestinationMapper extends BaseMapper<DestinationPO> {
}
