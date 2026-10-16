package com.cvretail.kiosko.rules.bdd;

import com.cvretail.kiosko.campaigns.CampaignView;
import com.cvretail.kiosko.rules.application.FindCandidateCampaigns;
import com.cvretail.kiosko.rules.domain.valueobject.Availability;
import com.cvretail.kiosko.rules.support.CampaignViewBuilder;
import com.cvretail.kiosko.rules.support.InMemoryCampaignCatalog;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** State of one scenario, shared between the step classes by PicoContainer. No static fields. */
public class RulesWorld {

    String store = "store-1";
    String kiosk = "kiosk-a";
    private ZoneId zone = ZoneId.of("UTC");
    private Instant instant = Instant.parse("2030-01-14T12:00:00Z");

    private final Map<String, CampaignViewBuilder> builders = new LinkedHashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    private String current;

    void zone(String zoneId) {
        this.zone = ZoneId.of(zoneId);
    }

    void instant(String isoInstant) {
        this.instant = Instant.parse(isoInstant);
    }

    /** Local date and time in the zone set so far, e.g. {@code 2030-01-14 12:00}. */
    void localTime(String dateAndTime) {
        this.instant = LocalDateTime.parse(dateAndTime.replace(' ', 'T')).atZone(zone).toInstant();
    }

    CampaignViewBuilder newCampaign(String name, CampaignView.Status status) {
        CampaignViewBuilder builder = CampaignViewBuilder.aCampaign()
                .withId(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)))
                .inStore(store)
                .withStatus(status);
        builders.put(name, builder);
        names.put(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name);
        current = name;
        return builder;
    }

    CampaignViewBuilder current() {
        return builders.get(current);
    }

    private FindCandidateCampaigns useCase() {
        InMemoryCampaignCatalog catalog = new InMemoryCampaignCatalog();
        builders.values().forEach(builder -> catalog.add(builder.build()));
        return new FindCandidateCampaigns(catalog, Clock.fixed(instant, zone));
    }

    /** The names of the candidates for the kiosk, in the order returned. */
    List<String> candidates() {
        return useCase().candidatesFor(store, kiosk).stream().map(view -> names.get(view.id())).toList();
    }

    Availability availabilityOfCurrent() {
        return useCase().availabilityOf(current().build(), kiosk);
    }
}
