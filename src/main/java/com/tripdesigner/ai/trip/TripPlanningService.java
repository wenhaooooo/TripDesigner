package com.tripdesigner.ai.trip;

import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.trip.api.vo.TripDetailVo;
import com.tripdesigner.trip.application.TripAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripPlanningService {

    private final TripPlannerAgent tripPlannerAgent;
    private final TripAppService tripAppService;

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }

    /**
     * Generate a new trip based on the user's prompt.
     * Creates a conversation (or uses an existing one) and runs the agent.
     */
    @Transactional
    public TripGenerationResult generate(Long conversationId, String prompt) {
        UserContext ctx = requireAuth();
        return tripPlannerAgent.generate(ctx.userId(), ctx.email(), conversationId, prompt);
    }

    /**
     * Chat with an existing trip to refine or discuss it.
     */
    @Transactional
    public String chat(Long conversationId, Long tripId, String prompt) {
        UserContext ctx = requireAuth();
        // Verify user owns the trip
        verifyTripOwner(tripId, ctx.userId());
        return tripPlannerAgent.chat(ctx.userId(), ctx.email(), conversationId, tripId, prompt);
    }

    /**
     * Get the generated trip detail.
     */
    @Transactional(readOnly = true)
    public TripDetailVo getTripDetail(Long tripId) {
        UserContext ctx = requireAuth();
        return tripAppService.getDetailForUser(tripId, ctx.userId());
    }

    private void verifyTripOwner(Long tripId, Long userId) {
        tripAppService.getDetailForUser(tripId, userId);  // throws if not found or not owner
    }
}
