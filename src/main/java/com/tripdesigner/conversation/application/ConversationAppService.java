package com.tripdesigner.conversation.application;

import com.tripdesigner.ai.trip.workflow.WorkflowSessionRepository;
import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.conversation.api.dto.AddMessageRequest;
import com.tripdesigner.conversation.api.dto.CreateConversationRequest;
import com.tripdesigner.conversation.api.vo.ConversationMessageVo;
import com.tripdesigner.conversation.api.vo.ConversationVo;
import com.tripdesigner.conversation.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对话应用服务。
 *
 * 处理对话和消息的 CRUD 业务逻辑。
 * 所有操作都需要用户认证，且只能操作当前用户的对话。
 * 添加消息时自动更新对话的 lastMessageAt 时间戳。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationAppService {
    private final ConversationRepository convRepo;
    private final ConversationMessageRepository msgRepo;
    private final WorkflowSessionRepository workflowSessionRepo;

    @Transactional(readOnly = true)
    public List<ConversationVo> list() {
        UserContext ctx = requireAuth();
        return convRepo.findByUserId(ctx.userId()).stream()
                .map(ConversationVo::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ConversationVo create(CreateConversationRequest req) {
        UserContext ctx = requireAuth();
        Conversation conv = Conversation.create(ctx.userId(), req.getTitle());
        return ConversationVo.from(convRepo.save(conv));
    }

    @Transactional(readOnly = true)
    public ConversationVo get(Long convId) {
        UserContext ctx = requireAuth();
        Conversation conv = convRepo.findById(convId)
                .orElseThrow(() -> new BizException(ResultCode.CONV_NOT_FOUND));
        verifyOwner(conv.getUserId(), ctx.userId());
        return ConversationVo.from(conv);
    }

    @Transactional
    public ConversationVo update(Long convId, CreateConversationRequest req) {
        UserContext ctx = requireAuth();
        Conversation conv = convRepo.findById(convId)
                .orElseThrow(() -> new BizException(ResultCode.CONV_NOT_FOUND));
        verifyOwner(conv.getUserId(), ctx.userId());

        Conversation updated = conv.withUpdatedTitle(req.getTitle());
        return ConversationVo.from(convRepo.save(updated));
    }

    @Transactional
    public void delete(Long convId) {
        UserContext ctx = requireAuth();
        Conversation conv = convRepo.findById(convId)
                .orElseThrow(() -> new BizException(ResultCode.CONV_NOT_FOUND));
        verifyOwner(conv.getUserId(), ctx.userId());

        msgRepo.deleteByConversationId(convId);
        workflowSessionRepo.deleteByConversationId(convId);
        convRepo.deleteById(convId);
        log.info("[ConversationAppService] Deleted conversation: id={}, userId={}", convId, ctx.userId());
    }

    @Transactional(readOnly = true)
    public List<ConversationMessageVo> listMessages(Long convId) {
        UserContext ctx = requireAuth();
        Conversation conv = convRepo.findById(convId)
                .orElseThrow(() -> new BizException(ResultCode.CONV_NOT_FOUND));
        verifyOwner(conv.getUserId(), ctx.userId());

        return msgRepo.findByConversationId(convId).stream()
                .map(ConversationMessageVo::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ConversationMessageVo addMessage(Long convId, AddMessageRequest req) {
        UserContext ctx = requireAuth();
        Conversation conv = convRepo.findById(convId)
                .orElseThrow(() -> new BizException(ResultCode.CONV_NOT_FOUND));
        verifyOwner(conv.getUserId(), ctx.userId());

        ConversationMessage msg = ConversationMessage.of(convId, ctx.userId(), req.getRole(), req.getContent(), req.getMetadata());
        ConversationMessage saved = msgRepo.save(msg);

        Conversation updated = conv.withUpdatedLastMessageAt(Instant.now());
        convRepo.save(updated);

        return ConversationMessageVo.from(saved);
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }

    private void verifyOwner(Long ownerId, Long currentUserId) {
        if (!ownerId.equals(currentUserId)) {
            throw new BizException(ResultCode.CONV_NOT_OWNER);
        }
    }
}
