package com.tripdesigner.trip;

import com.tripdesigner.auth.application.AuthAppService;
import com.tripdesigner.common.security.JwtUtil;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.support.IntegrationTest;
import com.tripdesigner.trip.api.dto.*;
import com.tripdesigner.trip.api.vo.TripDetailVo;
import com.tripdesigner.trip.api.vo.TripVo;
import com.tripdesigner.trip.application.TripAppService;
import com.tripdesigner.trip.application.TripActivityAppService;
import com.tripdesigner.trip.application.TripDayAppService;
import com.tripdesigner.trip.domain.TripStatus;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class TripIntegrationTest extends IntegrationTest {

    private final TripAppService tripService;
    private final TripDayAppService tripDayService;
    private final TripActivityAppService tripActivityService;
    private final AuthAppService authService;
    private final JwtUtil jwt;

    protected TripIntegrationTest(TripAppService tripService,
                                   TripDayAppService tripDayService,
                                   TripActivityAppService tripActivityService,
                                   AuthAppService authService,
                                   JwtUtil jwt) {
        this.tripService = tripService;
        this.tripDayService = tripDayService;
        this.tripActivityService = tripActivityService;
        this.authService = authService;
        this.jwt = jwt;
    }

    @Test
    void fullTripCrud() {
        // 1. Register & login
        var token = authService.register("trip-test@example.com", "password123");
        var claims = jwt.parse(token.getAccessToken());
        UserContextHolder.set(new UserContext(claims.get("uid", Long.class), claims.getSubject()));

        try {
            // 2. Create trip
            CreateTripRequest createReq = new CreateTripRequest();
            createReq.setTitle("Tokyo Trip");
            createReq.setDescription("A 5-day trip to Tokyo");
            createReq.setDestinationName("Tokyo");
            createReq.setStartDate(LocalDate.of(2025, 6, 1));
            createReq.setEndDate(LocalDate.of(2025, 6, 5));
            createReq.setBudget(5000);
            TripVo created = tripService.create(createReq);
            assertNotNull(created.getId());
            assertEquals("Tokyo Trip", created.getTitle());

            // 3. Get trip detail
            TripDetailVo detail = tripService.get(created.getId());
            assertEquals("Tokyo Trip", detail.getTrip().getTitle());
            assertTrue(detail.getDays().isEmpty());

            // 4. Create trip day
            CreateTripDayRequest dayReq = new CreateTripDayRequest();
            dayReq.setDayNumber(1);
            dayReq.setDate(LocalDate.of(2025, 6, 1));
            dayReq.setTitle("Day 1 - Arrival");
            dayReq.setDescription("Arrive at Haneda Airport");
            var day = tripDayService.create(created.getId(), dayReq);
            assertNotNull(day.getId());
            assertEquals("Day 1 - Arrival", day.getTitle());

            // 5. Create activity
            CreateTripActivityRequest actReq = new CreateTripActivityRequest();
            actReq.setName("Visit Sensoji");
            actReq.setStartTime(LocalTime.of(10, 0));
            actReq.setEndTime(LocalTime.of(12, 0));
            actReq.setCategory("sightseeing");
            actReq.setPlace("Asakusa");
            var activity = tripActivityService.create(created.getId(), day.getId(), actReq);
            assertNotNull(activity.getId());
            assertEquals("Visit Sensoji", activity.getName());

            // 6. List activities
            var activities = tripActivityService.list(created.getId(), day.getId());
            assertEquals(1, activities.size());
            assertEquals("Visit Sensoji", activities.get(0).getName());

            // 7. Update trip
            UpdateTripRequest updateReq = new UpdateTripRequest();
            updateReq.setTitle("Updated Tokyo Trip");
            var updated = tripService.update(created.getId(), updateReq);
            assertEquals("Updated Tokyo Trip", updated.getTitle());

            // 8. Update status
            var statusUpdated = tripService.updateStatus(created.getId(), TripStatus.PLANNED);
            assertEquals("PLANNED", statusUpdated.getStatus());

            // 9. Delete trip
            tripService.delete(created.getId());
            assertThrows(Exception.class, () -> tripService.get(created.getId()));
        } finally {
            UserContextHolder.clear();
        }
    }
}
