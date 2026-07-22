package com.tripdesigner.experience.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 体验 MyBatis Mapper 接口。
 * 对应 trip_experiences 表。
 */
@Mapper
public interface TripExperienceMapper extends BaseMapper<TripExperiencePO> {
}
