package com.cvretail.kiosko.campaigns;

import java.util.List;
import java.util.Optional;

/** Read-only queries on campaigns for other modules. The adapter that implements it comes with persistence. */
public interface CampaignCatalog {

    List<CampaignView> findByStatus(String storeId, CampaignView.Status status);

    Optional<CampaignView> findDefault(String storeId);
}
