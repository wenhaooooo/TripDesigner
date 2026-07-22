package com.tripdesigner.ai.trip;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.ai.trip.agent.AgentContext;
import com.tripdesigner.ai.trip.agent.WorkflowEngine;
import com.tripdesigner.common.security.AgentContextHolder;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.trip.api.vo.TripActivityVo;
import com.tripdesigner.trip.api.vo.TripDetailVo;
import com.tripdesigner.trip.api.vo.TripDayVo;
import com.tripdesigner.trip.api.vo.TripVo;
import com.tripdesigner.trip.application.TripAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Spring AI @Tool component for trip planning.
 * Each @Tool method is callable by the LLM during a trip planning conversation.
 * All methods operate on behalf of the currently authenticated user.
 */
@Slf4j
@Component
public class TripPlanningTools {

    private final TripAppService tripAppService;
    private final ObjectMapper objectMapper;
    private WorkflowEngine workflowEngine;

    public TripPlanningTools(TripAppService tripAppService, ObjectMapper objectMapper,
                             @Lazy WorkflowEngine workflowEngine) {
        this.tripAppService = tripAppService;
        this.objectMapper = objectMapper;
        this.workflowEngine = workflowEngine;
    }

    private UserContext requireUser() {
        // 1. 优先从 ThreadLocal 获取（同线程场景）
        UserContext ctx = UserContextHolder.get();
        if (ctx != null) {
            return ctx;
        }
        // 2. 从 AgentContextHolder 获取（timeout 线程场景）
        AgentContext agentCtx = AgentContextHolder.get();
        if (agentCtx == null && workflowEngine != null) {
            // 3. 从 WorkflowEngine 实例字段获取（LLM 工具调用跨线程场景）
            agentCtx = workflowEngine.getCurrentContext();
        }
        if (agentCtx != null && agentCtx.getUserId() != null) {
            return new UserContext(agentCtx.getUserId(), agentCtx.getUserEmail());
        }
        throw new IllegalStateException("User context not available");
    }

    private Long userId() {
        return requireUser().userId();
    }

    @Tool(description = "Create a new trip with basic information. Use this tool when the user asks to plan a trip. " +
            "Parameters: title (trip name), destinationName (main destination), startDate (yyyy-mm-dd), " +
            "endDate (yyyy-mm-dd), budget (integer in local currency, optional), description (optional). " +
            "Returns the created trip with its ID.")
    public String createTrip(String title, String destinationName, LocalDate startDate, LocalDate endDate,
                              Integer budget, String description) {
        try {
            TripVo trip = tripAppService.createForUser(
                    userId(), title, description, destinationName, startDate, endDate, budget);
            workflowEngine.setGeneratedTripId(trip.getId());
            log.info("[TripPlanningTools] Trip created: id={}, title='{}'", trip.getId(), trip.getTitle());
            return String.format("Trip created: ID=%d, title='%s', destination='%s', dates=%s~%s",
                    trip.getId(), trip.getTitle(), trip.getDestinationName(),
                    trip.getStartDate(), trip.getEndDate());
        } catch (Exception e) {
            log.error("[TripPlanningTools] Failed to create trip", e);
            return "Error creating trip: " + e.getMessage();
        }
    }

    @Tool(description = "Add a trip day to an existing trip. Use this to plan specific days of the trip. " +
            "Parameters: tripId (the trip to add to), dayNumber (day number, e.g. 1, 2, 3...), " +
            "date (yyyy-mm-dd), title (day theme, optional), description (optional). " +
            "If the day already exists, it will be returned instead of creating a new one. " +
            "Returns the trip day with its ID.")
    public String addTripDay(Long tripId, Integer dayNumber, LocalDate date,
                              String title, String description) {
        try {
            TripDayVo day = tripAppService.findOrCreateDayForTrip(
                    tripId, userId(), dayNumber);
            return String.format("Trip day: ID=%d, day=%d, title='%s', date=%s",
                    day.getId(), day.getDayNumber(), day.getTitle(), day.getDate());
        } catch (Exception e) {
            log.error("[TripPlanningTools] Failed to add trip day", e);
            return "Error adding trip day: " + e.getMessage();
        }
    }

    @Tool(description = "Add an activity to a specific trip day. Use this to plan activities within a day. " +
            "Parameters: tripDayId (the day to add to), name (activity name), startTime (HH:mm, optional), " +
            "endTime (HH:mm, optional), category (sightseeing/dining/transport/accommodation/shopping/other), " +
            "place (location, optional), description (optional). " +
            "Returns the created activity with its ID.")
    public String addTripActivity(Long tripDayId, String name, String startTime, String endTime,
                                   String category, String place, String description) {
        try {
            LocalTime st = (startTime != null && !startTime.isBlank()) ? LocalTime.parse(startTime) : null;
            LocalTime et = (endTime != null && !endTime.isBlank()) ? LocalTime.parse(endTime) : null;
            TripActivityVo activity = tripAppService.addActivityForDay(
                    tripDayId, userId(), name, st, et, category, place, description);
            return String.format("Activity added: ID=%d, name='%s', time=%s-%s, category=%s, place='%s'",
                    activity.getId(), activity.getName(), activity.getStartTime(),
                    activity.getEndTime(), activity.getCategory(), activity.getPlace());
        } catch (Exception e) {
            log.error("[TripPlanningTools] Failed to add trip activity", e);
            return "Error adding activity: " + e.getMessage();
        }
    }

    @Tool(description = "Add an activity to a specific trip day by day number. " +
            "This is more reliable than using tripDayId directly. " +
            "Parameters: tripId (the trip ID), dayNumber (day number like 1, 2, 3...), " +
            "name (activity name), startTime (HH:mm, optional), endTime (HH:mm, optional), " +
            "category (sightseeing/dining/transport/accommodation/shopping/other), " +
            "place (location, optional), description (optional). " +
            "If the day doesn't exist, it will be created automatically. " +
            "Returns the created activity with its ID.")
    public String addTripActivityByDayNumber(Long tripId, Integer dayNumber, String name, String startTime, 
                                              String endTime, String category, String place, String description) {
        try {
            LocalTime st = (startTime != null && !startTime.isBlank()) ? LocalTime.parse(startTime) : null;
            LocalTime et = (endTime != null && !endTime.isBlank()) ? LocalTime.parse(endTime) : null;
            TripActivityVo activity = tripAppService.addActivityForDayByNumber(
                    tripId, userId(), dayNumber, name, st, et, category, place, description);
            return String.format("Activity added: ID=%d, name='%s', day=%d, time=%s-%s, category=%s, place='%s'",
                    activity.getId(), activity.getName(), dayNumber, activity.getStartTime(),
                    activity.getEndTime(), activity.getCategory(), activity.getPlace());
        } catch (Exception e) {
            log.error("[TripPlanningTools] Failed to add trip activity by day number", e);
            return "Error adding activity: " + e.getMessage();
        }
    }

    @Tool(description = "Get full details of a trip including all days and activities. " +
            "Use this to review the current trip plan before adding more days or activities, " +
            "or when the user asks to see what has been planned so far. " +
            "Parameters: tripId (the trip to get). Returns JSON with trip details.")
    public String getTripDetail(Long tripId) {
        try {
            TripDetailVo detail = tripAppService.getDetailForUser(tripId, userId());
            return objectMapper.writeValueAsString(detail);
        } catch (Exception e) {
            log.error("[TripPlanningTools] Failed to get/parse trip detail", e);
            return "Error getting trip detail: " + e.getMessage();
        }
    }
}
