package com.tripdesigner.experience.api;

import com.tripdesigner.common.response.Result;
import com.tripdesigner.experience.api.dto.CreateExperienceRequest;
import com.tripdesigner.experience.api.dto.UpdateExperienceRequest;
import com.tripdesigner.experience.api.vo.ExperienceVo;
import com.tripdesigner.experience.application.ExperienceAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for trip experiences.
 * All endpoints require authentication (handled in application service).
 */
@RestController
@RequestMapping("/experiences")
@RequiredArgsConstructor
public class ExperienceController {

    private final ExperienceAppService experienceAppService;

    @PostMapping
    public Result<ExperienceVo> create(@Valid @RequestBody CreateExperienceRequest request) {
        ExperienceVo vo = experienceAppService.create(request);
        return Result.success(vo);
    }

    @GetMapping("/{id}")
    public Result<ExperienceVo> get(@PathVariable Long id) {
        ExperienceVo vo = experienceAppService.get(id);
        return Result.success(vo);
    }

    @GetMapping
    public Result<List<ExperienceVo>> list() {
        List<ExperienceVo> list = experienceAppService.list();
        return Result.success(list);
    }

    @GetMapping("/trip/{tripId}")
    public Result<List<ExperienceVo>> listForTrip(@PathVariable Long tripId) {
        List<ExperienceVo> list = experienceAppService.listForTrip(tripId);
        return Result.success(list);
    }

    @PutMapping("/{id}")
    public Result<ExperienceVo> update(@PathVariable Long id,
                                        @Valid @RequestBody UpdateExperienceRequest request) {
        ExperienceVo vo = experienceAppService.update(id, request);
        return Result.success(vo);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        experienceAppService.delete(id);
        return Result.success();
    }
}
