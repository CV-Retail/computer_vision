package com.cvretail.kiosko.rules.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class KioskSelectionTest {

    private static final KioskId A = new KioskId("kiosk-a");
    private static final KioskId B = new KioskId("kiosk-b");

    @Test
    void allKiosksIncludesAnyKiosk() {
        assertThat(new KioskSelection.AllKiosks().includes(A)).isTrue();
        assertThat(new KioskSelection.AllKiosks().includes(new KioskId("anything"))).isTrue();
    }

    @Test
    void selectedKiosksIncludeOnlyTheListedOnes() {
        KioskSelection selection = new KioskSelection.SelectedKiosks(Set.of(A));
        assertThat(selection.includes(A)).isTrue();
        assertThat(selection.includes(B)).isFalse();
    }

    @Test
    void theComparisonIsExactAndCaseSensitive() {
        KioskSelection selection = new KioskSelection.SelectedKiosks(Set.of(new KioskId("Kiosk-A")));
        assertThat(selection.includes(new KioskId("kiosk-a"))).isFalse();
        assertThat(selection.includes(new KioskId("Kiosk-A"))).isTrue();
    }

    @Test
    void selectedKiosksCannotBeEmpty() {
        assertThatThrownBy(() -> new KioskSelection.SelectedKiosks(Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theKiosksAreCopiedDefensively() {
        Set<KioskId> kiosks = new HashSet<>(Set.of(A));
        KioskSelection.SelectedKiosks selection = new KioskSelection.SelectedKiosks(kiosks);
        kiosks.add(B);
        assertThat(selection.kiosks()).containsExactly(A);
    }
}
