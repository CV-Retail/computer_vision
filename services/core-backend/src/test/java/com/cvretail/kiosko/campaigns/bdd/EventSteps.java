package com.cvretail.kiosko.campaigns.bdd;

import static org.assertj.core.api.Assertions.assertThat;

import com.cvretail.kiosko.campaigns.domain.entity.Campaign;
import com.cvretail.kiosko.campaigns.domain.event.CampaignActivated;
import com.cvretail.kiosko.campaigns.domain.event.CampaignCreated;
import com.cvretail.kiosko.campaigns.domain.event.CampaignEvent;
import com.cvretail.kiosko.campaigns.domain.event.CampaignFinished;
import com.cvretail.kiosko.campaigns.domain.event.CampaignPaused;
import com.cvretail.kiosko.campaigns.domain.event.CampaignScheduled;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Arrays;
import java.util.List;

public class EventSteps {

    private final CampaignWorld world;

    public EventSteps(CampaignWorld world) {
        this.world = world;
    }

    @Given("the events recorded so far are handed over")
    public void handedOverSoFar() {
        world.campaign.pullEvents();
    }

    @When("the recorded events are handed over")
    public void handedOver() {
        world.events.clear();
        world.events.addAll(world.campaign.pullEvents());
    }

    @When("the campaign is rebuilt from its stored state")
    public void rebuilt() {
        world.campaign = Campaign.restore(world.campaign.snapshot());
    }

    @Then("the recorded events are {string}")
    public void recordedAre(String names) {
        List<String> expected = Arrays.stream(names.split(",")).map(String::trim).toList();
        assertThat(world.events.stream().map(EventSteps::name).toList()).isEqualTo(expected);
        world.events.forEach(event -> {
            assertThat(event.campaignId()).isEqualTo(world.campaign.id());
            assertThat(event.storeId()).isEqualTo(world.campaign.storeId());
        });
    }

    @Then("no events are recorded")
    public void noEvents() {
        assertThat(world.campaign.pullEvents()).isEmpty();
    }

    private static String name(CampaignEvent event) {
        return switch (event) {
            case CampaignCreated e -> "created";
            case CampaignScheduled e -> "scheduled";
            case CampaignActivated e -> "activated";
            case CampaignPaused e -> "paused";
            case CampaignFinished e -> "finished";
        };
    }
}
