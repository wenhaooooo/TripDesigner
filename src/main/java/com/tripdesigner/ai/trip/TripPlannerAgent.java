package com.tripdesigner.ai.trip;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.conversation.api.dto.AddMessageRequest;
import com.tripdesigner.conversation.api.dto.CreateConversationRequest;
import com.tripdesigner.conversation.api.vo.ConversationVo;
import com.tripdesigner.conversation.application.ConversationAppService;
import com.tripdesigner.conversation.domain.ConversationRole;
import com.tripdesigner.trip.api.vo.TripDetailVo;
import com.tripdesigner.trip.api.vo.TripVo;
import com.tripdesigner.trip.application.TripAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Trip planning agent that uses Spring AI to generate trips based on conversation.
 * The agent calls tools (TripPlanningTools) to create trips, add days, and add activities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TripPlannerAgent {

    private static final String SYSTEM_PROMPT = """
            You are an expert trip planner assistant. Your job is to help users plan detailed travel itineraries.
            
            When a user asks you to plan a trip:
            1. First, call createTrip() to set up the basic trip information (title, destination, dates, budget)
            2. Then, for each day of the trip, call addTripDay() to create a day entry
            3. For each day, call addTripActivity() to add specific activities
            4. When appropriate, call getTripDetail() to review what has been planned so far
            
            Important rules:
            - Always use createTrip() first before adding days or activities
            - Be specific about activity times (startTime, endTime in HH:mm format)
            - Use realistic travel times between activities
            - Include a mix of sightseeing, dining, and other activities
            - After creating the trip, summarize the plan in a friendly, engaging way
            - If the user asks to refine or modify an existing trip, use getTripDetail() first to understand what exists
            - Keep descriptions engaging and informative but concise
            
            For each activity, choose an appropriate category: sightseeing, dining, transport, accommodation, shopping, or other.
            """;

    private final ChatClient chatClient;
    private final TripPlanningTools tripPlanningTools;
    private final ConversationAppService conversationAppService;
    private final TripAppService tripAppService;
    private final ObjectMapper objectMapper;

    private static final int MAX_ITERATIONS = 15;

    /**
     * Generate a new trip from a user prompt within a conversation.
     */
    @Transactional
    public TripGenerationResult generate(Long userId, String userEmail, Long conversationId, String userPrompt) {
        try {
            setContext(userId, userEmail);
            
            ConversationVo conv = resolveOrCreateConversation(conversationId, userPrompt);
            Long convId = conv.getId();

            conversationAppService.addMessage(convId,
                    AddMessageRequest.builder().role(ConversationRole.USER).content(userPrompt).build());

            List<Message> history = buildMessageHistory(convId);
            String assistantReply = runAgent(convId, history, userPrompt);

            conversationAppService.addMessage(convId,
                    AddMessageRequest.builder()
                            .role(ConversationRole.ASSISTANT)
                            .content(assistantReply)
                            .metadata(buildMetadata(Instant.now().toString()))
                            .build());

            TripDetailVo trip = findCreatedTrip(userId);
            return new TripGenerationResult(convId, trip);
        } finally {
            UserContextHolder.clear();
        }
    }

    /**
     * Chat with an existing trip — refine or discuss it.
     */
    @Transactional
    public String chat(Long userId, String userEmail, Long conversationId, Long tripId, String userPrompt) {
        try {
            setContext(userId, userEmail);
            
            ConversationVo conv = conversationAppService.get(conversationId);
            Long convId = conv.getId();

            conversationAppService.addMessage(convId,
                    AddMessageRequest.builder().role(ConversationRole.USER).content(userPrompt).build());

            List<Message> history = buildMessageHistory(convId);
            String systemPrompt = buildRefineSystemPrompt(tripId);
            String reply = chatClient.prompt()
                    .system(systemPrompt)
                    .messages(history)
                    .tools(tripPlanningTools)
                    .call()
                    .content();

            conversationAppService.addMessage(convId,
                    AddMessageRequest.builder()
                            .role(ConversationRole.ASSISTANT)
                            .content(reply)
                            .build());

            return reply;
        } finally {
            UserContextHolder.clear();
        }
    }

    private void setContext(Long userId, String userEmail) {
        UserContextHolder.set(new UserContext(userId, userEmail));
    }

    private ConversationVo resolveOrCreateConversation(Long conversationId, String userPrompt) {
        if (conversationId != null) {
            return conversationAppService.get(conversationId);
        }
        CreateConversationRequest req = new CreateConversationRequest();
        req.setTitle(truncateForTitle(userPrompt));
        return conversationAppService.create(req);
    }

    private String truncateForTitle(String prompt) {
        String title = prompt.replace("\n", " ").trim();
        return title.length() > 80 ? title.substring(0, 77) + "..." : title;
    }

    private List<Message> buildMessageHistory(Long convId) {
        List<Message> messages = new ArrayList<>();
        for (var msg : conversationAppService.listMessages(convId)) {
            if ("ASSISTANT".equals(msg.getRole())) {
                messages.add(new AssistantMessage(msg.getContent()));
            } else {
                messages.add(new UserMessage(msg.getContent()));
            }
        }
        return messages;
    }

    private String runAgent(Long convId, List<Message> history, String userPrompt) {
        String systemPrompt = SYSTEM_PROMPT + """
                
                The user has requested: "%s"
                Analyze their request carefully and plan a detailed trip.
                """.formatted(userPrompt);

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            String reply = chatClient.prompt()
                    .system(systemPrompt)
                    .messages(history)
                    .tools(tripPlanningTools)
                    .call()
                    .content();

            if (reply != null && !reply.isBlank()) {
                return reply;
            }
        }

        throw new BizException(ResultCode.AI_GENERATION_FAILED,
                "Agent reached max iterations without producing a response");
    }

    private String buildRefineSystemPrompt(Long tripId) {
        UserContext ctx = UserContextHolder.get();
        TripDetailVo trip = tripAppService.getDetailForUser(tripId, ctx.userId());
        String tripInfo;
        try {
            tripInfo = objectMapper.writeValueAsString(trip);
        } catch (JsonProcessingException e) {
            tripInfo = trip.getTitle() + " (details unavailable)";
        }
        return """
                You are an expert trip planner assistant. The user has an existing trip they want to discuss or refine.
                Current trip details: %s
                
                Help the user refine their trip plan. You can:
                - Add new days or activities
                - Suggest improvements
                - Answer questions about the itinerary
                - Provide travel tips and recommendations
                
                Keep responses conversational and helpful.
                """.formatted(tripInfo);
    }

    private String buildMetadata(String timestamp) {
        try {
            var metadata = new java.util.HashMap<String, Object>();
            metadata.put("type", "agent_response");
            metadata.put("timestamp", timestamp);
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            return null;
        }
    }

    private TripDetailVo findCreatedTrip(Long userId) {
        List<TripVo> trips = tripAppService.listForUser(userId);
        return trips.isEmpty() ? null : tripAppService.getDetailForUser(trips.get(0).getId(), userId);
    }
}
