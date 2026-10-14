package com.cvretail.kiosko.campaigns.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MinimumOnScreenTimeTest {

    @Test
    void defaultsToEightSeconds() {
        assertThat(MinimumOnScreenTime.defaultValue().seconds()).isEqualTo(8);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 8, 3600})
    void acceptsValuesInRange(int seconds) {
        assertThat(new MinimumOnScreenTime(seconds).seconds()).isEqualTo(seconds);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 3601})
    void rejectsValuesOutOfRange(int seconds) {
        assertThatThrownBy(() -> new MinimumOnScreenTime(seconds)).isInstanceOfSatisfying(
                CampaignViolationException.class,
                e -> assertThat(e.hasCode(ViolationCodes.MINIMUM_ON_SCREEN_OUT_OF_RANGE)).isTrue());
    }
}
