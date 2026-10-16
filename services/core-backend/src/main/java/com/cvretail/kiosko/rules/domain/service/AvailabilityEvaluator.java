package com.cvretail.kiosko.rules.domain.service;

import com.cvretail.kiosko.rules.domain.valueobject.Availability;
import com.cvretail.kiosko.rules.domain.valueobject.CampaignRunPlan;
import com.cvretail.kiosko.rules.domain.valueobject.DailySlot;
import com.cvretail.kiosko.rules.domain.valueobject.KioskId;
import com.cvretail.kiosko.rules.domain.valueobject.ScheduleWindow;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Step 1 of the rules engine: does the calendar and the scope of a campaign allow it to run on a kiosk at a
 * moment. A pure function of the local date and time: it knows nothing about clocks or time zones.
 *
 * <p>The checks run in a fixed order and the first failure is the answer. The part of a slot that crosses
 * midnight belongs to the day on which the slot started (the schedule day), and the dates and the days of the
 * week are checked against that day.
 */
public final class AvailabilityEvaluator {

    public static Availability evaluate(CampaignRunPlan plan, KioskId kiosk, LocalDateTime localNow) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(kiosk, "kiosk");
        Objects.requireNonNull(localNow, "localNow");

        if (!plan.lifecycle().isEligible()) {
            return Availability.STATUS_NOT_ELIGIBLE;
        }
        ScheduleWindow window = plan.window();
        LocalTime time = localNow.toLocalTime();
        LocalDate scheduleDay = scheduleDay(window, localNow.toLocalDate(), time);

        if (window.startDate().isPresent() && scheduleDay.isBefore(window.startDate().get())) {
            return Availability.BEFORE_START_DATE;
        }
        if (window.endDate().isPresent() && scheduleDay.isAfter(window.endDate().get())) {
            return Availability.AFTER_END_DATE;
        }
        if (!window.days().contains(scheduleDay.getDayOfWeek())) {
            return Availability.DAY_NOT_INCLUDED;
        }
        if (window.slot().isPresent() && !window.slot().get().contains(time)) {
            return Availability.OUTSIDE_TIME_SLOT;
        }
        if (!plan.kiosks().includes(kiosk)) {
            return Availability.KIOSK_NOT_INCLUDED;
        }
        return Availability.APPLIES;
    }

    private static LocalDate scheduleDay(ScheduleWindow window, LocalDate date, LocalTime time) {
        if (window.slot().isPresent()) {
            DailySlot slot = window.slot().get();
            if (slot.crossesMidnight() && time.isBefore(slot.end())) {
                return date.minusDays(1);
            }
        }
        return date;
    }

    private AvailabilityEvaluator() {
    }
}
