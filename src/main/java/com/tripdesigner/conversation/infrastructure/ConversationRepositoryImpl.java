package com.tripdesigner.conversation.infrastructure;
/**
 * 对话仓储实现。
 * 使用 MyBatis Plus 实现持久化。
 * 按用户 ID 查询时按更新时间降序排列。
 */

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tripdesigner.conversation.domain.Conversation;
import com.tripdesigner.conversation.domain.ConversationRepository;
import com.tripdesigner.conversation.domain.ConversationStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ConversationRepositoryImpl implements ConversationRepository {
    private final ConversationMapper mapper;

    public ConversationRepositoryImpl(ConversationMapper mapper) { this.mapper = mapper; }

    @Override
    public Conversation save(Conversation conversation) {
        ConversationPO po = toPO(conversation);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    @Override
    public Optional<Conversation> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    @Override
    public List<Conversation> findByUserId(Long userId) {
        return mapper.selectList(
                Wrappers.<ConversationPO>lambdaQuery()
                        .eq(ConversationPO::getUserId, userId)
                        .orderByDesc(ConversationPO::getCreatedAt))
                .stream()
                .map(this::fromPO)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private ConversationPO toPO(Conversation c) {
        ConversationPO po = new ConversationPO();
        po.setId(c.getId());
        po.setUserId(c.getUserId());
        po.setTitle(c.getTitle());
        po.setStatus(c.getStatus() != null ? c.getStatus().getCode() : null);
        po.setLastMessageAt(c.getLastMessageAt());
        po.setVersion(c.getVersion());
        return po;
    }

    private Conversation fromPO(ConversationPO po) {
        return Conversation.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .title(po.getTitle())
                .status(po.getStatus() != null ? ConversationStatus.of(po.getStatus()) : null)
                .lastMessageAt(po.getLastMessageAt())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .version(po.getVersion())
                .build();
    }
}
