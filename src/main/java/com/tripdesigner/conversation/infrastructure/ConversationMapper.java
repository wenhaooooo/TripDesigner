package com.tripdesigner.conversation.infrastructure;
/**
 * 对话 MyBatis Mapper 接口。
 * 对应 conversations 表。
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMapper extends BaseMapper<ConversationPO> {
}
