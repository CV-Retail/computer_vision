package com.cvretail.kiosko.campaigns;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Read-only picture of a campaign for other modules. Made of simple values and its own types, so it does not
 * expose the aggregate or anything from the internal packages.
 */
public record CampaignView(
        UUID id,
        String storeId,
        Type type,
        Status status,
        Optional<Priority> priority,
        Schedule schedule,
        Scope scope,
        Optional<Integer> minimumOnScreenSeconds,
        Optional<UUID> audienceRuleId,
        Set<UUID> manualVetoIds,
        List<PlaylistItem> playlist) {

    public enum Type { REGULAR, DEFAULT }

    public enum Status { DRAFT, SCHEDULED, ACTIVE, PAUSED, FINISHED }

    public enum Priority { HIGH, MEDIUM, LOW }

    public record PlaylistItem(String mediaRef, int durationSeconds) {
    }

    public record Schedule(Optional<LocalDate> startDate, Optional<LocalDate> endDate, Set<DayOfWeek> days,
            Optional<TimeSlot> slot) {
    }

    public record TimeSlot(LocalTime start, LocalTime end) {
    }

    /** All kiosks of the store when {@code allKiosks} is true, otherwise the listed ones. */
    public record Scope(boolean allKiosks, Set<String> kioskIds) {
    }
}
