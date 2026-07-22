package com.tripdesigner.memory.infrastructure;
/**
 * 用户偏好 MyBatis Mapper 接口。
 * 对应 user_preferences 表。
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserPreferenceMapper extends BaseMapper<UserPreferencePO> {
}
