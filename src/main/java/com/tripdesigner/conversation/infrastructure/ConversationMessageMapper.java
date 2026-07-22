package com.tripdesigner.conversation.infrastructure;
/**
 * 对话消息 MyBatis Mapper 接口。
 * 对应 conversation_messages 表。
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessagePO> {
}
