package com.tripdesigner.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tripdesigner.knowledge.infrastructure.po.RestaurantPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 餐厅 MyBatis Mapper 接口，对应 kb_restaurants 表。
 */
@Mapper
public interface RestaurantMapper extends BaseMapper<RestaurantPO> {
}
