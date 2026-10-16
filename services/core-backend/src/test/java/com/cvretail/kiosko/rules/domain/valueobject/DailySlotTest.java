package com.cvretail.kiosko.rules.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class DailySlotTest {

    private static final LocalTime NINE = LocalTime.of(9, 0);
    private static final LocalTime SIX_PM = LocalTime.of(18, 0);

    @Test
    void startAndEndMustDiffer() {
        assertThatThrownBy(() -> new DailySlot(NINE, NINE)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullTimesAreRejected() {
        assertThatThrownBy(() -> new DailySlot(null, NINE)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DailySlot(NINE, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void crossesMidnightOnlyWhenTheStartIsLaterThanTheEnd() {
        assertThat(new DailySlot(NINE, SIX_PM).crossesMidnight()).isFalse();
        assertThat(new DailySlot(SIX_PM, NINE).crossesMidnight()).isTrue();
    }

    @Test
    void aSameDaySlotIncludesTheStartAndExcludesTheEnd() {
        DailySlot slot = new DailySlot(NINE, SIX_PM);
        assertThat(slot.contains(NINE.minusNanos(1))).isFalse();
        assertThat(slot.contains(NINE)).isTrue();
        assertThat(slot.contains(SIX_PM.minusNanos(1))).isTrue();
        assertThat(slot.contains(SIX_PM)).isFalse();
    }

    @Test
    void aSlotThatCrossesMidnightCoversBothSidesAndNotTheGap() {
        DailySlot night = new DailySlot(LocalTime.of(22, 0), LocalTime.of(2, 0));
        assertThat(night.contains(LocalTime.of(21, 59, 59))).isFalse();
        assertThat(night.contains(LocalTime.of(22, 0))).isTrue();
        assertThat(night.contains(LocalTime.MAX)).isTrue();
        assertThat(night.contains(LocalTime.MIDNIGHT)).isTrue();
        assertThat(night.contains(LocalTime.of(1, 59, 59))).isTrue();
        assertThat(night.contains(LocalTime.of(2, 0))).isFalse();
        assertThat(night.contains(LocalTime.NOON)).isFalse();
    }
}
