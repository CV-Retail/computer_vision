package com.cvretail.kiosko.campaigns.domain.repository;

import static com.cvretail.kiosko.campaigns.support.CampaignBuilder.aDefaultCampaign;
import static com.cvretail.kiosko.campaigns.support.CampaignBuilder.aRegularCampaign;
import static org.assertj.core.api.Assertions.assertThat;

import com.cvretail.kiosko.campaigns.domain.entity.Campaign;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignId;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignName;
import com.cvretail.kiosko.campaigns.domain.valueobject.StoreId;
import org.junit.jupiter.api.Test;

/** Contract every implementation of {@link CampaignRepository} must meet. Extend it to test an adapter. */
public abstract class CampaignRepositoryContractTest {

    protected abstract CampaignRepository repository();

    private final StoreId store1 = new StoreId("store-1");
    private final StoreId store2 = new StoreId("store-2");

    @Test
    void savedCampaignIsFoundByStoreAndId() {
        Campaign campaign = aRegularCampaign().inStore("store-1").build();
        repository().save(campaign);
        assertThat(repository().findById(store1, campaign.id())).hasValueSatisfying(
                found -> assertThat(found.snapshot()).isEqualTo(campaign.snapshot()));
    }

    @Test
    void savingAgainUpdatesInsteadOfDuplicating() {
        Campaign campaign = aRegularCampaign().inStore("store-1").build();
        repository().save(campaign);
        campaign.rename(new CampaignName("Renamed"));
        repository().save(campaign);
        assertThat(repository().findAllByStore(store1)).hasSize(1);
        assertThat(repository().findById(store1, campaign.id()).orElseThrow().name().value()).isEqualTo("Renamed");
    }

    @Test
    void aCampaignOfAnotherStoreIsNeverFound() {
        Campaign campaign = aRegularCampaign().inStore("store-1").build();
        repository().save(campaign);
        assertThat(repository().findById(store2, campaign.id())).isEmpty();
        assertThat(repository().findAllByStore(store2)).isEmpty();
        assertThat(repository().deleteById(store2, campaign.id())).isFalse();
        assertThat(repository().findById(store1, campaign.id())).isPresent();
    }

    @Test
    void unknownIdIsEmpty() {
        assertThat(repository().findById(store1, CampaignId.generate())).isEmpty();
    }

    @Test
    void findAllByStoreListsOnlyThatStore() {
        repository().save(aRegularCampaign().inStore("store-1").build());
        repository().save(aRegularCampaign().inStore("store-1").build());
        repository().save(aRegularCampaign().inStore("store-2").build());
        assertThat(repository().findAllByStore(store1)).hasSize(2);
        assertThat(repository().findAllByStore(store2)).hasSize(1);
    }

    @Test
    void findDefaultReturnsTheDefaultCampaignOfTheStore() {
        Campaign regular = aRegularCampaign().inStore("store-1").build();
        Campaign fallback = aDefaultCampaign().inStore("store-1").build();
        repository().save(regular);
        assertThat(repository().findDefault(store1)).isEmpty();
        repository().save(fallback);
        assertThat(repository().findDefault(store1)).hasValueSatisfying(c -> assertThat(c.id()).isEqualTo(fallback.id()));
        assertThat(repository().findDefault(store2)).isEmpty();
    }

    @Test
    void deleteReportsWhetherTheCampaignExisted() {
        Campaign campaign = aRegularCampaign().inStore("store-1").build();
        repository().save(campaign);
        assertThat(repository().deleteById(store1, campaign.id())).isTrue();
        assertThat(repository().deleteById(store1, campaign.id())).isFalse();
        assertThat(repository().findById(store1, campaign.id())).isEmpty();
    }
}
