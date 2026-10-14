package com.cvretail.kiosko.campaigns.bdd;

import com.cvretail.kiosko.campaigns.domain.entity.Campaign;
import com.cvretail.kiosko.campaigns.domain.event.CampaignEvent;
import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** State of one scenario, shared between the step classes by PicoContainer. No static fields. */
public class CampaignWorld {

    String store = "store-1";
    Campaign campaign;
    CampaignSnapshot before;
    CampaignViolationException rejection;
    final List<CampaignEvent> events = new ArrayList<>();

    /** Runs a change as the administrator would: a rejection is kept to be checked by a later step. */
    void attempt(Runnable change) {
        before = campaign == null ? null : campaign.snapshot();
        rejection = null;
        try {
            change.run();
        } catch (CampaignViolationException e) {
            rejection = e;
        }
    }

    /** The result of the last change, in business words. */
    String outcome() {
        if (rejection == null) {
            return "becomes " + campaign.status().name().toLowerCase(Locale.ROOT);
        }
        if (rejection.hasCode(ViolationCodes.STATUS_FINISHED)) {
            return "is rejected: the campaign is finished";
        }
        if (rejection.hasCode(ViolationCodes.STATUS_TRANSITION_NOT_ALLOWED)) {
            return "is rejected: transition not allowed";
        }
        return "is rejected: " + rejection.violations().get(0).code();
    }
}
