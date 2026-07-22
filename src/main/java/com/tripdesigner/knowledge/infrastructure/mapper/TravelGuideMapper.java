package com.tripdesigner.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tripdesigner.knowledge.infrastructure.po.TravelGuidePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 旅行指南 MyBatis Mapper 接口，对应 kb_travel_guides 表。
 */
@Mapper
public interface TravelGuideMapper extends BaseMapper<TravelGuidePO> {
}
