package com.cvretail.kiosko.campaigns.domain.service;

import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignStatus;
import java.util.EnumSet;
import java.util.Set;

/** Which status transition is allowed from which status. */
public final class CampaignStatusPolicy {

    /** The four transitions of the life cycle, with where they start and where they end. */
    public enum Transition {
        SCHEDULE(CampaignStatus.SCHEDULED, CampaignStatus.DRAFT),
        ACTIVATE(CampaignStatus.ACTIVE, CampaignStatus.DRAFT, CampaignStatus.SCHEDULED, CampaignStatus.PAUSED),
        PAUSE(CampaignStatus.PAUSED, CampaignStatus.SCHEDULED, CampaignStatus.ACTIVE),
        FINISH(CampaignStatus.FINISHED, CampaignStatus.SCHEDULED, CampaignStatus.ACTIVE, CampaignStatus.PAUSED);

        private final CampaignStatus target;
        private final Set<CampaignStatus> allowedFrom;

        Transition(CampaignStatus target, CampaignStatus first, CampaignStatus... others) {
            this.target = target;
            this.allowedFrom = EnumSet.of(first, others);
        }

        public CampaignStatus target() {
            return target;
        }
    }

    /** Returns the status after the transition, or throws when it is not allowed from {@code current}. */
    public static CampaignStatus requireAllowed(CampaignStatus current, Transition transition) {
        if (current == CampaignStatus.FINISHED) {
            throw new CampaignViolationException("status", ViolationCodes.STATUS_FINISHED,
                    "a finished campaign accepts no change");
        }
        if (!transition.allowedFrom.contains(current)) {
            throw new CampaignViolationException("status", ViolationCodes.STATUS_TRANSITION_NOT_ALLOWED,
                    "cannot " + transition.name().toLowerCase() + " a campaign that is " + current.name().toLowerCase());
        }
        return transition.target;
    }

    private CampaignStatusPolicy() {
    }
}
