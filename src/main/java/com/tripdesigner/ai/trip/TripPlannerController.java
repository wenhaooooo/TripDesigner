package com.tripdesigner.ai.trip;

import com.tripdesigner.ai.trip.dto.GenerateTripRequest;
import com.tripdesigner.common.response.Result;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.trip.api.vo.TripDetailVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/trip")
@RequiredArgsConstructor
public class TripPlannerController {

    private final TripPlanningService tripPlanningService;
    private final AsyncTripGenerationService asyncTripGenerationService;

    /**
     * Generate a new trip from a user prompt (synchronous - blocking).
     */
    @PostMapping("/generate")
    public Result<TripGenerationResult> generate(
            @RequestParam(required = false) Long conversationId,
            @RequestParam String prompt) {
        return Result.success(tripPlanningService.generate(conversationId, prompt));
    }

    /**
     * Generate a new trip asynchronously. Returns a task ID immediately.
     * Use GET /ai/trip/tasks/{taskId} to check progress.
     */
    @PostMapping("/generate/async")
    public Result<TripGenerationTask> generateAsync(@RequestBody GenerateTripRequest request) {
        UserContext ctx = UserContextHolder.get();
        TripGenerationTask task = asyncTripGenerationService.createTask(ctx.userId(), ctx.email(), request.getPrompt());
        asyncTripGenerationService.executeAsync(task.getId(), ctx.userId(), ctx.email(), request.getPrompt());
        return Result.success(task);
    }

    /**
     * Get task status and progress.
     */
    @GetMapping("/tasks/{taskId}")
    public Result<TripGenerationTask> getTask(@PathVariable Long taskId) {
        UserContext ctx = UserContextHolder.get();
        return Result.success(asyncTripGenerationService.getTask(taskId, ctx.userId()));
    }

    /**
     * List all tasks for current user.
     */
    @GetMapping("/tasks")
    public Result<List<TripGenerationTask>> listTasks() {
        UserContext ctx = UserContextHolder.get();
        return Result.success(asyncTripGenerationService.listTasks(ctx.userId()));
    }

    /**
     * Chat with an existing trip to refine or discuss it.
     */
    @PostMapping("/chat")
    public Result<String> chat(
            @RequestParam Long conversationId,
            @RequestParam Long tripId,
            @RequestParam String prompt) {
        return Result.success(tripPlanningService.chat(conversationId, tripId, prompt));
    }

    /**
     * Get the generated trip detail.
     */
    @GetMapping("/{tripId}")
    public Result<TripDetailVo> getTripDetail(@PathVariable Long tripId) {
        return Result.success(tripPlanningService.getTripDetail(tripId));
    }
}
