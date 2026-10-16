package com.cvretail.kiosko.rules.domain.valueobject;

/** Life cycle status of a campaign, as the rules see it. */
public enum CampaignLifecycle {
    DRAFT,
    SCHEDULED,
    ACTIVE,
    PAUSED,
    FINISHED;

    /** Only scheduled and active campaigns may be candidates; the schedule decides the rest. */
    public boolean isEligible() {
        return this == SCHEDULED || this == ACTIVE;
    }
}
