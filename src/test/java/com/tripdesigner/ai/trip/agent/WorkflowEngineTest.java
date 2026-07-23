package com.tripdesigner.ai.trip.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.ai.rag.DestinationKnowledgeService;
import com.tripdesigner.ai.rag.RagMemoryService;
import com.tripdesigner.ai.trip.workflow.WorkflowSessionRepository;
import com.tripdesigner.ai.trip.workflow.WorkflowStep;
import com.tripdesigner.ai.trip.workflow.WorkflowStepRepository;
import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.conversation.application.ConversationAppService;
import com.tripdesigner.memory.api.dto.PreferenceRequest;
import com.tripdesigner.memory.api.dto.TripMemoryRequest;
import com.tripdesigner.memory.application.MemoryAppService;
import com.tripdesigner.trip.application.TripAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * WorkflowEngine 单元测试。
 *
 * 验证核心反馈循环逻辑：
 * - Agent 成功执行
 * - Agent 失败后重试
 * - 连续失败触发熔断
 * - Memory 回路：MEMORY 标签解析并落库
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowEngineTest {

    @Mock private TripAppService tripAppService;
    @Mock private ConversationAppService conversationAppService;
    @Mock private MemoryAppService memoryAppService;
    @Mock private RagMemoryService ragMemoryService;
    @Mock private DestinationKnowledgeService destinationKnowledgeService;
    @Mock private WorkflowSessionRepository sessionRepo;
    @Mock private WorkflowStepRepository stepRepo;
    @Mock private WorkflowSessionSetupService setupService;
    @Mock private com.tripdesigner.ai.trip.LanguageDetector languageDetector;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        when(stepRepo.save(any(WorkflowStep.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void executeAgents_completes_when_all_agents_succeed() {
        AbstractAgent planner = stubAgent("planner", "plan output");
        AbstractAgent transport = stubAgent("transport", "transport output");
        WorkflowEngine engine = buildEngine(planner, transport);

        AgentContext context = new AgentContext();

        engine.executeAgents(1L, context);

        assertEquals("plan output", context.getShared("planner_output"));
        assertEquals("transport output", context.getShared("transport_output"));
    }

    @Test
    void executeAgents_aborts_after_two_consecutive_failures() {
        AbstractAgent failingPlanner = stubFailingAgent("planner");
        AbstractAgent failingTransport = stubFailingAgent("transport");
        AbstractAgent dining = stubAgent("dining", "dining output");
        WorkflowEngine engine = buildEngine(failingPlanner, failingTransport, dining);

        AgentContext context = new AgentContext();

        assertThrows(BizException.class, () -> engine.executeAgents(1L, context));
        assertNull(context.getShared("dining_output"));
    }

    @Test
    void executeAgents_retries_then_succeeds() {
        AbstractAgent retryAgent = stubAgentRetryingThenSucceed("planner", "plan output");
        WorkflowEngine engine = buildEngine(retryAgent);

        AgentContext context = new AgentContext();

        engine.executeAgents(1L, context);

        assertEquals("plan output", context.getShared("planner_output"));
    }

    @Test
    void persistReflectionMemories_parses_and_persists_preference_and_highlight() throws Exception {
        WorkflowEngine engine = buildEngine();
        // 模拟 TripPlanningTools 回调写入 tripId（写入 ThreadLocal + context 兜底）
        AgentContext context = new AgentContext();
        engine.setGeneratedTripId(42L);

        context.putShared("reflection_output", """
                ## Review
                Plan looks good.

                MEMORY: PREFERENCE_DISCOVERED - User prefers budget-friendly options
                MEMORY: HIGHLIGHT - Great food scene in Hangzhou
                """);

        invokePersistMemories(engine, 1L, context);

        ArgumentCaptor<PreferenceRequest> prefCaptor = ArgumentCaptor.forClass(PreferenceRequest.class);
        verify(memoryAppService).savePreference(eq(1L), prefCaptor.capture());
        assertEquals("AI_DISCOVERED", prefCaptor.getValue().getCategory());
        assertEquals("AI_DISCOVERED", prefCaptor.getValue().getSource());

        ArgumentCaptor<TripMemoryRequest> memCaptor = ArgumentCaptor.forClass(TripMemoryRequest.class);
        verify(memoryAppService).saveTripMemory(eq(1L), memCaptor.capture());
        assertEquals("HIGHLIGHT", memCaptor.getValue().getMemoryType());
        assertEquals(42L, memCaptor.getValue().getTripId());
        assertTrue(memCaptor.getValue().getTags().contains("auto_extracted"));
    }

    @Test
    void persistReflectionMemories_skips_when_no_memory_tags() throws Exception {
        WorkflowEngine engine = buildEngine();

        AgentContext context = new AgentContext();
        context.putShared("reflection_output", "Just a regular review without any memory tags.");

        invokePersistMemories(engine, 1L, context);

        verify(memoryAppService, never()).savePreference(any(), any());
        verify(memoryAppService, never()).saveTripMemory(any(), any());
    }

    private WorkflowEngine buildEngine(AbstractAgent... agents) {
        com.tripdesigner.ai.trip.config.WorkflowProperties props = new com.tripdesigner.ai.trip.config.WorkflowProperties();
        props.setSkipOnFailure("");
        return new WorkflowEngine(
                List.of(agents),
                tripAppService,
                conversationAppService,
                memoryAppService,
                ragMemoryService,
                destinationKnowledgeService,
                sessionRepo,
                stepRepo,
                objectMapper,
                props,
                languageDetector,
                setupService
        );
    }

    private void invokePersistMemories(WorkflowEngine engine, Long userId, AgentContext context) throws Exception {
        Method method = WorkflowEngine.class.getDeclaredMethod("persistReflectionMemories", Long.class, AgentContext.class);
        method.setAccessible(true);
        method.invoke(engine, userId, context);
    }

    private AbstractAgent stubAgent(String name, String output) {
        AbstractAgent agent = mock(AbstractAgent.class);
        when(agent.getName()).thenReturn(name);
        when(agent.getMaxRetries()).thenReturn(3);
        when(agent.execute(any())).thenReturn(output);
        return agent;
    }

    private AbstractAgent stubFailingAgent(String name) {
        AbstractAgent agent = mock(AbstractAgent.class);
        when(agent.getName()).thenReturn(name);
        when(agent.getMaxRetries()).thenReturn(3);
        when(agent.execute(any())).thenThrow(new RuntimeException("LLM error"));
        return agent;
    }

    private AbstractAgent stubAgentRetryingThenSucceed(String name, String output) {
        AbstractAgent agent = mock(AbstractAgent.class);
        when(agent.getName()).thenReturn(name);
        when(agent.getMaxRetries()).thenReturn(3);
        when(agent.execute(any()))
                .thenThrow(new RuntimeException("transient error"))
                .thenReturn(output);
        return agent;
    }
}

