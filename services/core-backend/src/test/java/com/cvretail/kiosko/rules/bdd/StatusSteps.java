package com.cvretail.kiosko.rules.bdd;

import com.cvretail.kiosko.campaigns.CampaignView;
import io.cucumber.java.en.Given;
import java.util.Locale;

/** Setting the scene: store, kiosk, server time and zone, and campaigns in a status. */
public class StatusSteps {

    private final RulesWorld world;

    public StatusSteps(RulesWorld world) {
        this.world = world;
    }

    @Given("a store {string} and a kiosk {string}")
    public void aStoreAndAKiosk(String store, String kiosk) {
        world.store = store;
        world.kiosk = kiosk;
    }

    @Given("the server time zone is {string}")
    public void theServerTimeZoneIs(String zone) {
        world.zone(zone);
    }

    @Given("the server time is {string}")
    public void theServerTimeIs(String localTime) {
        world.localTime(localTime);
    }

    @Given("the server instant is {string}")
    public void theServerInstantIs(String instant) {
        world.instant(instant);
    }

    @Given("a campaign in status {string}")
    public void aCampaignInStatus(String status) {
        world.newCampaign("campaign", status(status));
    }

    @Given("a campaign {string} in status {string}")
    public void aNamedCampaignInStatus(String name, String status) {
        world.newCampaign(name, status(status));
    }

    private static CampaignView.Status status(String text) {
        return CampaignView.Status.valueOf(text.toUpperCase(Locale.ROOT));
    }
}
