package com.tripdesigner.trip.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TripTest {

    @Test
    void create_sets_draft_status_and_zero_version() {
        Trip t = Trip.create(1L, "Tokyo Trip", "A short trip", "Tokyo",
                LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 5), 5000);
        assertNull(t.getId());
        assertEquals("Tokyo Trip", t.getTitle());
        assertEquals(TripStatus.DRAFT, t.getStatus());
        assertEquals(0, t.getVersion());
        assertEquals(LocalDate.of(2025, 6, 5), t.getEndDate());
    }

    @Test
    void withUpdatedStatus_changes_status_keeps_other_fields() {
        Trip t = Trip.create(1L, "Test", null, null, null, null, null);
        Trip updated = t.withUpdatedStatus(TripStatus.PLANNED);
        assertEquals(TripStatus.PLANNED, updated.getStatus());
        assertEquals("Test", updated.getTitle());
        assertNull(updated.getEndDate());
    }

    @Test
    void withUpdatedFields_updates_non_null_keeps_null() {
        Trip t = Trip.create(1L, "Old Title", "old desc", "Old Dest",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 5), 1000);
        Trip updated = t.withUpdatedFields("New Title", null, null, null, null, null);
        assertEquals("New Title", updated.getTitle());
        assertEquals("old desc", updated.getDescription());
        assertEquals("Old Dest", updated.getDestinationName());
    }

    @Test
    void tripDay_create_sets_version_zero() {
        TripDay d = TripDay.create(1L, 1, LocalDate.of(2025, 6, 1), "Day 1", "First day");
        assertNull(d.getId());
        assertEquals(1L, d.getTripId());
        assertEquals(LocalDate.of(2025, 6, 1), d.getDate());
        assertEquals(0, d.getVersion());
    }

    @Test
    void tripActivity_create_sets_sort_order_zero() {
        TripActivity a = TripActivity.create(1L, "Visit temple", null, null, "sightseeing", "Sensoji", null);
        assertNull(a.getId());
        assertEquals("Visit temple", a.getName());
        assertEquals(0, a.getSortOrder());
        assertEquals(0, a.getVersion());
    }
}
