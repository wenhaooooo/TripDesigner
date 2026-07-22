package com.tripdesigner.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tripdesigner.knowledge.infrastructure.po.CountryPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 国家 MyBatis Mapper 接口，对应 kb_countries 表。
 */
@Mapper
public interface CountryMapper extends BaseMapper<CountryPO> {
}
