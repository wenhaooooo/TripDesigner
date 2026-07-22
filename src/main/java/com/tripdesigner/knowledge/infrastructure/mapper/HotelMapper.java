package com.tripdesigner.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tripdesigner.knowledge.infrastructure.po.HotelPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 酒店 MyBatis Mapper 接口，对应 kb_hotels 表。
 */
@Mapper
public interface HotelMapper extends BaseMapper<HotelPO> {
}
