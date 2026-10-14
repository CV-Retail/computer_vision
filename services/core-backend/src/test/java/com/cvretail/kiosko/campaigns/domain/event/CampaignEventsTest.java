package com.cvretail.kiosko.campaigns.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignId;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignType;
import com.cvretail.kiosko.campaigns.domain.valueobject.StoreId;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CampaignEventsTest {

    @Test
    void theHierarchyHasExactlyTheFiveEvents() {
        Set<Class<?>> permitted = Arrays.stream(CampaignEvent.class.getPermittedSubclasses())
                .collect(Collectors.toSet());
        assertThat(permitted).containsExactlyInAnyOrder(CampaignCreated.class, CampaignScheduled.class,
                CampaignActivated.class, CampaignPaused.class, CampaignFinished.class);
    }

    @Test
    void eventsCarryTheCampaignAndTheStore() {
        CampaignId id = CampaignId.generate();
        StoreId store = new StoreId("s1");
        CampaignEvent created = new CampaignCreated(id, store, CampaignType.DEFAULT);
        assertThat(created.campaignId()).isEqualTo(id);
        assertThat(created.storeId()).isEqualTo(store);
        assertThat(((CampaignCreated) created).type()).isEqualTo(CampaignType.DEFAULT);
        assertThat(new CampaignPaused(id, store)).isEqualTo(new CampaignPaused(id, store));
    }
}
