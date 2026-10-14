package com.cvretail.kiosko.campaigns.domain.repository;

class InMemoryCampaignRepositoryTest extends CampaignRepositoryContractTest {

    private final CampaignRepository repository = new InMemoryCampaignRepository();

    @Override
    protected CampaignRepository repository() {
        return repository;
    }
}
