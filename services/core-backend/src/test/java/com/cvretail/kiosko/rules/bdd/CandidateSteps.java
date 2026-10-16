package com.cvretail.kiosko.rules.bdd;

import static org.assertj.core.api.Assertions.assertThat;

import com.cvretail.kiosko.rules.domain.valueobject.Availability;
import io.cucumber.java.en.Then;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** What the engine answers. */
public class CandidateSteps {

    private static final Map<String, Availability> REASONS = Map.of(
            "the campaign is not released", Availability.STATUS_NOT_ELIGIBLE,
            "the start date has not arrived", Availability.BEFORE_START_DATE,
            "the end date has passed", Availability.AFTER_END_DATE,
            "the day is not included", Availability.DAY_NOT_INCLUDED,
            "it is outside the time slot", Availability.OUTSIDE_TIME_SLOT,
            "the kiosk is not included", Availability.KIOSK_NOT_INCLUDED);

    private final RulesWorld world;

    public CandidateSteps(RulesWorld world) {
        this.world = world;
    }

    @Then("the campaign is a candidate for the kiosk")
    public void isACandidate() {
        assertThat(world.availabilityOfCurrent()).isEqualTo(Availability.APPLIES);
        assertThat(world.candidates()).isNotEmpty();
    }

    @Then("the campaign is not a candidate for the kiosk")
    public void isNotACandidate() {
        assertThat(world.availabilityOfCurrent().applies()).isFalse();
    }

    @Then("the reason is {string}")
    public void theReasonIs(String reason) {
        Availability expected = REASONS.get(reason);
        assertThat(expected).as("known reason in the scenario: %s", reason).isNotNull();
        assertThat(world.availabilityOfCurrent()).isEqualTo(expected);
    }

    @Then("the result is {string}")
    public void theResultIs(String result) {
        Availability expected = result.equals("a candidate") ? Availability.APPLIES : REASONS.get(result);
        assertThat(expected).as("known result in the scenario: %s", result).isNotNull();
        assertThat(world.availabilityOfCurrent()).isEqualTo(expected);
    }

    @Then("the candidates are {string}")
    public void theCandidatesAre(String names) {
        List<String> expected = Arrays.stream(names.split(",")).map(String::trim).toList();
        assertThat(world.candidates()).isEqualTo(expected);
    }

    @Then("there are no candidates")
    public void thereAreNoCandidates() {
        assertThat(world.candidates()).isEmpty();
    }
}
