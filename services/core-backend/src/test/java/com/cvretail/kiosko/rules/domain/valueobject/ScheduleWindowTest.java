package com.cvretail.kiosko.rules.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScheduleWindowTest {

    private static final LocalDate D1 = LocalDate.of(2030, 1, 10);
    private static final LocalDate D2 = LocalDate.of(2030, 1, 20);

    private static ScheduleWindow window(Optional<LocalDate> start, Optional<LocalDate> end, Set<DayOfWeek> days) {
        return new ScheduleWindow(start, end, days, Optional.empty());
    }

    @Test
    void anOpenEndedWindowIsValid() {
        assertThat(window(Optional.empty(), Optional.empty(), EnumSet.allOf(DayOfWeek.class)).days()).hasSize(7);
    }

    @Test
    void theEndDateMayEqualButNotPrecedeTheStartDate() {
        assertThat(window(Optional.of(D1), Optional.of(D1), Set.of(DayOfWeek.MONDAY)).endDate()).contains(D1);
        assertThatThrownBy(() -> window(Optional.of(D2), Optional.of(D1), Set.of(DayOfWeek.MONDAY)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void atLeastOneDayIsRequired() {
        assertThatThrownBy(() -> window(Optional.empty(), Optional.empty(), Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullComponentsAreRejected() {
        assertThatThrownBy(() -> new ScheduleWindow(null, Optional.empty(), Set.of(DayOfWeek.MONDAY), Optional.empty()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ScheduleWindow(Optional.empty(), Optional.empty(), null, Optional.empty()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void theDaysAreCopiedDefensively() {
        Set<DayOfWeek> days = new HashSet<>(Set.of(DayOfWeek.MONDAY));
        ScheduleWindow window = window(Optional.empty(), Optional.empty(), days);
        days.add(DayOfWeek.FRIDAY);
        assertThat(window.days()).containsExactly(DayOfWeek.MONDAY);
    }
}
