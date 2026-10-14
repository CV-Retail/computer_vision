package com.cvretail.kiosko.campaigns.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import com.cvretail.kiosko.campaigns.domain.service.CampaignStatusPolicy.Transition;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignStatus;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CampaignStatusPolicyTest {

    private static final CampaignStatus D = CampaignStatus.DRAFT;
    private static final CampaignStatus S = CampaignStatus.SCHEDULED;
    private static final CampaignStatus A = CampaignStatus.ACTIVE;
    private static final CampaignStatus P = CampaignStatus.PAUSED;
    private static final CampaignStatus F = CampaignStatus.FINISHED;

    @Test
    void everyStatusAndTransitionPairFollowsTheTable() {
        Map<Transition, CampaignStatus[]> allowed = new EnumMap<>(Transition.class);
        allowed.put(Transition.SCHEDULE, new CampaignStatus[] {D});
        allowed.put(Transition.ACTIVATE, new CampaignStatus[] {D, S, P});
        allowed.put(Transition.PAUSE, new CampaignStatus[] {S, A});
        allowed.put(Transition.FINISH, new CampaignStatus[] {S, A, P});

        for (Transition transition : Transition.values()) {
            for (CampaignStatus from : CampaignStatus.values()) {
                boolean ok = java.util.Arrays.asList(allowed.get(transition)).contains(from);
                if (ok) {
                    assertThat(CampaignStatusPolicy.requireAllowed(from, transition)).isEqualTo(transition.target());
                } else {
                    String code = from == F ? ViolationCodes.STATUS_FINISHED
                            : ViolationCodes.STATUS_TRANSITION_NOT_ALLOWED;
                    assertThatThrownBy(() -> CampaignStatusPolicy.requireAllowed(from, transition))
                            .isInstanceOfSatisfying(CampaignViolationException.class,
                                    e -> assertThat(e.hasCode(code)).isTrue());
                }
            }
        }
    }

    @Test
    void theMessageNamesTheStatusAndTheTransition() {
        assertThatThrownBy(() -> CampaignStatusPolicy.requireAllowed(D, Transition.PAUSE))
                .hasMessageContaining("pause").hasMessageContaining("draft");
    }
}
