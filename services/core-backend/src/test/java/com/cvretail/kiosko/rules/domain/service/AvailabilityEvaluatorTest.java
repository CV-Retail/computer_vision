package com.cvretail.kiosko.rules.domain.service;

import static com.cvretail.kiosko.rules.domain.valueobject.Availability.AFTER_END_DATE;
import static com.cvretail.kiosko.rules.domain.valueobject.Availability.APPLIES;
import static com.cvretail.kiosko.rules.domain.valueobject.Availability.BEFORE_START_DATE;
import static com.cvretail.kiosko.rules.domain.valueobject.Availability.DAY_NOT_INCLUDED;
import static com.cvretail.kiosko.rules.domain.valueobject.Availability.KIOSK_NOT_INCLUDED;
import static com.cvretail.kiosko.rules.domain.valueobject.Availability.OUTSIDE_TIME_SLOT;
import static com.cvretail.kiosko.rules.domain.valueobject.Availability.STATUS_NOT_ELIGIBLE;
import static org.assertj.core.api.Assertions.assertThat;

import com.cvretail.kiosko.rules.domain.valueobject.Availability;
import com.cvretail.kiosko.rules.domain.valueobject.CampaignLifecycle;
import com.cvretail.kiosko.rules.domain.valueobject.CampaignRunPlan;
import com.cvretail.kiosko.rules.domain.valueobject.DailySlot;
import com.cvretail.kiosko.rules.domain.valueobject.KioskId;
import com.cvretail.kiosko.rules.domain.valueobject.KioskSelection;
import com.cvretail.kiosko.rules.domain.valueobject.ScheduleWindow;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AvailabilityEvaluatorTest {

    private static final KioskId KIOSK = new KioskId("kiosk-a");
    private static final Set<DayOfWeek> EVERY_DAY = EnumSet.allOf(DayOfWeek.class);

    /** 2030-01-14 is a Monday. */
    private static LocalDateTime at(String date, String time) {
        return LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time));
    }

    private static CampaignRunPlan plan(CampaignLifecycle lifecycle, ScheduleWindow window, KioskSelection kiosks) {
        return new CampaignRunPlan(UUID.randomUUID(), lifecycle, window, kiosks);
    }

    private static CampaignRunPlan active(ScheduleWindow window) {
        return plan(CampaignLifecycle.ACTIVE, window, new KioskSelection.AllKiosks());
    }

    private static ScheduleWindow window(String start, String end, Set<DayOfWeek> days, DailySlot slot) {
        return new ScheduleWindow(
                Optional.ofNullable(start).map(LocalDate::parse),
                Optional.ofNullable(end).map(LocalDate::parse),
                days,
                Optional.ofNullable(slot));
    }

    private static DailySlot slot(String start, String end) {
        return new DailySlot(LocalTime.parse(start), LocalTime.parse(end));
    }

    private static Availability evaluate(CampaignRunPlan plan, LocalDateTime now) {
        return AvailabilityEvaluator.evaluate(plan, KIOSK, now);
    }

    @Test
    void everyStatusIsDecidedByTheLifecycle() {
        ScheduleWindow always = window(null, null, EVERY_DAY, null);
        LocalDateTime now = at("2030-01-14", "12:00");
        for (CampaignLifecycle lifecycle : CampaignLifecycle.values()) {
            Availability expected = lifecycle.isEligible() ? APPLIES : STATUS_NOT_ELIGIBLE;
            assertThat(evaluate(plan(lifecycle, always, new KioskSelection.AllKiosks()), now))
                    .as(lifecycle.name()).isEqualTo(expected);
        }
    }

    @Test
    void theStartDateIsIncludedFromItsFirstMoment() {
        CampaignRunPlan plan = active(window("2030-01-14", null, EVERY_DAY, null));
        assertThat(evaluate(plan, at("2030-01-13", "23:59:59"))).isEqualTo(BEFORE_START_DATE);
        assertThat(evaluate(plan, at("2030-01-14", "00:00"))).isEqualTo(APPLIES);
    }

    @Test
    void theEndDateIsIncludedUntilItsLastMoment() {
        CampaignRunPlan plan = active(window(null, "2030-01-14", EVERY_DAY, null));
        assertThat(evaluate(plan, at("2030-01-14", "23:59:59"))).isEqualTo(APPLIES);
        assertThat(evaluate(plan, at("2030-01-15", "00:00"))).isEqualTo(AFTER_END_DATE);
    }

    @Test
    void noDatesMeansNoLimit() {
        CampaignRunPlan plan = active(window(null, null, EVERY_DAY, null));
        assertThat(evaluate(plan, at("1999-12-31", "00:00"))).isEqualTo(APPLIES);
        assertThat(evaluate(plan, at("2099-12-31", "23:59:59"))).isEqualTo(APPLIES);
    }

    @Test
    void eachDayOfTheWeekIsIncludedOrExcluded() {
        for (DayOfWeek day : DayOfWeek.values()) {
            CampaignRunPlan plan = active(window(null, null, Set.of(day), null));
            for (int offset = 0; offset < 7; offset++) {
                LocalDateTime now = at("2030-01-14", "12:00").plusDays(offset);
                Availability expected = now.getDayOfWeek() == day ? APPLIES : DAY_NOT_INCLUDED;
                assertThat(evaluate(plan, now)).as("%s on %s", day, now.getDayOfWeek()).isEqualTo(expected);
            }
        }
    }

    @Test
    void theDayChangesAtMidnightSharp() {
        CampaignRunPlan mondayOnly = active(window(null, null, Set.of(DayOfWeek.MONDAY), null));
        assertThat(evaluate(mondayOnly, at("2030-01-13", "23:59:59"))).isEqualTo(DAY_NOT_INCLUDED);
        assertThat(evaluate(mondayOnly, at("2030-01-14", "00:00"))).isEqualTo(APPLIES);
        assertThat(evaluate(mondayOnly, at("2030-01-14", "23:59:59.999999999"))).isEqualTo(APPLIES);
        assertThat(evaluate(mondayOnly, at("2030-01-15", "00:00"))).isEqualTo(DAY_NOT_INCLUDED);
    }

    @Test
    void aSameDaySlotIncludesItsStartAndExcludesItsEnd() {
        CampaignRunPlan plan = active(window(null, null, EVERY_DAY, slot("09:00", "18:00")));
        assertThat(evaluate(plan, at("2030-01-14", "08:59:59"))).isEqualTo(OUTSIDE_TIME_SLOT);
        assertThat(evaluate(plan, at("2030-01-14", "09:00"))).isEqualTo(APPLIES);
        assertThat(evaluate(plan, at("2030-01-14", "17:59:59"))).isEqualTo(APPLIES);
        assertThat(evaluate(plan, at("2030-01-14", "17:59:59.999999999"))).isEqualTo(APPLIES);
        assertThat(evaluate(plan, at("2030-01-14", "18:00"))).isEqualTo(OUTSIDE_TIME_SLOT);
    }

    @Test
    void twoConsecutiveSlotsNeitherOverlapNorLeaveAGap() {
        CampaignRunPlan morning = active(window(null, null, EVERY_DAY, slot("08:00", "12:00")));
        CampaignRunPlan afternoon = active(window(null, null, EVERY_DAY, slot("12:00", "20:00")));
        LocalDateTime noon = at("2030-01-14", "12:00");
        assertThat(evaluate(morning, noon)).isEqualTo(OUTSIDE_TIME_SLOT);
        assertThat(evaluate(afternoon, noon)).isEqualTo(APPLIES);
    }

    /** Monday only, from and to 2030-01-14, slot 22:00 to 02:00: the table of design 3.2. */
    @Test
    void aSlotThatCrossesMidnightBelongsToTheDayItStarted() {
        CampaignRunPlan plan = active(window("2030-01-14", "2030-01-14", Set.of(DayOfWeek.MONDAY), slot("22:00", "02:00")));
        assertThat(evaluate(plan, at("2030-01-14", "21:59"))).isEqualTo(OUTSIDE_TIME_SLOT);
        assertThat(evaluate(plan, at("2030-01-14", "22:00"))).isEqualTo(APPLIES);
        assertThat(evaluate(plan, at("2030-01-14", "23:59:59"))).isEqualTo(APPLIES);
        assertThat(evaluate(plan, at("2030-01-15", "00:00"))).isEqualTo(APPLIES);
        assertThat(evaluate(plan, at("2030-01-15", "01:59:59"))).isEqualTo(APPLIES);
        assertThat(evaluate(plan, at("2030-01-15", "02:00"))).isEqualTo(AFTER_END_DATE);
        assertThat(evaluate(plan, at("2030-01-14", "01:00"))).isEqualTo(BEFORE_START_DATE);
    }

    @Test
    void theAfterMidnightPartUsesTheDayOfTheWeekOfTheDayTheSlotStarted() {
        CampaignRunPlan mondayNight = active(window(null, null, Set.of(DayOfWeek.MONDAY), slot("22:00", "02:00")));
        assertThat(evaluate(mondayNight, at("2030-01-15", "01:00"))).isEqualTo(APPLIES);
        CampaignRunPlan tuesdayNight = active(window(null, null, Set.of(DayOfWeek.TUESDAY), slot("22:00", "02:00")));
        assertThat(evaluate(tuesdayNight, at("2030-01-15", "01:00"))).isEqualTo(DAY_NOT_INCLUDED);
    }

    @Test
    void theGapOfACrossingSlotKeepsTheCalendarDay() {
        CampaignRunPlan plan = active(window(null, null, EVERY_DAY, slot("22:00", "02:00")));
        assertThat(evaluate(plan, at("2030-01-14", "12:00"))).isEqualTo(OUTSIDE_TIME_SLOT);
        assertThat(evaluate(plan, at("2030-01-14", "02:00"))).isEqualTo(OUTSIDE_TIME_SLOT);
    }

    @Test
    void slotsAreComparedDownToTheNanosecond() {
        CampaignRunPlan plan = active(window(null, null, EVERY_DAY, slot("09:00:00.5", "09:00:01")));
        assertThat(evaluate(plan, at("2030-01-14", "09:00:00.499999999"))).isEqualTo(OUTSIDE_TIME_SLOT);
        assertThat(evaluate(plan, at("2030-01-14", "09:00:00.5"))).isEqualTo(APPLIES);
        assertThat(evaluate(plan, at("2030-01-14", "09:00:01"))).isEqualTo(OUTSIDE_TIME_SLOT);
    }

    @Test
    void theKioskMustBeIncluded() {
        ScheduleWindow always = window(null, null, EVERY_DAY, null);
        CampaignRunPlan selected = plan(CampaignLifecycle.ACTIVE, always,
                new KioskSelection.SelectedKiosks(Set.of(KIOSK)));
        LocalDateTime now = at("2030-01-14", "12:00");
        assertThat(evaluate(selected, now)).isEqualTo(APPLIES);
        assertThat(AvailabilityEvaluator.evaluate(selected, new KioskId("kiosk-b"), now)).isEqualTo(KIOSK_NOT_INCLUDED);
    }

    @Test
    void whenSeveralReasonsHoldTheFirstOneInTheOrderOfTheChecksWins() {
        CampaignRunPlan everything = plan(CampaignLifecycle.PAUSED,
                window("2030-02-01", "2030-02-02", Set.of(DayOfWeek.SUNDAY), slot("09:00", "10:00")),
                new KioskSelection.SelectedKiosks(Set.of(new KioskId("kiosk-b"))));
        LocalDateTime monday = at("2030-01-14", "12:00");
        assertThat(evaluate(everything, monday)).isEqualTo(STATUS_NOT_ELIGIBLE);

        CampaignRunPlan active = plan(CampaignLifecycle.ACTIVE, everything.window(), everything.kiosks());
        assertThat(evaluate(active, monday)).isEqualTo(BEFORE_START_DATE);
        assertThat(evaluate(active, at("2030-03-01", "12:00"))).isEqualTo(AFTER_END_DATE);

        CampaignRunPlan open = plan(CampaignLifecycle.ACTIVE,
                window(null, null, Set.of(DayOfWeek.SUNDAY), slot("09:00", "10:00")), everything.kiosks());
        assertThat(evaluate(open, monday)).isEqualTo(DAY_NOT_INCLUDED);

        CampaignRunPlan anyDay = plan(CampaignLifecycle.ACTIVE,
                window(null, null, EVERY_DAY, slot("09:00", "10:00")), everything.kiosks());
        assertThat(evaluate(anyDay, monday)).isEqualTo(OUTSIDE_TIME_SLOT);
        assertThat(evaluate(anyDay, at("2030-01-14", "09:30"))).isEqualTo(KIOSK_NOT_INCLUDED);
    }

    @Test
    void theSameInputsAlwaysGiveTheSameAnswer() {
        CampaignRunPlan plan = active(window("2030-01-01", "2030-12-31", EVERY_DAY, slot("22:00", "02:00")));
        LocalDateTime now = at("2030-06-15", "23:30");
        Availability first = evaluate(plan, now);
        for (int i = 0; i < 100; i++) {
            assertThat(evaluate(plan, now)).isEqualTo(first);
        }
    }
}
