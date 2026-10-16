package com.cvretail.kiosko.rules.bdd;

import io.cucumber.java.en.Given;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Locale;

/** What the campaign says about dates, days and the time slot. */
public class ScheduleSteps {

    private final RulesWorld world;

    public ScheduleSteps(RulesWorld world) {
        this.world = world;
    }

    @Given("it starts on {string}")
    public void itStartsOn(String date) {
        world.current().startingOn(date);
    }

    @Given("it ends on {string}")
    public void itEndsOn(String date) {
        world.current().endingOn(date);
    }

    @Given("it runs only on {string}")
    public void itRunsOnlyOn(String days) {
        world.current().onDays(Arrays.stream(days.split(","))
                .map(day -> DayOfWeek.valueOf(day.trim().toUpperCase(Locale.ROOT)))
                .toArray(DayOfWeek[]::new));
    }

    @Given("it runs between {string} and {string}")
    public void itRunsBetween(String start, String end) {
        world.current().between(start, end);
    }
}
