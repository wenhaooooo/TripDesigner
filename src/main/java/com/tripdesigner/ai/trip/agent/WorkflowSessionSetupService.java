package com.tripdesigner.ai.trip.agent;

import com.tripdesigner.ai.trip.workflow.WorkflowSession;
import com.tripdesigner.ai.trip.workflow.WorkflowSessionRepository;
import com.tripdesigner.ai.trip.workflow.WorkflowStatus;
import com.tripdesigner.conversation.api.dto.AddMessageRequest;
import com.tripdesigner.conversation.api.dto.CreateConversationRequest;
import com.tripdesigner.conversation.api.vo.ConversationVo;
import com.tripdesigner.conversation.application.ConversationAppService;
import com.tripdesigner.conversation.domain.ConversationRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 工作流会话初始化服务。
 *
 * 独立 Bean 以确保 @Transactional 通过 Spring AOP 代理生效
 * （同类内自调用会绕过代理导致事务失效）。
 *
 * 职责：
 *   - 创建对话（如未传入 conversationId）
 *   - 记录用户消息到对话
 *   - 创建 workflow_session 记录并标记为 RUNNING
 *
 * 该方法必须独立提交事务，确保 sessionId 立即可用，
 * 即使后续 Agent 执行失败也不会回滚会话创建。
 */
@Service
@RequiredArgsConstructor
public class WorkflowSessionSetupService {

    private final ConversationAppService conversationAppService;
    private final WorkflowSessionRepository sessionRepo;

    /**
     * Phase 1: 创建工作流会话和对话（独立事务）。
     * 如果未传入已有对话 ID，则自动创建新对话。
     *
     * @param userId               用户 ID
     * @param givenConversationId  已有对话 ID（可选）
     * @param userRequest          用户请求
     * @return 包含会话 ID 和对话 ID 的结果
     */
    @Transactional
    public WorkflowEngine.SetupResult setup(Long userId, Long givenConversationId, String userRequest) {
        Long convId = givenConversationId;
        if (convId == null) {
            CreateConversationRequest convReq = new CreateConversationRequest();
            convReq.setTitle(truncateTitle(userRequest));
            ConversationVo conv = conversationAppService.create(convReq);
            convId = conv.getId();
        }

        conversationAppService.addMessage(convId,
                AddMessageRequest.builder().role(ConversationRole.USER).content(userRequest).build());

        WorkflowSession session = WorkflowSession.create(convId, userId);
        session = sessionRepo.save(session);

        session = session.withStatus(WorkflowStatus.RUNNING);
        sessionRepo.save(session);

        return new WorkflowEngine.SetupResult(session.getId(), convId);
    }

    private String truncateTitle(String prompt) {
        String title = prompt.replace("\n", " ").trim();
        return title.length() > 80 ? title.substring(0, 77) + "..." : title;
    }
}
