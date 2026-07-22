package com.tripdesigner.price.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 价格监测 MyBatis Mapper 接口。
 * 对应 price_monitors 表。
 */
@Mapper
public interface PriceMonitorMapper extends BaseMapper<PriceMonitorPO> {
}
