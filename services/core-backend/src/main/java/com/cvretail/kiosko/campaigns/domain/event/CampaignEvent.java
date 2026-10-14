package com.cvretail.kiosko.campaigns.domain.event;

import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignId;
import com.cvretail.kiosko.campaigns.domain.valueobject.StoreId;

/** Something that happened to a campaign. Carries no time: the domain does not read the clock. */
public sealed interface CampaignEvent
        permits CampaignCreated, CampaignScheduled, CampaignActivated, CampaignPaused, CampaignFinished {

    CampaignId campaignId();

    StoreId storeId();
}
