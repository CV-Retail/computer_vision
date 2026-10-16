package com.cvretail.kiosko.campaigns.bdd;

import static com.cvretail.kiosko.campaigns.support.CampaignBuilder.aDefaultCampaign;
import static org.assertj.core.api.Assertions.assertThat;

import com.cvretail.kiosko.campaigns.domain.valueobject.AudienceRuleId;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignName;
import com.cvretail.kiosko.campaigns.domain.valueobject.MinimumOnScreenTime;
import com.cvretail.kiosko.campaigns.domain.valueobject.Priority;
import com.cvretail.kiosko.campaigns.domain.valueobject.Schedule;
import com.cvretail.kiosko.campaigns.domain.valueobject.VetoId;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.DayOfWeek;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Editing of the fields of a campaign, for regular and default campaigns. */
public class EditingSteps {

    private final CampaignWorld world;
    private VetoId veto;

    public EditingSteps(CampaignWorld world) {
        this.world = world;
    }

    @Given("a default campaign with {int} playlist item(s)")
    public void aDefaultCampaignWithItems(int items) {
        world.campaign = aDefaultCampaign().inStore(world.store).withPlaylistItems(items).build();
    }

    @Given("a veto is on the campaign")
    public void aVetoIsOnTheCampaign() {
        veto = new VetoId(UUID.randomUUID());
        world.campaign.addManualVeto(veto);
    }

    @When("the administrator renames the campaign to {string}")
    public void renames(String name) {
        world.attempt(() -> world.campaign.rename(new CampaignName(name)));
    }

    @When("the administrator sets the priority to {string}")
    public void setsPriority(String priority) {
        world.attempt(() -> world.campaign.changePriority(Priority.valueOf(priority.toUpperCase(Locale.ROOT))));
    }

    @When("the administrator assigns an audience rule")
    public void assignsAudienceRule() {
        world.attempt(() -> world.campaign.assignAudienceRule(new AudienceRuleId(UUID.randomUUID())));
    }

    @When("the administrator removes the audience rule")
    public void removesAudienceRule() {
        world.attempt(world.campaign::clearAudienceRule);
    }

    @When("the administrator sets the minimum time on screen to {int} seconds")
    public void setsMinimumTime(int seconds) {
        world.attempt(() -> world.campaign.changeMinimumOnScreenTime(new MinimumOnScreenTime(seconds)));
    }

    @When("the administrator adds a veto")
    public void addsAVeto() {
        veto = new VetoId(UUID.randomUUID());
        world.attempt(() -> world.campaign.addManualVeto(veto));
    }

    @When("the administrator adds the same veto again")
    public void addsTheSameVetoAgain() {
        world.attempt(() -> world.campaign.addManualVeto(veto));
    }

    @When("the administrator removes the veto")
    public void removesTheVeto() {
        world.attempt(() -> world.campaign.removeManualVeto(veto));
    }

    @When("the administrator removes a veto that is not on the campaign")
    public void removesAnUnknownVeto() {
        world.attempt(() -> world.campaign.removeManualVeto(new VetoId(UUID.randomUUID())));
    }

    @When("the administrator tries to give the default campaign {}")
    public void triesToGiveTheDefaultCampaign(String regularOnlyField) {
        world.attempt(() -> {
            switch (regularOnlyField) {
                case "a priority" -> world.campaign.changePriority(Priority.HIGH);
                case "an audience rule" -> world.campaign.assignAudienceRule(new AudienceRuleId(UUID.randomUUID()));
                case "a minimum time" -> world.campaign.changeMinimumOnScreenTime(MinimumOnScreenTime.defaultValue());
                case "a manual veto" -> world.campaign.addManualVeto(new VetoId(UUID.randomUUID()));
                case "a custom schedule" -> world.campaign.reschedule(
                        Schedule.always().withDays(Set.of(DayOfWeek.MONDAY)).withDates(Optional.empty(), Optional.empty()));
                default -> throw new IllegalArgumentException("Unknown field in the scenario: " + regularOnlyField);
            }
        });
    }

    @Then("the campaign has an audience rule")
    public void hasAnAudienceRule() {
        assertThat(world.campaign.audienceRuleId()).isPresent();
    }

    @Then("the campaign has {int} manual veto(es)")
    public void hasVetoes(int count) {
        assertThat(world.campaign.manualVetoes()).hasSize(count);
    }
}
