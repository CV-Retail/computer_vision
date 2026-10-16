package com.cvretail.kiosko.rules.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CampaignLifecycleTest {

    @Test
    void onlyScheduledAndActiveAreEligible() {
        assertThat(CampaignLifecycle.ACTIVE.isEligible()).isTrue();
        assertThat(CampaignLifecycle.SCHEDULED.isEligible()).isTrue();
        assertThat(CampaignLifecycle.DRAFT.isEligible()).isFalse();
        assertThat(CampaignLifecycle.PAUSED.isEligible()).isFalse();
        assertThat(CampaignLifecycle.FINISHED.isEligible()).isFalse();
    }

    @Test
    void onlyAppliesCountsAsApplying() {
        for (Availability availability : Availability.values()) {
            assertThat(availability.applies()).isEqualTo(availability == Availability.APPLIES);
        }
    }
}
