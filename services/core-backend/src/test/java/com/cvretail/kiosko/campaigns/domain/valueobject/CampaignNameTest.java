package com.cvretail.kiosko.campaigns.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import org.junit.jupiter.api.Test;

class CampaignNameTest {

    @Test
    void trimsTheName() {
        assertThat(new CampaignName("  Summer sale ").value()).isEqualTo("Summer sale");
    }

    @Test
    void acceptsTheMaximumLength() {
        assertThat(new CampaignName("x".repeat(100)).value()).hasSize(100);
    }

    @Test
    void rejectsBlankNames() {
        for (String blank : new String[] {"", "   ", "\t"}) {
            assertThatThrownBy(() -> new CampaignName(blank)).isInstanceOfSatisfying(
                    CampaignViolationException.class, e -> assertThat(e.hasCode(ViolationCodes.NAME_BLANK)).isTrue());
        }
    }

    @Test
    void rejectsNamesOver100CharactersAfterTrimming() {
        assertThatThrownBy(() -> new CampaignName("x".repeat(101))).isInstanceOfSatisfying(
                CampaignViolationException.class, e -> assertThat(e.hasCode(ViolationCodes.NAME_TOO_LONG)).isTrue());
        assertThat(new CampaignName(" " + "x".repeat(100) + " ").value()).hasSize(100);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new CampaignName(null)).hasMessageContaining("name.required");
    }
}
