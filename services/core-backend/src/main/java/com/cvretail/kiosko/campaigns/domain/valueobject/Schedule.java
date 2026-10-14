package com.cvretail.kiosko.campaigns.domain.valueobject;

import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

/** When a campaign runs: optional dates, a non-empty set of days and an optional time slot. */
public record Schedule(Optional<LocalDate> startDate, Optional<LocalDate> endDate,
        Set<DayOfWeek> days, Optional<TimeSlot> slot) {

    public Schedule {
        Require.notNull(startDate, "startDate");
        Require.notNull(endDate, "endDate");
        Require.notNull(days, "days");
        Require.notNull(slot, "slot");
        days = Set.copyOf(days);
        if (days.isEmpty()) {
            throw new CampaignViolationException("days", ViolationCodes.SCHEDULE_DAYS_EMPTY,
                    "at least one day of the week is required");
        }
        if (startDate.isPresent() && endDate.isPresent() && endDate.get().isBefore(startDate.get())) {
            throw new CampaignViolationException("endDate", ViolationCodes.SCHEDULE_END_BEFORE_START,
                    "the end date must not be before the start date");
        }
    }

    /** Every day, all day, with no dates. */
    public static Schedule always() {
        return new Schedule(Optional.empty(), Optional.empty(), Set.of(DayOfWeek.values()), Optional.empty());
    }

    public Schedule withDates(Optional<LocalDate> start, Optional<LocalDate> end) {
        return new Schedule(start, end, days, slot);
    }

    public Schedule withDays(Set<DayOfWeek> newDays) {
        return new Schedule(startDate, endDate, newDays, slot);
    }

    public Schedule withSlot(Optional<TimeSlot> newSlot) {
        return new Schedule(startDate, endDate, days, newSlot);
    }
}
