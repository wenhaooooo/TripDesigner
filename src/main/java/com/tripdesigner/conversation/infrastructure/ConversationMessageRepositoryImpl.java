package com.tripdesigner.conversation.infrastructure;
/**
 * 对话消息仓储实现。
 * 使用 MyBatis Plus 实现持久化。
 * 按对话 ID 查询时按创建时间升序排列。
 */

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tripdesigner.conversation.domain.ConversationMessage;
import com.tripdesigner.conversation.domain.ConversationMessageRepository;
import com.tripdesigner.conversation.domain.ConversationRole;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ConversationMessageRepositoryImpl implements ConversationMessageRepository {
    private final ConversationMessageMapper mapper;

    public ConversationMessageRepositoryImpl(ConversationMessageMapper mapper) { this.mapper = mapper; }

    @Override
    public ConversationMessage save(ConversationMessage message) {
        ConversationMessagePO po = toPO(message);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    @Override
    public Optional<ConversationMessage> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    @Override
    public List<ConversationMessage> findByConversationId(Long conversationId) {
        return mapper.selectList(
                Wrappers.<ConversationMessagePO>lambdaQuery()
                        .eq(ConversationMessagePO::getConversationId, conversationId)
                        .orderByAsc(ConversationMessagePO::getCreatedAt))
                .stream()
                .map(this::fromPO)
                .toList();
    }

    @Override
    public void deleteByConversationId(Long conversationId) {
        mapper.delete(Wrappers.<ConversationMessagePO>lambdaQuery()
                .eq(ConversationMessagePO::getConversationId, conversationId));
    }

    private ConversationMessagePO toPO(ConversationMessage m) {
        ConversationMessagePO po = new ConversationMessagePO();
        po.setId(m.getId());
        po.setConversationId(m.getConversationId());
        po.setUserId(m.getUserId());
        po.setRole(m.getRole() != null ? m.getRole().name() : null);
        po.setContent(m.getContent());
        po.setMetadata(m.getMetadata());
        po.setVersion(m.getVersion());
        return po;
    }

    private ConversationMessage fromPO(ConversationMessagePO po) {
        return ConversationMessage.builder()
                .id(po.getId())
                .conversationId(po.getConversationId())
                .userId(po.getUserId())
                .role(po.getRole() != null ? ConversationRole.valueOf(po.getRole()) : null)
                .content(po.getContent())
                .metadata(po.getMetadata())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .version(po.getVersion())
                .build();
    }
}
