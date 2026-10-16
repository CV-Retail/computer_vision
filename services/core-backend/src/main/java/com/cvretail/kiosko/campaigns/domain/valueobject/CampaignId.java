package com.cvretail.kiosko.campaigns.domain.valueobject;

import java.util.UUID;

/** Identity of a campaign. */
public record CampaignId(UUID value) {

    public CampaignId {
        Require.notNull(value, "id");
    }

    public static CampaignId generate() {
        return new CampaignId(UUID.randomUUID());
    }
}
