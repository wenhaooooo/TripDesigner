package com.tripdesigner.ai.trip.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 工作流异步执行器。
 *
 * 为什么不直接在 WorkflowEngine 上加 @Async？
 *   Spring 的 @Async 依赖 AOP 代理，同类内自调用不会触发异步执行。
 *   因此将异步包装抽取到独立组件，由 Controller 跨类调用，保证代理生效。
 *
 * 使用 workflowExecutor 线程池（见 AsyncConfig），避免阻塞 Tomcat 请求线程。
 *
 * 使用模式：
 *   Long sessionId = workflowEngine.setupSessionPublic(...).sessionId();  // 同步创建会话
 *   workflowAsyncRunner.executeAsync(...);  // 异步执行，立即返回
 *   // 前端轮询 GET /ai/workflow/{sessionId} 查询进度
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowAsyncRunner {

    private final WorkflowEngine workflowEngine;

    /**
     * 使用已创建的 sessionId 异步执行工作流主体。
     * 通过 @Async 在 workflowExecutor 线程池中执行，不阻塞调用线程。
     *
     * 必须由外部类调用（Controller），同类内自调用不会触发 @Async 代理。
     *
     * @param userId       用户 ID
     * @param userEmail    用户邮箱
     * @param convId       对话 ID
     * @param sessionId    已创建的工作流会话 ID
     * @param userRequest  用户请求
     */
    @Async("workflowExecutor")
    public void executeAsync(Long userId, String userEmail,
                             Long convId, Long sessionId, String userRequest) {
        try {
            workflowEngine.executeWithExistingSession(userId, userEmail, convId, sessionId, userRequest);
            log.info("[WorkflowAsyncRunner] Async workflow completed for session={}", sessionId);
        } catch (Exception e) {
            // executeCore 已将异常转为 BizException 并标记会话 FAILED，这里只记录日志
            log.error("[WorkflowAsyncRunner] Async workflow failed for session={}", sessionId, e);
        }
    }
}
