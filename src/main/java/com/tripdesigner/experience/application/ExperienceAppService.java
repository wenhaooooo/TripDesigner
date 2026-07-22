package com.tripdesigner.experience.application;

import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.experience.api.dto.CreateExperienceRequest;
import com.tripdesigner.experience.api.dto.UpdateExperienceRequest;
import com.tripdesigner.experience.api.vo.ExperienceVo;
import com.tripdesigner.experience.domain.ExperienceRepository;
import com.tripdesigner.experience.domain.TripExperience;
import com.tripdesigner.trip.domain.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for trip experiences.
 * Handles business logic for creating, reading, updating, and deleting user experiences.
 */
@Service
@RequiredArgsConstructor
public class ExperienceAppService {

    private final ExperienceRepository experienceRepository;
    private final TripRepository tripRepository;

    @Transactional
    public ExperienceVo create(CreateExperienceRequest request) {
        UserContext ctx = requireAuth();

        // Verify the trip belongs to the user
        tripRepository.findById(request.getTripId())
                .filter(trip -> trip.getUserId().equals(ctx.userId()))
                .orElseThrow(() -> new BizException(ResultCode.PERMISSION_DENIED, "Trip not found"));

        TripExperience experience = TripExperience.create(
                ctx.userId(), request.getTripId(), request.getTitle(), request.getContent(),
                request.getRating(), request.getTags(), request.getMediaUrls()
        );

        // Associate with day or activity if specified
        if (request.getTripDayId() != null) {
            experience = experience.withDay(request.getTripDayId());
        }
        if (request.getTripActivityId() != null) {
            experience = experience.withActivity(request.getTripActivityId());
        }

        experience = experienceRepository.save(experience);
        return toVo(experience);
    }

    @Transactional
    public ExperienceVo update(Long experienceId, UpdateExperienceRequest request) {
        UserContext ctx = requireAuth();

        TripExperience existing = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new BizException(ResultCode.EXPERIENCE_NOT_FOUND, "Experience not found"));

        if (!existing.getUserId().equals(ctx.userId())) {
            throw new BizException(ResultCode.PERMISSION_DENIED);
        }

        TripExperience updated = existing.withUpdatedFields(
                request.getTitle(), request.getContent(), request.getRating(),
                request.getTags(), request.getMediaUrls()
        );

        updated = experienceRepository.save(updated);
        return toVo(updated);
    }

    public ExperienceVo get(Long experienceId) {
        UserContext ctx = requireAuth();

        TripExperience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new BizException(ResultCode.EXPERIENCE_NOT_FOUND, "Experience not found"));

        if (!experience.getUserId().equals(ctx.userId())) {
            throw new BizException(ResultCode.PERMISSION_DENIED);
        }

        return toVo(experience);
    }

    public List<ExperienceVo> list() {
        UserContext ctx = requireAuth();
        return experienceRepository.findByUserId(ctx.userId()).stream()
                .map(this::toVo)
                .toList();
    }

    public List<ExperienceVo> listForTrip(Long tripId) {
        UserContext ctx = requireAuth();

        // Verify the trip belongs to the user
        tripRepository.findById(tripId)
                .filter(trip -> trip.getUserId().equals(ctx.userId()))
                .orElseThrow(() -> new BizException(ResultCode.PERMISSION_DENIED, "Trip not found"));

        return experienceRepository.findByTripId(tripId).stream()
                .map(this::toVo)
                .toList();
    }

    @Transactional
    public void delete(Long experienceId) {
        UserContext ctx = requireAuth();

        TripExperience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new BizException(ResultCode.EXPERIENCE_NOT_FOUND, "Experience not found"));

        if (!experience.getUserId().equals(ctx.userId())) {
            throw new BizException(ResultCode.PERMISSION_DENIED);
        }

        experienceRepository.delete(experienceId);
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }

    private ExperienceVo toVo(TripExperience experience) {
        ExperienceVo vo = new ExperienceVo();
        vo.setId(experience.getId());
        vo.setTripId(experience.getTripId());
        vo.setTripDayId(experience.getTripDayId());
        vo.setTripActivityId(experience.getTripActivityId());
        vo.setTitle(experience.getTitle());
        vo.setContent(experience.getContent());
        vo.setRating(experience.getRating());
        vo.setTags(experience.getTags());
        vo.setMediaUrls(experience.getMediaUrls());
        vo.setStatus(experience.getStatus().name());
        vo.setCreatedAt(experience.getCreatedAt());
        vo.setUpdatedAt(experience.getUpdatedAt());
        return vo;
    }
}
