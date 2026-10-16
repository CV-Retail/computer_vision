package com.cvretail.kiosko.campaigns.domain.event;

import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignId;
import com.cvretail.kiosko.campaigns.domain.valueobject.StoreId;

/** A campaign moved to the matching status. */
public record CampaignScheduled(CampaignId campaignId, StoreId storeId) implements CampaignEvent {
}
