package com.tripdesigner.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tripdesigner.knowledge.infrastructure.po.RoutePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 路线 MyBatis Mapper 接口，对应 kb_routes 表。
 */
@Mapper
public interface RouteMapper extends BaseMapper<RoutePO> {
}
