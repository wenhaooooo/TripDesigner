package com.tripdesigner.ai.trip.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.ai.trip.workflow.*;
import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.common.security.AgentContextHolder;
import com.tripdesigner.conversation.api.dto.AddMessageRequest;
import com.tripdesigner.conversation.api.vo.ConversationMessageVo;
import com.tripdesigner.conversation.application.ConversationAppService;
import com.tripdesigner.conversation.domain.ConversationRole;
import com.tripdesigner.memory.api.dto.PreferenceRequest;
import com.tripdesigner.memory.api.dto.TripMemoryRequest;
import com.tripdesigner.memory.application.MemoryAppService;
import com.tripdesigner.trip.api.vo.TripDetailVo;
import com.tripdesigner.trip.api.vo.TripVo;
import com.tripdesigner.trip.application.TripAppService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多 Agent 工作流引擎 —— 核心编排器。
 *
 * 负责协调 8 个专业 Agent 按顺序执行，生成完整的旅行计划。
 * 工作流分为三个阶段：
 *
 * Phase 1 - Setup（独立事务）:
 *   - 创建对话会话
 *   - 记录用户消息
 *   - 创建工作流会话并标记为 RUNNING
 *
 * Phase 2 - Execution（非事务，LLM 调用可能耗时较长）:
 *   - 按固定顺序依次执行各 Agent
 *   - 每个 Agent 的输出存入 sharedData 供后续 Agent 使用
 *   - 支持自动重试（最多 3 次，指数退避）
 *   - 每个 Agent 的步骤持久化到 workflow_steps 表
 *
 * Phase 3 - Completion（独立事务）:
 *   - 生成最终概要消息存入对话
 *   - 标记工作流会话为 COMPLETED
 *
 * Agent 执行顺序：
 * Planner → Transport → Dining → Sightseeing → Accommodation → Budget → Activity → Reflection
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEngine {

    /** 解析 ReflectionAgent 输出中的 MEMORY 标签，用于自动落库偏好/记忆 */
    private static final Pattern MEMORY_PATTERN = Pattern.compile(
            "MEMORY:\\s*(PREFERENCE_DISCOVERED|HIGHLIGHT|LESSON_LEARNED|LOWLIGHT|ADVICE)\\s*-\\s*(.+)");

    /** 取消标志位：sessionId → 是否已取消（协作式取消，在 Agent 间隙生效） */
    private final Map<Long, AtomicBoolean> cancelFlags = new ConcurrentHashMap<>();

    /** Spring 注入的所有 Agent（通过 List<AbstractAgent> 自动收集） */
    private final List<AbstractAgent> agents;

    /** 行程应用服务 */
    private final TripAppService tripAppService;

    /** 对话应用服务（创建工作流关联的对话和消息） */
    private final ConversationAppService conversationAppService;

    /** 偏好/记忆服务（为 Agent 提供个性化上下文） */
    private final MemoryAppService memoryAppService;

    /** RAG 记忆服务（方案1：语义检索用户偏好和记忆） */
    private final com.tripdesigner.ai.rag.RagMemoryService ragMemoryService;

    /** 目的地知识服务（方案2：RAG 检索目的地知识库） */
    private final com.tripdesigner.ai.rag.DestinationKnowledgeService destinationKnowledgeService;

    /** 工作流会话仓储 */
    private final WorkflowSessionRepository sessionRepo;

    /** 工作流步骤仓储 */
    private final WorkflowStepRepository stepRepo;

    /** Jackson ObjectMapper */
    private final ObjectMapper objectMapper;

    /** 工作流配置（Agent 执行顺序、熔断阈值等，可外部化） */
    private final com.tripdesigner.ai.trip.config.WorkflowProperties workflowProperties;

    /** 语言检测器（自动检测用户请求语言） */
    private final com.tripdesigner.ai.trip.LanguageDetector languageDetector;

    /**
     * 会话初始化服务（独立 Bean，确保 @Transactional 通过 AOP 代理生效）。
     *
     * 注意：同类内自调用 @Transactional 方法不会走代理，事务失效；
     * 因此将会话创建逻辑抽取到独立 Bean。
     */
    private final WorkflowSessionSetupService setupService;

    /**
     * Agent 超时执行专用线程池。
     * 缓存线程池避免每次调用都创建新线程池导致资源泄漏。
     * 设置 keepAlive 60s，空闲线程会被回收。
     */
    private final java.util.concurrent.ExecutorService timeoutExecutor =
            java.util.concurrent.Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "workflow-agent-timeout");
                t.setDaemon(true);
                return t;
            });

    /**
     * 当前工作流创建的行程 ID（ThreadLocal，每个工作流线程独立）。
     *
     * 替代原实例字段 generatedTripId，避免多工作流并发时实例字段被互相覆盖。
     * TripPlanningTools 在 LLM 调用线程内通过 setGeneratedTripId 写入，
     * executeWithExistingSession 在同一线程内通过 findCreatedTrip 读取。
     *
     * 注意：executeWithTimeout 会切换线程执行 agent.execute()，
     * 因此 setGeneratedTripId 可能写在 timeout 线程，findCreatedTrip 读在主线程。
     * 为兼容此场景，同时在 AgentContext.tripId 中保存一份作为兜底。
     */
    private final ThreadLocal<Long> currentTripId = new ThreadLocal<>();

    /**
     * 由 TripPlanningTools 或其他组件回调，记录本次工作流创建的行程 ID。
     * 同时写入 ThreadLocal 和 AgentContext（兜底）。
     *
     * @param tripId 创建的行程 ID
     */
    public void setGeneratedTripId(Long tripId) {
        currentTripId.set(tripId);
        // 同步写入当前 context（如果存在），作为超时线程场景下的兜底
        if (currentContext != null) {
            currentContext.setTripId(tripId);
        }
    }

    /** 当前正在执行的 AgentContext（用于 TripPlanningTools 回调时同步 tripId） */
    private volatile AgentContext currentContext;

    /**
     * 获取当前正在执行的 AgentContext。
     * 供 TripPlanningTools 在 LLM 工具调用时获取用户信息（跨线程场景）。
     *
     * @return 当前 AgentContext，或 null 如果没有正在执行的工作流
     */
    public AgentContext getCurrentContext() {
        AgentContext ctx = currentContext;
        if (ctx == null) {
            log.warn("[WorkflowEngine] getCurrentContext() returned null - no active workflow context");
        }
        return ctx;
    }

    /**
     * 执行完整的多人协作工作流，生成旅行计划（流式版本）。
     *
     * 执行流程：
     * 1. setupSession：创建对话 + 工作流会话（独立事务）
     * 2. 构建 AgentContext（含用户信息、偏好摘要、记忆摘要）
     * 3. streamExecuteAgents：按顺序依次执行 8 个 Agent，实时推送输出
     * 4. completeWorkflow：保存结果概要到对话，标记会话完成（独立事务）
     * 5. 查找并返回生成的行程详情
     *
     * @param userId               用户 ID
     * @param userEmail            用户邮箱
     * @param givenConversationId  已有对话 ID（null 则新建）
     * @param userRequest          用户的旅行需求描述
     * @param contentCallback      内容回调，用于推送每个 Agent 的流式输出
     * @return 工作流执行结果（包含会话 ID、对话 ID 和生成的行程）
     */
    public WorkflowResult streamExecute(Long userId, String userEmail, Long givenConversationId, 
                                        String userRequest, Consumer<SseMessage> contentCallback) {
        SetupResult setup = setupService.setup(userId, givenConversationId, userRequest);
        Long convId = setup.conversationId();
        Long sessionId = setup.sessionId();

        return streamExecuteCore(userId, userEmail, userRequest, convId, sessionId, contentCallback);
    }

    /**
     * 执行完整的多人协作工作流，生成旅行计划。
     */
    public WorkflowResult execute(Long userId, String userEmail, Long givenConversationId, String userRequest) {
        SetupResult setup = setupService.setup(userId, givenConversationId, userRequest);
        Long convId = setup.conversationId();
        Long sessionId = setup.sessionId();

        return executeCore(userId, userEmail, userRequest, convId, sessionId);
    }

    /**
     * 使用已创建的 sessionId 执行工作流主体（供异步启动场景使用）。
     * 会话已由 setupSessionPublic 创建并标记为 RUNNING，本方法只负责执行 Agent 和完成。
     *
     * @param userId       用户 ID
     * @param userEmail    用户邮箱
     * @param convId       对话 ID
     * @param sessionId    已创建的工作流会话 ID
     * @param userRequest  用户请求
     * @return 工作流执行结果
     */
    public WorkflowResult executeWithExistingSession(Long userId, String userEmail,
                                                     Long convId, Long sessionId, String userRequest) {
        return executeCore(userId, userEmail, userRequest, convId, sessionId);
    }

    /**
     * 工作流核心执行逻辑（Phase 2-5），被 execute 和 executeWithExistingSession 共用。
     */
    private WorkflowResult executeCore(Long userId, String userEmail, String userRequest,
                                       Long convId, Long sessionId) {
        cancelFlags.put(sessionId, new AtomicBoolean(false));

        try {
            log.info("[WorkflowEngine] Starting workflow for user={}, conversation={}", userId, convId);

            // 构建 Agent 共享上下文（包含用户偏好、记忆摘要和对话历史）
            AgentContext context = buildContext(userId, userEmail, userRequest, convId);
            // 设置当前 context，供 TripPlanningTools 回调时同步 tripId
            this.currentContext = context;
            // 清空 ThreadLocal，避免上一轮残留
            currentTripId.remove();

            // Phase 2: 依次执行各 Agent（非事务，LLM 调用可能耗时较长）
            executeAgents(sessionId, context);

            // Phase 3: 保存结果，标记工作流完成（独立事务）
            UserContextHolder.set(new com.tripdesigner.common.security.UserContext(userId, userEmail));
            try {
                completeWorkflow(sessionId, convId, context);

                // Memory 回路：解析 ReflectionAgent 输出的 MEMORY 标签并落库（失败不阻塞主流程）
                try {
                    persistReflectionMemories(userId, context);
                } catch (Exception e) {
                    log.warn("[WorkflowEngine] Failed to persist reflection memories for user={}", userId, e);
                }

                // 查找工作流执行期间创建的行程
                TripDetailVo trip = findCreatedTrip(userId, context);
                return new WorkflowResult(sessionId, convId, trip);
            } finally {
                UserContextHolder.clear();
            }
        } catch (Exception e) {
            log.error("[WorkflowEngine] Workflow failed for user={}", userId, e);
            try {
                sessionRepo.updateStatus(sessionId, WorkflowStatus.FAILED, e.getMessage());
            } catch (Exception markEx) {
                log.warn("[WorkflowEngine] Failed to mark session {} as FAILED", sessionId, markEx);
            }
            throw new BizException(ResultCode.AI_GENERATION_FAILED, e.getMessage(), e);
        } finally {
            cancelFlags.remove(sessionId);
            // 清理 ThreadLocal 和 context 引用，避免内存泄漏
            currentTripId.remove();
            this.currentContext = null;
        }
    }

    /**
     * 流式工作流核心执行逻辑（Phase 2-5），支持实时推送每个 Agent 的输出。
     */
    private WorkflowResult streamExecuteCore(Long userId, String userEmail, String userRequest,
                                             Long convId, Long sessionId, Consumer<SseMessage> contentCallback) {
        cancelFlags.put(sessionId, new AtomicBoolean(false));

        try {
            log.info("[WorkflowEngine] Starting streaming workflow for user={}, conversation={}", userId, convId);

            AgentContext context = buildContext(userId, userEmail, userRequest, convId);
            this.currentContext = context;
            currentTripId.remove();

            contentCallback.accept(SseMessage.start(sessionId));

            // Phase 2: 依次执行各 Agent，实时推送输出
            streamExecuteAgents(sessionId, context, contentCallback);

            // Phase 3: 保存结果，标记工作流完成
            UserContextHolder.set(new com.tripdesigner.common.security.UserContext(userId, userEmail));
            try {
                completeWorkflow(sessionId, convId, context);

                try {
                    persistReflectionMemories(userId, context);
                } catch (Exception e) {
                    log.warn("[WorkflowEngine] Failed to persist reflection memories for user={}", userId, e);
                }

                String summary = buildFinalSummary(context);
                contentCallback.accept(SseMessage.summary(sessionId, summary));
                contentCallback.accept(SseMessage.complete(sessionId));

                TripDetailVo trip = findCreatedTrip(userId, context);
                return new WorkflowResult(sessionId, convId, trip);
            } finally {
                UserContextHolder.clear();
            }
        } catch (Exception e) {
            log.error("[WorkflowEngine] Streaming workflow failed for user={}", userId, e);
            try {
                sessionRepo.updateStatus(sessionId, WorkflowStatus.FAILED, e.getMessage());
            } catch (Exception markEx) {
                log.warn("[WorkflowEngine] Failed to mark session {} as FAILED", sessionId, markEx);
            }
            contentCallback.accept(SseMessage.error(sessionId, e.getMessage()));
            throw new BizException(ResultCode.AI_GENERATION_FAILED, e.getMessage(), e);
        } finally {
            cancelFlags.remove(sessionId);
            currentTripId.remove();
            this.currentContext = null;
        }
    }

    /**
     * Phase 1: 创建工作流会话和对话（独立事务）。
     * 委托给 {@link WorkflowSessionSetupService#setup}，通过独立 Bean 确保
     * @Transactional 经由 Spring AOP 代理生效。
     *
     * @param userId               用户 ID
     * @param givenConversationId  已有对话 ID（可选）
     * @param userRequest          用户请求
     * @return 包含会话 ID 和对话 ID 的结果
     */
    public SetupResult setupSessionPublic(Long userId, Long givenConversationId, String userRequest) {
        return setupService.setup(userId, givenConversationId, userRequest);
    }

    /** setupSession 的内部返回结果 */
    public record SetupResult(Long sessionId, Long conversationId) {}

    /**
     * Phase 2: 按顺序执行所有 Agent。
     * 每个 Agent 的输出通过 AgentContext.sharedData 传递给后续 Agent，
     * 每个步骤的状态变化持久化到 workflow_steps 表。
     *
     * @param sessionId 工作流会话 ID
     * @param context   Agent 共享上下文
     */
    protected void executeAgents(Long sessionId, AgentContext context) {
        int consecutiveFailures = 0;
        for (String agentName : workflowProperties.getAgentOrder()) {
            if (cancelFlags.getOrDefault(sessionId, new AtomicBoolean(false)).get()) {
                sessionRepo.updateStatus(sessionId, WorkflowStatus.CANCELLED, "cancelled by user");
                throw new BizException(ResultCode.AI_GENERATION_FAILED, "Workflow cancelled by user");
            }

            AbstractAgent agent = findAgent(agentName);
            if (agent == null) {
                log.warn("[WorkflowEngine] Agent not found: {}", agentName);
                continue;
            }

            WorkflowStep step = executeAgentStep(sessionId, agent, context);
            if (step.getStatus().equals(StepStatus.COMPLETED)) {
                consecutiveFailures = 0;
                // PlannerAgent 创建行程后，将 trip detail 注入共享上下文
                if ("planner".equals(agentName)) {
                    injectTripDetail(context);
                }
            } else {
                if (workflowProperties.shouldSkipOnFailure(agentName)) {
                    log.warn("[WorkflowEngine] Agent {} failed but skipped (non-critical): {}", agentName, step.getErrorMessage());
                    context.putShared(agentName + "_output", "[Skipped] " + step.getErrorMessage());
                    continue;
                }
                consecutiveFailures++;
                log.warn("[WorkflowEngine] Agent {} failed: {}", agentName, step.getErrorMessage());
                if (consecutiveFailures >= workflowProperties.getMaxConsecutiveFailures()) {
                    throw new BizException(ResultCode.AI_GENERATION_FAILED,
                            "Workflow aborted after " + consecutiveFailures + " consecutive agent failures");
                }
            }
        }
    }

    /**
     * Phase 2: 流式执行所有 Agent，实时推送输出。
     * 支持并行执行独立的中间 Agent（Transport、Dining、Sightseeing、Accommodation、Budget），
     * 每个 Agent 的输出通过 AgentContext.sharedData 传递给后续 Agent，
     * 每个步骤的状态变化持久化到 workflow_steps 表，
     * 同时通过 contentCallback 实时推送每个 Agent 的流式输出。
     *
     * 执行顺序（并行优化）：
     * 1. Planner（串行）→ 创建行程框架
     * 2. Transport、Dining、Sightseeing、Accommodation、Budget（并行）→ 彼此独立
     * 3. Activity（串行）→ 整合所有信息
     * 4. Reflection（串行）→ 最终审查
     *
     * @param sessionId         工作流会话 ID
     * @param context           Agent 共享上下文
     * @param contentCallback   SSE 内容回调
     */
    protected void streamExecuteAgents(Long sessionId, AgentContext context, Consumer<SseMessage> contentCallback) {
        int consecutiveFailures = 0;
        List<String> agentOrder = workflowProperties.getAgentOrder();

        for (int i = 0; i < agentOrder.size(); i++) {
            String agentName = agentOrder.get(i);

            if (cancelFlags.getOrDefault(sessionId, new AtomicBoolean(false)).get()) {
                sessionRepo.updateStatus(sessionId, WorkflowStatus.CANCELLED, "cancelled by user");
                throw new BizException(ResultCode.AI_GENERATION_FAILED, "Workflow cancelled by user");
            }

            if (workflowProperties.isParallelExecutionEnabled() && isParallelAgent(agentName) && i < agentOrder.size() - 1 && isParallelAgent(agentOrder.get(i + 1))) {
                List<String> parallelAgents = new ArrayList<>();
                while (i < agentOrder.size() && isParallelAgent(agentOrder.get(i))) {
                    parallelAgents.add(agentOrder.get(i));
                    i++;
                }
                i--;

                List<WorkflowStep> parallelResults = streamExecuteAgentsParallel(sessionId, parallelAgents, context, contentCallback);
                for (WorkflowStep step : parallelResults) {
                    String pAgentName = step.getAgentName();
                    if (step.getStatus().equals(StepStatus.COMPLETED)) {
                        consecutiveFailures = 0;
                    } else {
                        if (workflowProperties.shouldSkipOnFailure(pAgentName)) {
                            log.warn("[WorkflowEngine] Agent {} failed but skipped (non-critical): {}", pAgentName, step.getErrorMessage());
                            context.putShared(pAgentName + "_output", "[Skipped] " + step.getErrorMessage());
                            contentCallback.accept(SseMessage.agentSkipped(sessionId, pAgentName, step.getErrorMessage()));
                        } else {
                            consecutiveFailures++;
                            log.warn("[WorkflowEngine] Agent {} failed: {}", pAgentName, step.getErrorMessage());
                            if (consecutiveFailures >= workflowProperties.getMaxConsecutiveFailures()) {
                                throw new BizException(ResultCode.AI_GENERATION_FAILED,
                                        "Workflow aborted after " + consecutiveFailures + " consecutive agent failures");
                            }
                        }
                    }
                }
                continue;
            }

            AbstractAgent agent = findAgent(agentName);
            if (agent == null) {
                log.warn("[WorkflowEngine] Agent not found: {}", agentName);
                continue;
            }

            WorkflowStep step = streamExecuteAgentStep(sessionId, agent, context, contentCallback);
            if (step.getStatus().equals(StepStatus.COMPLETED)) {
                consecutiveFailures = 0;
                if ("planner".equals(agentName)) {
                    injectTripDetail(context);
                }
            } else {
                if (workflowProperties.shouldSkipOnFailure(agentName)) {
                    log.warn("[WorkflowEngine] Agent {} failed but skipped (non-critical): {}", agentName, step.getErrorMessage());
                    context.putShared(agentName + "_output", "[Skipped] " + step.getErrorMessage());
                    contentCallback.accept(SseMessage.agentSkipped(sessionId, agentName, step.getErrorMessage()));
                    continue;
                }
                consecutiveFailures++;
                log.warn("[WorkflowEngine] Agent {} failed: {}", agentName, step.getErrorMessage());
                if (consecutiveFailures >= workflowProperties.getMaxConsecutiveFailures()) {
                    throw new BizException(ResultCode.AI_GENERATION_FAILED,
                            "Workflow aborted after " + consecutiveFailures + " consecutive agent failures");
                }
            }
        }
    }

    /**
     * 判断 Agent 是否可以并行执行（Transport、Dining、Sightseeing、Accommodation、Budget、Weather）
     */
    private boolean isParallelAgent(String agentName) {
        return List.of("transport", "dining", "sightseeing", "accommodation", "budget", "weather").contains(agentName);
    }

    /**
     * 并行执行多个独立的 Agent，使用 CompletableFuture 实现并发。
     * 每个 Agent 在独立线程中执行，结果汇总后返回。
     *
     * @param sessionId         工作流会话 ID
     * @param parallelAgents    要并行执行的 Agent 名称列表
     * @param context           Agent 共享上下文
     * @param contentCallback   SSE 内容回调
     * @return 所有 Agent 的执行结果
     */
    private List<WorkflowStep> streamExecuteAgentsParallel(Long sessionId, List<String> parallelAgents,
                                                           AgentContext context, Consumer<SseMessage> contentCallback) {
        log.info("[WorkflowEngine] Starting parallel execution for agents: {}", parallelAgents);

        List<CompletableFuture<WorkflowStep>> futures = new ArrayList<>();

        for (String agentName : parallelAgents) {
            AbstractAgent agent = findAgent(agentName);
            if (agent == null) {
                log.warn("[WorkflowEngine] Agent not found in parallel execution: {}", agentName);
                continue;
            }

            CompletableFuture<WorkflowStep> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return streamExecuteAgentStep(sessionId, agent, context, contentCallback);
                } catch (Exception e) {
                    log.error("[WorkflowEngine] Parallel agent {} execution error", agentName, e);
                    return WorkflowStep.builder()
                            .sessionId(sessionId)
                            .agentName(agentName)
                            .status(StepStatus.FAILED)
                            .errorMessage(e.getMessage())
                            .build();
                }
            });

            futures.add(future);
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            allFutures.get(workflowProperties.getAgentTimeoutSeconds() * 2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("[WorkflowEngine] Parallel execution timeout, cancelling remaining futures");
            futures.forEach(f -> f.cancel(true));
        } catch (InterruptedException | ExecutionException e) {
            log.error("[WorkflowEngine] Parallel execution error", e);
            Thread.currentThread().interrupt();
        }

        List<WorkflowStep> results = new ArrayList<>();
        for (CompletableFuture<WorkflowStep> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                log.warn("[WorkflowEngine] Parallel agent result retrieval error", e);
            }
        }

        log.info("[WorkflowEngine] Parallel execution completed for {} agents", results.size());
        return results;
    }

    /**
     * Phase 3: 保存工作流完成结果。
     * 生成最终概要消息并添加到对话中，标记工作流会话为 COMPLETED。
     *
     * @param sessionId 工作流会话 ID
     * @param convId    对话 ID
     * @param context   Agent 共享上下文（包含各 Agent 输出）
     */
    @Transactional
    protected void completeWorkflow(Long sessionId, Long convId, AgentContext context) {
        // 构建最终摘要消息
        String summary = buildFinalSummary(context);
        conversationAppService.addMessage(convId,
                AddMessageRequest.builder()
                        .role(ConversationRole.ASSISTANT)
                        .content(summary)
                        .metadata(buildWorkflowMetadata(sessionId))
                        .build());

        // 标记工作流会话为 COMPLETED
        sessionRepo.complete(sessionId, Instant.now());
    }

    /**
     * 构建 AgentContext，注入用户偏好和旅行记忆摘要。
     * 偏好和记忆从 Redis 缓存中获取（由 MemoryAppService 管理），
     * 格式化为自然语言后直接注入 Agent 提示词中用于个性化推荐。
     *
     * @param userId     用户 ID
     * @param userEmail  用户邮箱
     * @param userRequest 用户请求
     * @param convId     对话 ID
     * @return 构建好的 AgentContext
     */
    private AgentContext buildContext(Long userId, String userEmail, String userRequest, Long convId) {
        AgentContext context = new AgentContext();
        context.setUserId(userId);
        context.setUserEmail(userEmail);
        context.setUserRequest(userRequest);
        context.setPreferenceSummary(memoryAppService.getPreferenceSummary(userId));
        context.setTripMemorySummary(memoryAppService.getTripMemorySummary(userId));
        context.setConversationHistory(buildConversationHistory(convId));
        
        String detectedLanguage = languageDetector.detectLanguage(userRequest);
        context.setUserLanguage(detectedLanguage);
        log.info("[WorkflowEngine] Detected user language: {} for request: {}", detectedLanguage, 
                userRequest != null && userRequest.length() > 50 ? userRequest.substring(0, 50) + "..." : userRequest);

        try {
            String ragMemory = ragMemoryService.buildMemoryContext(userId, userRequest);
            context.setRagMemoryContext(ragMemory);
        } catch (Exception e) {
            log.warn("[WorkflowEngine] RAG memory retrieval failed, fallback to full summary: {}", e.getMessage());
        }

        try {
            String destination = extractDestination(userRequest);
            if (destination != null) {
                String knowledge = destinationKnowledgeService.buildKnowledgeContext(destination, userRequest);
                context.setRagKnowledgeContext(knowledge);
            }
        } catch (Exception e) {
            log.warn("[WorkflowEngine] RAG knowledge retrieval failed: {}", e.getMessage());
        }

        return context;
    }

    /**
     * 从用户请求中提取目的地名称（简单实现，可后续替换为 LLM 提取）。
     */
    private String extractDestination(String userRequest) {
        if (userRequest == null || userRequest.isBlank()) {
            return null;
        }
        String[] commonDestinations = {
            "东京", "大阪", "京都", "巴黎", "伦敦", "纽约", "首尔", "曼谷",
            "杭州", "北京", "上海", "成都", "西安", "厦门", "青岛", "重庆",
            "苏州", "南京", "广州", "深圳", "三亚", "丽江", "大理"
        };
        for (String d : commonDestinations) {
            if (userRequest.contains(d)) {
                return d;
            }
        }
        return null;
    }

    /**
     * 构建对话历史摘要，取最近 N 条消息拼成 "[role]: content" 格式。
     * 用于让 PlannerAgent 在连续对话场景下理解上下文。
     *
     * @param convId 对话 ID（null 返回 null）
     * @return 格式化的对话历史，或 null
     */
    private String buildConversationHistory(Long convId) {
        if (convId == null) {
            return null;
        }
        try {
            List<ConversationMessageVo> messages = conversationAppService.listMessages(convId);
            if (messages == null || messages.isEmpty()) {
                return null;
            }
            int start = Math.max(0, messages.size() - workflowProperties.getConversationHistoryLimit());
            List<ConversationMessageVo> recent = messages.subList(start, messages.size());
            StringBuilder sb = new StringBuilder();
            for (ConversationMessageVo m : recent) {
                String role = "assistant".equalsIgnoreCase(m.getRole()) ? "Assistant" : "User";
                sb.append("[").append(role).append("]: ").append(m.getContent()).append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("[WorkflowEngine] Failed to load conversation history for convId={}", convId, e);
            return null;
        }
    }

    /**
     * 根据 Agent 名称查找对应的 Agent 实例。
     * 通过 Spring 注入的 List<AbstractAgent> 自动装配。
     *
     * @param name Agent 名称
     * @return Agent 实例，未找到返回 null
     */
    private AbstractAgent findAgent(String name) {
        return agents.stream()
                .filter(a -> a.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * 执行单个 Agent 步骤，包含重试机制。
     *
     * 执行流程：
     * 1. 创建 PENDING 状态的 step 记录
     * 2. 标记为 RUNNING
     * 3. 调用 agent.execute()
     * 4. 成功则标记为 COMPLETED，失败则重试
     *
     * 重试策略：
     * - 最多重试 MAX_RETRIES 次
     * - 每次重试前等待 1s * attempt（指数退避）
     * - 所有重试耗尽后标记为 FAILED
     *
     * @param sessionId 工作流会话 ID
     * @param agent     要执行的 Agent
     * @param context   共享上下文
     * @return 执行完成的 WorkflowStep
     */
    private WorkflowStep executeAgentStep(Long sessionId, AbstractAgent agent, AgentContext context) {
        String agentName = agent.getName();
        String outputKey = agentName + "_output";
        int maxRetries = agent.getMaxRetries();
        long timeoutSeconds = workflowProperties.getAgentTimeoutSeconds();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            // 创建步骤记录（PENDING）
            WorkflowStep step = WorkflowStep.create(sessionId, agentName);
            step = stepRepo.save(step);

            // 标记为 RUNNING
            step = step.withRunning(Instant.now());
            stepRepo.save(step);

            try {
                // 执行 Agent 的核心逻辑（调用 LLM），带超时控制
                String output = executeWithTimeout(agent, context, timeoutSeconds);

                // 将输出保存到上下文，供后续 Agent 使用
                context.putShared(outputKey, output);

                // 记录执行步骤到上下文
                context.addStep(agentName, output, true, attempt);

                // 标记为 COMPLETED
                step = step.withCompleted(output, Instant.now());
                stepRepo.save(step);

                log.info("[WorkflowEngine] Agent {} completed in {} attempt(s)", agentName, attempt);
                return step;
            } catch (Exception e) {
                log.warn("[WorkflowEngine] Agent {} failed attempt {}", agentName, attempt, e);

                // 标记为 FAILED
                step = step.withFailed(e.getMessage(), Instant.now());
                stepRepo.save(step);

                if (attempt < maxRetries) {
                    try {
                        // 线性退避：等待 1s, 2s, 3s
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // 所有重试耗尽，标记最终失败状态
        WorkflowStep step = WorkflowStep.builder()
                .sessionId(sessionId)
                .agentName(agentName)
                .status(StepStatus.FAILED)
                .errorMessage("All " + maxRetries + " retries exhausted")
                .iteration(maxRetries)
                .build();
        stepRepo.save(step);
        return step;
    }

    /**
     * 流式执行单个 Agent 步骤，实时推送输出。
     *
     * 执行流程：
     * 1. 创建 PENDING 状态的 step 记录
     * 2. 标记为 RUNNING，推送 agent_start 事件
     * 3. 调用 agent.streamExecute()，实时推送内容块
     * 4. 成功则标记为 COMPLETED，推送 agent_end 事件
     * 5. 失败则重试
     *
     * @param sessionId         工作流会话 ID
     * @param agent             要执行的 Agent
     * @param context           共享上下文
     * @param contentCallback   SSE 内容回调
     * @return 执行完成的 WorkflowStep
     */
    private WorkflowStep streamExecuteAgentStep(Long sessionId, AbstractAgent agent, AgentContext context,
                                                Consumer<SseMessage> contentCallback) {
        String agentName = agent.getName();
        String outputKey = agentName + "_output";
        int maxRetries = agent.getMaxRetries();
        long timeoutSeconds = workflowProperties.getAgentTimeoutSeconds();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            WorkflowStep step = WorkflowStep.create(sessionId, agentName);
            step = stepRepo.save(step);

            step = step.withRunning(Instant.now());
            stepRepo.save(step);

            contentCallback.accept(SseMessage.agentStart(sessionId, agentName));

            try {
                StringBuilder fullOutput = new StringBuilder();
                Consumer<String> tokenCallback = content -> {
                    fullOutput.append(content);
                    contentCallback.accept(SseMessage.agentContent(sessionId, agentName, content));
                };

                String output = streamExecuteWithTimeout(agent, context, timeoutSeconds, tokenCallback);

                context.putShared(outputKey, output);
                context.addStep(agentName, output, true, attempt);

                step = step.withCompleted(output, Instant.now());
                stepRepo.save(step);

                contentCallback.accept(SseMessage.agentEnd(sessionId, agentName));
                log.info("[WorkflowEngine] Agent {} completed in {} attempt(s)", agentName, attempt);
                return step;
            } catch (Exception e) {
                log.warn("[WorkflowEngine] Agent {} failed attempt {}", agentName, attempt, e);

                step = step.withFailed(e.getMessage(), Instant.now());
                stepRepo.save(step);

                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        WorkflowStep step = WorkflowStep.builder()
                .sessionId(sessionId)
                .agentName(agentName)
                .status(StepStatus.FAILED)
                .errorMessage("All " + maxRetries + " retries exhausted")
                .iteration(maxRetries)
                .build();
        stepRepo.save(step);
        contentCallback.accept(SseMessage.agentFailed(sessionId, agentName, step.getErrorMessage()));
        return step;
    }

    /**
     * 带超时控制的 Agent 执行。
     *
     * 在独立线程中执行 agent.execute()，超时后抛出 TimeoutException，
     * 由调用方重试或标记失败。避免单个 Agent 阻塞整个工作流。
     *
     * 注意：超时后底层 LLM 调用可能仍在进行，但本工作流不再等待其结果。
     */
    private String executeWithTimeout(AbstractAgent agent, AgentContext context, long timeoutSeconds) throws Exception {
        if (timeoutSeconds <= 0) {
            return agent.execute(context);
        }

        Future<String> future = timeoutExecutor.submit(() -> {
            UserContextHolder.set(new com.tripdesigner.common.security.UserContext(
                    context.getUserId(), context.getUserEmail()));
            try {
                return agent.execute(context);
            } finally {
                UserContextHolder.clear();
                currentTripId.remove();
            }
        });
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new BizException(ResultCode.AI_GENERATION_FAILED,
                    "Agent " + agent.getName() + " timed out after " + timeoutSeconds + " seconds");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception ex) {
                throw ex;
            }
            throw e;
        }
    }

    /**
     * 构建最终行程概要，将各 Agent 的核心输出汇总为自然语言。
     * 包含行程概览、每日安排、预算和复盘建议。
     *
     * @param context Agent 共享上下文
     * @return 格式化的行程概要文本
     */
    private String buildFinalSummary(AgentContext context) {
        StringBuilder sb = new StringBuilder();

        String plan = context.getShared("planner_output");
        String activity = context.getShared("activity_output");
        String budget = context.getShared("budget_output");
        String reflection = context.getShared("reflection_output");

        if (plan != null) {
            sb.append("## Trip Overview\n\n").append(plan).append("\n\n---\n\n");
        }
        if (activity != null) {
            sb.append("## Daily Schedule\n\n").append(activity).append("\n\n---\n\n");
        }
        if (budget != null) {
            sb.append("## Budget\n\n").append(budget).append("\n\n---\n\n");
        }
        if (reflection != null) {
            sb.append("## Review & Recommendations\n\n").append(reflection);
        }

        return sb.toString().trim().isEmpty() ? "Trip plan generated successfully." : sb.toString();
    }

    /**
     * 构建工作流元数据 JSON，附加到对话消息中。
     * 包含工作流类型、会话 ID 和 Agent 数量等信息。
     *
     * @param sessionId 工作流会话 ID
     * @return JSON 字符串，或 null（构建失败时）
     */
    private String buildWorkflowMetadata(Long sessionId) {
        try {
            var metadata = new java.util.HashMap<String, Object>();
            metadata.put("type", "multi_agent_workflow");
            metadata.put("workflowSessionId", sessionId);
            metadata.put("agentCount", workflowProperties.getAgentOrder().size());
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("[WorkflowEngine] Failed to build workflow metadata for session {}", sessionId, e);
            return null;
        }
    }

    /**
     * 查找工作流执行期间创建的行程。
     * 优先使用 ThreadLocal 中的 tripId（TripPlanningTools 回调设置），
     * 其次从 AgentContext.tripId 读取（兜底：超时线程场景下 ThreadLocal 可能未同步）。
     *
     * @param userId  用户 ID
     * @param context 当前工作流上下文
     * @return 行程详情，或 null
     */
    private TripDetailVo findCreatedTrip(Long userId, AgentContext context) {
        Long tripId = currentTripId.get();
        if (tripId == null && context != null) {
            tripId = context.getTripId();
        }
        if (tripId != null) {
            try {
                return tripAppService.getDetailForUser(tripId, userId);
            } catch (Exception e) {
                log.warn("[WorkflowEngine] Generated trip {} not found for user {}", tripId, userId, e);
            }
        }
        return null;
    }

    /**
     * 将当前已创建的行程详情（tripId、days 等）注入共享上下文，
     * 供 ActivityAgent 等后续 Agent 获取 tripId 并调用 addTripDay / addTripActivity 工具。
     * <p>
     * 在 PlannerAgent 执行成功后调用，确保后续 Agent 能拿到行程的完整信息。
     *
     * @param context Agent 共享上下文
     */
    private void injectTripDetail(AgentContext context) {
        Long tripId = currentTripId.get();
        if (tripId == null && context != null) {
            tripId = context.getTripId();
        }
        if (tripId == null) {
            log.warn("[WorkflowEngine] No tripId available to inject into shared context");
            return;
        }
        try {
            TripDetailVo detail = tripAppService.getDetailForUser(tripId, context.getUserId());
            String json = objectMapper.writeValueAsString(detail);
            context.putShared("trip_detail", json);
            log.info("[WorkflowEngine] Injected trip_detail for tripId={} into shared context", tripId);
        } catch (Exception e) {
            log.warn("[WorkflowEngine] Failed to inject trip detail for tripId={}", tripId, e);
        }
    }

    /**
     * 取消正在执行的工作流（协作式取消）。
     * 设置取消标志位，在下一个 Agent 执行前检查并中断。
     * 单个 Agent 执行中不可中断（LLM 调用阻塞）。
     *
     * @param sessionId 工作流会话 ID
     */
    public void cancel(Long sessionId) {
        AtomicBoolean flag = cancelFlags.get(sessionId);
        if (flag == null) {
            throw new BizException(ResultCode.AI_GENERATION_FAILED,
                    "Workflow session not found or already completed: " + sessionId);
        }
        flag.set(true);
        log.info("[WorkflowEngine] Cancel requested for session {}", sessionId);
    }

    /**
     * 解析 ReflectionAgent 输出中的 MEMORY 标签并落库。
     *
     * 标签格式：MEMORY: <TYPE> - <content>
     * - PREFERENCE_DISCOVERED → 写入 UserPreference（source=AI_DISCOVERED）
     * - HIGHLIGHT/LESSON_LEARNED/LOWLIGHT/ADVICE → 写入 TripMemory
     *
     * 每条记忆独立 try-catch，单条失败不影响其他。
     *
     * @param userId  用户 ID
     * @param context Agent 共享上下文
     */
    private void persistReflectionMemories(Long userId, AgentContext context) {
        String reflection = context.getShared("reflection_output");
        if (reflection == null || reflection.isBlank()) {
            return;
        }

        Matcher matcher = MEMORY_PATTERN.matcher(reflection);
        int persisted = 0;
        while (matcher.find()) {
            String type = matcher.group(1);
            String content = matcher.group(2).trim();
            try {
                if ("PREFERENCE_DISCOVERED".equals(type)) {
                    PreferenceRequest req = new PreferenceRequest();
                    req.setCategory("AI_DISCOVERED");
                    req.setPreference(Map.of("insight", content));
                    req.setSource("AI_DISCOVERED");
                    memoryAppService.savePreference(userId, req);
                    // 方案1：同步索引到 RAG 向量库，供后续语义检索
                    ragMemoryService.indexPreference(userId, "AI_DISCOVERED", content);
                } else {
                    TripMemoryRequest req = new TripMemoryRequest();
                    Long tripId = currentTripId.get();
                    if (tripId == null && context != null) {
                        tripId = context.getTripId();
                    }
                    req.setTripId(tripId != null ? tripId : 0L);
                    req.setMemoryType(type);
                    req.setContent(content);
                    req.setTags(List.of("auto_extracted"));
                    memoryAppService.saveTripMemory(userId, req);
                    // 方案1：同步索引到 RAG 向量库，供后续语义检索
                    ragMemoryService.indexTripMemory(userId, type, content);
                }
                persisted++;
            } catch (Exception e) {
                log.warn("[WorkflowEngine] Failed to persist memory [{}]: {}", type, content, e);
            }
        }
        if (persisted > 0) {
            log.info("[WorkflowEngine] Persisted {} memories for user={}", persisted, userId);
        }
    }

    /**
     * 查询工作流执行详情，包含会话信息和所有步骤记录。
     *
     * @param sessionId 工作流会话 ID
     * @return 工作流执行详情
     * @throws BizException 会话不存在时抛出
     */
    public WorkflowDetails getWorkflowDetails(Long sessionId) {
        WorkflowSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new BizException(ResultCode.AI_GENERATION_FAILED, "Workflow session not found"));
        List<WorkflowStep> steps = stepRepo.findBySessionId(sessionId);
        return new WorkflowDetails(session, steps);
    }

    /**
     * 工作流执行结果。
     * 包含工作流会话 ID、关联的对话 ID 和生成的行程详情。
     */
    @Getter
    @RequiredArgsConstructor
    public static class WorkflowResult {
        private final Long workflowSessionId;
        private final Long conversationId;
        private final TripDetailVo trip;
    }

    /**
     * 工作流执行详情（用于查看工作流进度和历史）。
     * 包含工作流会话和所有步骤的详细记录。
     */
    @Getter
    @RequiredArgsConstructor
    public static class WorkflowDetails {
        private final WorkflowSession session;
        private final List<WorkflowStep> steps;
    }

    /**
     * 带超时控制的 Agent 流式执行。
     *
     * 在独立线程中执行 agent.streamExecute()，超时后抛出 TimeoutException。
     */
    private String streamExecuteWithTimeout(AbstractAgent agent, AgentContext context, long timeoutSeconds,
                                           Consumer<String> contentCallback) throws Exception {
        if (timeoutSeconds <= 0) {
            return agent.streamExecute(context, contentCallback);
        }

        Future<String> future = timeoutExecutor.submit(() -> {
            UserContextHolder.set(new com.tripdesigner.common.security.UserContext(
                    context.getUserId(), context.getUserEmail()));
            AgentContextHolder.set(context);
            try {
                return agent.streamExecute(context, contentCallback);
            } finally {
                UserContextHolder.clear();
                AgentContextHolder.clear();
                currentTripId.remove();
            }
        });
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new BizException(ResultCode.AI_GENERATION_FAILED,
                    "Agent " + agent.getName() + " timed out after " + timeoutSeconds + " seconds");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception ex) {
                throw ex;
            }
            throw e;
        }
    }

    /**
     * SSE 消息类型，用于流式推送工作流进度和内容。
     */
    @Getter
    public static class SseMessage {
        private final String type;
        private final Long sessionId;
        private final String agentName;
        private final String content;
        private final String error;

        private SseMessage(String type, Long sessionId, String agentName, String content, String error) {
            this.type = type;
            this.sessionId = sessionId;
            this.agentName = agentName;
            this.content = content;
            this.error = error;
        }

        public static SseMessage start(Long sessionId) {
            return new SseMessage("start", sessionId, null, null, null);
        }

        public static SseMessage agentStart(Long sessionId, String agentName) {
            return new SseMessage("agent_start", sessionId, agentName, null, null);
        }

        public static SseMessage agentContent(Long sessionId, String agentName, String content) {
            return new SseMessage("agent_content", sessionId, agentName, content, null);
        }

        public static SseMessage agentEnd(Long sessionId, String agentName) {
            return new SseMessage("agent_end", sessionId, agentName, null, null);
        }

        public static SseMessage agentFailed(Long sessionId, String agentName, String error) {
            return new SseMessage("agent_failed", sessionId, agentName, null, error);
        }

        public static SseMessage agentSkipped(Long sessionId, String agentName, String reason) {
            return new SseMessage("agent_skipped", sessionId, agentName, null, reason);
        }

        public static SseMessage summary(Long sessionId, String content) {
            return new SseMessage("summary", sessionId, null, content, null);
        }

        public static SseMessage complete(Long sessionId) {
            return new SseMessage("complete", sessionId, null, null, null);
        }

        public static SseMessage error(Long sessionId, String error) {
            return new SseMessage("error", sessionId, null, null, error);
        }
    }

}
