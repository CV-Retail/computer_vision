package com.cvretail.kiosko.campaigns.bdd;

import static org.assertj.core.api.Assertions.assertThat;

import com.cvretail.kiosko.campaigns.domain.valueobject.KioskId;
import com.cvretail.kiosko.campaigns.domain.valueobject.Schedule;
import com.cvretail.kiosko.campaigns.domain.valueobject.Scope;
import com.cvretail.kiosko.campaigns.domain.valueobject.TimeSlot;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ScheduleAndScopeSteps {

    private final CampaignWorld world;

    public ScheduleAndScopeSteps(CampaignWorld world) {
        this.world = world;
    }

    @When("the administrator schedules the campaign from {string} to {string}")
    public void setsDates(String start, String end) {
        world.attempt(() -> world.campaign.reschedule(world.campaign.runSchedule()
                .withDates(Optional.of(LocalDate.parse(start)), Optional.of(LocalDate.parse(end)))));
    }

    @When("the administrator runs the campaign only on {string}")
    public void setsDays(String days) {
        world.attempt(() -> world.campaign.reschedule(world.campaign.runSchedule().withDays(days(days))));
    }

    @When("the administrator runs the campaign between {string} and {string}")
    public void setsSlot(String start, String end) {
        world.attempt(() -> world.campaign.reschedule(world.campaign.runSchedule()
                .withSlot(Optional.of(new TimeSlot(LocalTime.parse(start), LocalTime.parse(end))))));
    }

    @When("the administrator restricts the campaign to the kiosks {string}")
    public void restrictsToKiosks(String kiosks) {
        world.attempt(() -> world.campaign.changeScope(Scope.SelectedKiosks.of(kiosks(kiosks))));
    }

    @When("the administrator makes the campaign run on all kiosks")
    public void runsOnAllKiosks() {
        world.attempt(() -> world.campaign.changeScope(Scope.all()));
    }

    @Then("the campaign runs from {string} to {string}")
    public void runsFromTo(String start, String end) {
        Schedule schedule = world.campaign.runSchedule();
        assertThat(schedule.startDate()).contains(LocalDate.parse(start));
        assertThat(schedule.endDate()).contains(LocalDate.parse(end));
    }

    @Then("the campaign runs only on {string}")
    public void runsOnlyOn(String days) {
        assertThat(world.campaign.runSchedule().days()).isEqualTo(days(days));
    }

    @Then("the campaign runs every day")
    public void runsEveryDay() {
        assertThat(world.campaign.runSchedule().days()).containsExactlyInAnyOrder(DayOfWeek.values());
    }

    @Then("the campaign runs between {string} and {string}")
    public void runsBetween(String start, String end) {
        assertThat(world.campaign.runSchedule().slot())
                .contains(new TimeSlot(LocalTime.parse(start), LocalTime.parse(end)));
    }

    @Then("the campaign runs only on the kiosks {string}")
    public void runsOnlyOnKiosks(String kiosks) {
        assertThat(world.campaign.scope()).isEqualTo(Scope.SelectedKiosks.of(kiosks(kiosks)));
    }

    @Then("the campaign runs on all kiosks")
    public void campaignRunsOnAllKiosks() {
        assertThat(world.campaign.scope()).isEqualTo(Scope.all());
    }

    private static Set<DayOfWeek> days(String text) {
        return split(text).stream().map(day -> DayOfWeek.valueOf(day.toUpperCase(Locale.ROOT)))
                .collect(Collectors.toSet());
    }

    private static List<KioskId> kiosks(String text) {
        return split(text).stream().map(KioskId::new).toList();
    }

    private static List<String> split(String text) {
        return text.isBlank() ? List.of() : Arrays.stream(text.split(",")).map(String::trim).toList();
    }
}
