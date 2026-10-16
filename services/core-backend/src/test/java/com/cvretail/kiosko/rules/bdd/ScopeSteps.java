package com.cvretail.kiosko.rules.bdd;

import io.cucumber.java.en.Given;
import java.util.Arrays;

/** Where the campaign runs, and which store it belongs to. */
public class ScopeSteps {

    private final RulesWorld world;

    public ScopeSteps(RulesWorld world) {
        this.world = world;
    }

    @Given("it runs only on the kiosks {string}")
    public void itRunsOnlyOnTheKiosks(String kiosks) {
        world.current().onKiosks(Arrays.stream(kiosks.split(",")).map(String::trim).toArray(String[]::new));
    }

    @Given("it belongs to the store {string}")
    public void itBelongsToTheStore(String store) {
        world.current().inStore(store);
    }
}
