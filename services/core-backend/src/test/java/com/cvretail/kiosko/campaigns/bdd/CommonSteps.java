package com.cvretail.kiosko.campaigns.bdd;

import static com.cvretail.kiosko.campaigns.support.CampaignBuilder.aRegularCampaign;
import static org.assertj.core.api.Assertions.assertThat;

import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignStatus;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.time.LocalDate;
import java.util.Locale;

/** Steps used by several features: the store, ready-made campaigns and the checks on a rejection. */
public class CommonSteps {

    static final LocalDate START = LocalDate.of(2030, 1, 1);

    private final CampaignWorld world;

    public CommonSteps(CampaignWorld world) {
        this.world = world;
    }

    @Given("a store {string}")
    public void aStore(String store) {
        world.store = store;
    }

    @Given("a regular draft campaign")
    public void aRegularDraftCampaign() {
        world.campaign = aRegularCampaign().inStore(world.store).build();
    }

    @Given("a complete regular campaign in status {string}")
    public void aCompleteRegularCampaignInStatus(String status) {
        world.campaign = aRegularCampaign().inStore(world.store).withPlaylistItems(1).withAudienceRule()
                .startingOn(START).inStatus(CampaignStatus.valueOf(status.toUpperCase(Locale.ROOT))).build();
    }

    @Given("an active regular campaign with one playlist item")
    public void anActiveRegularCampaignWithOnePlaylistItem() {
        world.campaign = aRegularCampaign().inStore(world.store).withPlaylistItems(1)
                .inStatus(CampaignStatus.ACTIVE).build();
    }

    @Given("a regular campaign with no playlist and no audience rule")
    public void aRegularCampaignWithNothingYet() {
        aRegularDraftCampaign();
    }

    @Then("the change is rejected because {}")
    public void theChangeIsRejectedBecause(String reason) {
        assertThat(world.rejection).as("the change should have been rejected").isNotNull();
        assertThat(world.rejection.hasCode(RejectionReasons.codeOf(reason)))
                .as("rejection codes were %s", world.rejection.violations()).isTrue();
    }

    @Then("the change is accepted")
    public void theChangeIsAccepted() {
        assertThat(world.rejection).as("the change should have been accepted").isNull();
    }

    @Then("the campaign is unchanged")
    public void theCampaignIsUnchanged() {
        assertThat(world.campaign.snapshot()).isEqualTo(world.before);
    }

    @Then("the campaign status is {string}")
    public void theCampaignStatusIs(String status) {
        assertThat(world.campaign.status()).isEqualTo(CampaignStatus.valueOf(status.toUpperCase(Locale.ROOT)));
    }
}
