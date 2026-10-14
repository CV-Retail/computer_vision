package com.cvretail.kiosko.campaigns.domain.repository;

import com.cvretail.kiosko.campaigns.domain.entity.Campaign;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignId;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignType;
import com.cvretail.kiosko.campaigns.domain.valueobject.StoreId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory implementation of the port, for tests. Stores snapshots, so callers never share an instance. */
public final class InMemoryCampaignRepository implements CampaignRepository {

    private record Key(StoreId storeId, CampaignId id) {
    }

    private final Map<Key, Campaign> campaigns = new LinkedHashMap<>();

    @Override
    public void save(Campaign campaign) {
        campaigns.put(new Key(campaign.storeId(), campaign.id()), Campaign.restore(campaign.snapshot()));
    }

    @Override
    public Optional<Campaign> findById(StoreId storeId, CampaignId id) {
        return Optional.ofNullable(campaigns.get(new Key(storeId, id))).map(c -> Campaign.restore(c.snapshot()));
    }

    @Override
    public List<Campaign> findAllByStore(StoreId storeId) {
        return campaigns.values().stream()
                .filter(c -> c.storeId().equals(storeId))
                .map(c -> Campaign.restore(c.snapshot()))
                .toList();
    }

    @Override
    public Optional<Campaign> findDefault(StoreId storeId) {
        return findAllByStore(storeId).stream().filter(c -> c.type() == CampaignType.DEFAULT).findFirst();
    }

    @Override
    public boolean deleteById(StoreId storeId, CampaignId id) {
        return campaigns.remove(new Key(storeId, id)) != null;
    }
}
