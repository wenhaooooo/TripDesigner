package com.tripdesigner.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tripdesigner.knowledge.infrastructure.po.CityPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 城市 MyBatis Mapper 接口，对应 kb_cities 表。
 */
@Mapper
public interface CityMapper extends BaseMapper<CityPO> {
}
