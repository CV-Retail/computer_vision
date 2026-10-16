package com.cvretail.kiosko.rules.support;

import com.cvretail.kiosko.campaigns.CampaignCatalog;
import com.cvretail.kiosko.campaigns.CampaignView;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Fake of the published query port: returns the views it holds, by store and status, in insertion order. */
public final class InMemoryCampaignCatalog implements CampaignCatalog {

    private final List<CampaignView> views = new ArrayList<>();
    private final List<String> queries = new ArrayList<>();

    public InMemoryCampaignCatalog add(CampaignView view) {
        views.add(view);
        return this;
    }

    /** The queries received, as "storeId:STATUS", in order. */
    public List<String> queries() {
        return List.copyOf(queries);
    }

    @Override
    public List<CampaignView> findByStatus(String storeId, CampaignView.Status status) {
        queries.add(storeId + ":" + status);
        return views.stream()
                .filter(view -> view.storeId().equals(storeId) && view.status() == status)
                .toList();
    }

    @Override
    public Optional<CampaignView> findDefault(String storeId) {
        return views.stream()
                .filter(view -> view.storeId().equals(storeId) && view.type() == CampaignView.Type.DEFAULT)
                .findFirst();
    }
}
