package com.cvretail.kiosko.campaigns.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScopeTest {

    private static final KioskId A = new KioskId("kiosk-a");
    private static final KioskId B = new KioskId("kiosk-b");

    @Test
    void allKiosksIsTheDefaultShape() {
        assertThat(Scope.all()).isEqualTo(new Scope.AllKiosks());
    }

    @Test
    void selectedKiosksKeepTheListedOnes() {
        assertThat(Scope.SelectedKiosks.of(List.of(A, B)).kiosks()).containsExactlyInAnyOrder(A, B);
    }

    @Test
    void selectedKiosksCannotBeEmpty() {
        assertThatThrownBy(() -> new Scope.SelectedKiosks(Set.of())).isInstanceOfSatisfying(
                CampaignViolationException.class, e -> assertThat(e.hasCode(ViolationCodes.SCOPE_KIOSKS_EMPTY)).isTrue());
        assertThatThrownBy(() -> Scope.SelectedKiosks.of(List.of())).isInstanceOf(CampaignViolationException.class);
    }

    @Test
    void repeatedKiosksAreRejected() {
        assertThatThrownBy(() -> Scope.SelectedKiosks.of(List.of(A, A))).isInstanceOfSatisfying(
                CampaignViolationException.class,
                e -> assertThat(e.hasCode(ViolationCodes.SCOPE_KIOSKS_DUPLICATE)).isTrue());
    }
}
