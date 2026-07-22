package com.tripdesigner.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tripdesigner.knowledge.infrastructure.po.PoiPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 景点（POI）MyBatis Mapper 接口，对应 kb_pois 表。
 */
@Mapper
public interface PoiMapper extends BaseMapper<PoiPO> {
}
