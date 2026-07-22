package com.tripdesigner.ai.trip;

import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.trip.api.vo.TripDetailVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTripGenerationService {

    private final TripGenerationTaskRepository taskRepository;
    private final TripPlannerAgent tripPlannerAgent;

    public TripGenerationTask createTask(Long userId, String userEmail, String prompt) {
        TripGenerationTask task = TripGenerationTask.create(userId, prompt);
        return taskRepository.save(task);
    }

    @Async
    public void executeAsync(Long taskId, Long userId, String userEmail, String prompt) {
        log.info("[AsyncTripGenerationService] Starting async trip generation: taskId={}", taskId);
        
        try {
            UserContextHolder.set(new UserContext(userId, userEmail));
            
            TripGenerationTask task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
            
            task = task.withStatus(TripGenerationStatus.RUNNING);
            task = task.withProgress(10, "正在分析您的旅行需求...");
            taskRepository.save(task);

            TripGenerationResult result = tripPlannerAgent.generate(userId, userEmail, null, prompt);
            
            task = task.withProgress(90, "行程生成完成，正在保存...");
            taskRepository.save(task);

            task = task.withResult(result.getConversationId(), 
                    result.getTrip() != null ? result.getTrip().getId() : null);
            taskRepository.save(task);

            log.info("[AsyncTripGenerationService] Async trip generation completed: taskId={}, tripId={}", 
                    taskId, task.getTripId());
        } catch (Exception e) {
            log.error("[AsyncTripGenerationService] Async trip generation failed: taskId={}", taskId, e);
            try {
                TripGenerationTask task = taskRepository.findById(taskId).orElse(null);
                if (task != null) {
                    task = task.withError(e.getMessage());
                    taskRepository.save(task);
                }
            } catch (Exception ex) {
                log.error("[AsyncTripGenerationService] Failed to update task error status", ex);
            }
        } finally {
            UserContextHolder.clear();
        }
    }

    @Transactional(readOnly = true)
    public TripGenerationTask getTask(Long taskId, Long userId) {
        TripGenerationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));
        if (!task.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问");
        }
        return task;
    }

    @Transactional(readOnly = true)
    public List<TripGenerationTask> listTasks(Long userId) {
        return taskRepository.findByUserId(userId);
    }
}