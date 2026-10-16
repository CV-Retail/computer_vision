package com.cvretail.kiosko.campaigns.domain.repository;

import com.cvretail.kiosko.campaigns.domain.entity.Campaign;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignId;
import com.cvretail.kiosko.campaigns.domain.valueobject.StoreId;
import java.util.List;
import java.util.Optional;

/** Port to store campaigns. Every operation takes the store, so a query can never cross stores. */
public interface CampaignRepository {

    /** Inserts or updates the campaign. */
    void save(Campaign campaign);

    Optional<Campaign> findById(StoreId storeId, CampaignId id);

    List<Campaign> findAllByStore(StoreId storeId);

    Optional<Campaign> findDefault(StoreId storeId);

    /** Returns true if the campaign existed. */
    boolean deleteById(StoreId storeId, CampaignId id);
}
