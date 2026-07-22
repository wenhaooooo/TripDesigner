package com.tripdesigner.memory.api;

import com.tripdesigner.common.response.Result;
import com.tripdesigner.memory.api.dto.PreferenceRequest;
import com.tripdesigner.memory.api.dto.TripMemoryRequest;
import com.tripdesigner.memory.api.vo.MemorySummaryVo;
import com.tripdesigner.memory.api.vo.PreferenceVo;
import com.tripdesigner.memory.api.vo.TripMemoryVo;
import com.tripdesigner.memory.application.MemoryAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for user preferences and trip memories.
 * All endpoints require authentication (handled in application service).
 */
@RestController
@RequestMapping("/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryAppService memoryAppService;

    // ==================== Preferences ====================

    @PostMapping("/preferences")
    public Result<PreferenceVo> savePreference(@Valid @RequestBody PreferenceRequest request) {
        PreferenceVo vo = memoryAppService.savePreference(request);
        return Result.success(vo);
    }

    @GetMapping("/preferences")
    public Result<List<PreferenceVo>> listPreferences() {
        List<PreferenceVo> list = memoryAppService.listPreferences();
        return Result.success(list);
    }

    @GetMapping("/preferences/{id}")
    public Result<PreferenceVo> getPreference(@PathVariable Long id) {
        PreferenceVo vo = memoryAppService.getPreference(id);
        return Result.success(vo);
    }

    @DeleteMapping("/preferences/{id}")
    public Result<Void> deletePreference(@PathVariable Long id) {
        memoryAppService.deletePreference(id);
        return Result.success();
    }

    // ==================== Trip Memories ====================

    @PostMapping("/trip-memories")
    public Result<TripMemoryVo> saveTripMemory(@Valid @RequestBody TripMemoryRequest request) {
        TripMemoryVo vo = memoryAppService.saveTripMemory(request);
        return Result.success(vo);
    }

    @GetMapping("/trip-memories")
    public Result<List<TripMemoryVo>> listTripMemories() {
        List<TripMemoryVo> list = memoryAppService.listTripMemories();
        return Result.success(list);
    }

    @GetMapping("/trip-memories/type/{memoryType}")
    public Result<List<TripMemoryVo>> listTripMemoriesByType(@PathVariable String memoryType) {
        List<TripMemoryVo> list = memoryAppService.listTripMemoriesByType(memoryType);
        return Result.success(list);
    }

    // ==================== Memory Summary for AI ====================

    @GetMapping("/summary")
    public Result<MemorySummaryVo> getMemorySummary() {
        MemorySummaryVo summary = memoryAppService.getMemorySummary();
        return Result.success(summary);
    }
}
