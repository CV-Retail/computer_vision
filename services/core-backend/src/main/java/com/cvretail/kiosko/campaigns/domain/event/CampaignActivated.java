package com.cvretail.kiosko.campaigns.domain.event;

import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignId;
import com.cvretail.kiosko.campaigns.domain.valueobject.StoreId;

/** A campaign moved to the matching status. */
public record CampaignActivated(CampaignId campaignId, StoreId storeId) implements CampaignEvent {
}
