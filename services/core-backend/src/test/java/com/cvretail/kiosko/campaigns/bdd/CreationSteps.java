package com.cvretail.kiosko.campaigns.bdd;

import static org.assertj.core.api.Assertions.assertThat;

import com.cvretail.kiosko.campaigns.domain.entity.Campaign;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignId;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignName;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignType;
import com.cvretail.kiosko.campaigns.domain.valueobject.Priority;
import com.cvretail.kiosko.campaigns.domain.valueobject.Schedule;
import com.cvretail.kiosko.campaigns.domain.valueobject.Scope;
import com.cvretail.kiosko.campaigns.domain.valueobject.StoreId;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Locale;

public class CreationSteps {

    private final CampaignWorld world;

    public CreationSteps(CampaignWorld world) {
        this.world = world;
    }

    @When("the administrator creates a regular campaign named {string}")
    public void createsARegularCampaign(String name) {
        world.attempt(() -> world.campaign = Campaign.createRegular(
                CampaignId.generate(), new StoreId(world.store), new CampaignName(name)));
    }

    @When("the administrator creates the default campaign named {string}")
    public void createsTheDefaultCampaign(String name) {
        world.attempt(() -> world.campaign = Campaign.createDefault(
                CampaignId.generate(), new StoreId(world.store), new CampaignName(name)));
    }

    @Then("the campaign belongs to the store {string}")
    public void belongsToStore(String store) {
        assertThat(world.campaign.storeId().value()).isEqualTo(store);
    }

    @Then("its name is {string}")
    public void itsNameIs(String name) {
        assertThat(world.campaign.name().value()).isEqualTo(name);
    }

    @Then("its priority is {string}")
    public void itsPriorityIs(String priority) {
        assertThat(world.campaign.priority()).contains(Priority.valueOf(priority.toUpperCase(Locale.ROOT)));
    }

    @Then("it has no priority")
    public void hasNoPriority() {
        assertThat(world.campaign.priority()).isEmpty();
    }

    @Then("its minimum time on screen is {int} seconds")
    public void itsMinimumTimeIs(int seconds) {
        assertThat(world.campaign.minimumOnScreenTime().orElseThrow().seconds()).isEqualTo(seconds);
    }

    @Then("it has no minimum time on screen")
    public void hasNoMinimumTime() {
        assertThat(world.campaign.minimumOnScreenTime()).isEmpty();
    }

    @Then("it has no audience rule")
    public void hasNoAudienceRule() {
        assertThat(world.campaign.audienceRuleId()).isEmpty();
    }

    @Then("it has no manual vetoes")
    public void hasNoVetoes() {
        assertThat(world.campaign.manualVetoes()).isEmpty();
    }

    @Then("it runs on all kiosks")
    public void runsOnAllKiosks() {
        assertThat(world.campaign.scope()).isEqualTo(Scope.all());
    }

    @Then("it runs every day without dates")
    public void runsAlways() {
        assertThat(world.campaign.runSchedule()).isEqualTo(Schedule.always());
    }

    @Then("its playlist is empty")
    public void playlistIsEmpty() {
        assertThat(world.campaign.playlist().isEmpty()).isTrue();
    }

    @Then("it is a {string} campaign")
    public void itIsOfType(String type) {
        assertThat(world.campaign.type()).isEqualTo(CampaignType.valueOf(type.toUpperCase(Locale.ROOT)));
    }
}
