package com.cvretail.kiosko.campaigns.domain.event;

import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignId;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignType;
import com.cvretail.kiosko.campaigns.domain.valueobject.StoreId;

/** A campaign was created as a draft. */
public record CampaignCreated(CampaignId campaignId, StoreId storeId, CampaignType type) implements CampaignEvent {
}
