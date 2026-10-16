package com.cvretail.kiosko.rules.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class KioskIdTest {

    @Test
    void keepsTheValueExactly() {
        assertThat(new KioskId("Kiosk-A").value()).isEqualTo("Kiosk-A");
        assertThat(new KioskId("kiosk-a")).isNotEqualTo(new KioskId("Kiosk-A"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void blankIdsAreRejected(String value) {
        assertThatThrownBy(() -> new KioskId(value)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullIsRejected() {
        assertThatThrownBy(() -> new KioskId(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
