package com.tripdesigner.user.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 MyBatis Mapper 接口。
 * 对应 users 表。
 */
@Mapper
public interface UserMapper extends BaseMapper<UserPO> {
}