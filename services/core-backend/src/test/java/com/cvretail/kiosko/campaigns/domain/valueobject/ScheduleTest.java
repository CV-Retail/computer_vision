package com.cvretail.kiosko.campaigns.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScheduleTest {

    private static final LocalDate D1 = LocalDate.of(2030, 1, 10);
    private static final LocalDate D2 = LocalDate.of(2030, 1, 20);

    private static void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(CampaignViolationException.class,
                e -> assertThat(e.hasCode(code)).isTrue());
    }

    @Test
    void alwaysRunsEveryDayAllDayWithoutDates() {
        Schedule always = Schedule.always();
        assertThat(always.days()).containsExactlyInAnyOrder(DayOfWeek.values());
        assertThat(always.startDate()).isEmpty();
        assertThat(always.endDate()).isEmpty();
        assertThat(always.slot()).isEmpty();
    }

    @Test
    void endDateMayEqualButNotPrecedeTheStartDate() {
        assertThat(Schedule.always().withDates(Optional.of(D1), Optional.of(D1)).endDate()).contains(D1);
        assertCode(() -> Schedule.always().withDates(Optional.of(D2), Optional.of(D1)),
                ViolationCodes.SCHEDULE_END_BEFORE_START);
    }

    @Test
    void oneDateAloneIsAccepted() {
        assertThat(Schedule.always().withDates(Optional.empty(), Optional.of(D1)).endDate()).contains(D1);
    }

    @Test
    void daysCannotBeEmpty() {
        assertCode(() -> Schedule.always().withDays(Set.of()), ViolationCodes.SCHEDULE_DAYS_EMPTY);
        assertThat(Schedule.always().withDays(Set.of(DayOfWeek.MONDAY)).days()).containsExactly(DayOfWeek.MONDAY);
    }

    @Test
    void timeSlotMustHaveDifferentStartAndEndButMayCrossMidnight() {
        assertCode(() -> new TimeSlot(LocalTime.NOON, LocalTime.NOON), ViolationCodes.SCHEDULE_SLOT_EMPTY_RANGE);
        TimeSlot night = new TimeSlot(LocalTime.of(22, 0), LocalTime.of(2, 0));
        assertThat(Schedule.always().withSlot(Optional.of(night)).slot()).contains(night);
    }
}
