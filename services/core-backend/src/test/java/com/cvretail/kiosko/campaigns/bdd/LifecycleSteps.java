package com.cvretail.kiosko.campaigns.bdd;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class LifecycleSteps {

    private final CampaignWorld world;

    public LifecycleSteps(CampaignWorld world) {
        this.world = world;
    }

    @When("the administrator tries to {word} the campaign")
    public void triesTo(String transition) {
        world.attempt(switch (transition) {
            case "schedule" -> world.campaign::schedule;
            case "activate" -> world.campaign::activate;
            case "pause" -> world.campaign::pause;
            case "finish" -> world.campaign::finish;
            default -> throw new IllegalArgumentException("Unknown transition in the scenario: " + transition);
        });
    }

    @When("the administrator resumes the campaign")
    public void resumes() {
        world.attempt(world.campaign::activate);
    }

    @Then("the outcome is {string}")
    public void theOutcomeIs(String outcome) {
        assertThat(world.outcome()).isEqualTo(outcome);
    }
}
