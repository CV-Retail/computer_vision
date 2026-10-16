package com.cvretail.kiosko.rules.domain.valueobject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** When a campaign may run: optional dates (both included), the days of the week and an optional slot. */
public record ScheduleWindow(
        Optional<LocalDate> startDate,
        Optional<LocalDate> endDate,
        Set<DayOfWeek> days,
        Optional<DailySlot> slot) {

    public ScheduleWindow {
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        Objects.requireNonNull(days, "days");
        Objects.requireNonNull(slot, "slot");
        days = Set.copyOf(days);
        if (days.isEmpty()) {
            throw new IllegalArgumentException("at least one day of the week is required");
        }
        if (startDate.isPresent() && endDate.isPresent() && endDate.get().isBefore(startDate.get())) {
            throw new IllegalArgumentException("the end date must not be before the start date");
        }
    }
}
