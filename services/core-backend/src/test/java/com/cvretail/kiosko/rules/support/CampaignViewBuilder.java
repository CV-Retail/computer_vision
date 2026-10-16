package com.cvretail.kiosko.rules.support;

import com.cvretail.kiosko.campaigns.CampaignView;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Test data builder for the published campaign view: a test states only what it cares about. */
public final class CampaignViewBuilder {

    private UUID id = UUID.randomUUID();
    private String storeId = "store-1";
    private CampaignView.Type type = CampaignView.Type.REGULAR;
    private CampaignView.Status status = CampaignView.Status.ACTIVE;
    private Optional<LocalDate> startDate = Optional.empty();
    private Optional<LocalDate> endDate = Optional.empty();
    private Set<DayOfWeek> days = EnumSet.allOf(DayOfWeek.class);
    private Optional<CampaignView.TimeSlot> slot = Optional.empty();
    private CampaignView.Scope scope = new CampaignView.Scope(true, Set.of());

    private CampaignViewBuilder() {
    }

    public static CampaignViewBuilder aCampaign() {
        return new CampaignViewBuilder();
    }

    public CampaignViewBuilder withId(UUID value) {
        this.id = value;
        return this;
    }

    public CampaignViewBuilder inStore(String value) {
        this.storeId = value;
        return this;
    }

    public CampaignViewBuilder withStatus(CampaignView.Status value) {
        this.status = value;
        return this;
    }

    public CampaignViewBuilder ofType(CampaignView.Type value) {
        this.type = value;
        return this;
    }

    public CampaignViewBuilder startingOn(String date) {
        this.startDate = Optional.of(LocalDate.parse(date));
        return this;
    }

    public CampaignViewBuilder endingOn(String date) {
        this.endDate = Optional.of(LocalDate.parse(date));
        return this;
    }

    public CampaignViewBuilder onDays(DayOfWeek... values) {
        this.days = EnumSet.copyOf(List.of(values));
        return this;
    }

    public CampaignViewBuilder between(String start, String end) {
        this.slot = Optional.of(new CampaignView.TimeSlot(LocalTime.parse(start), LocalTime.parse(end)));
        return this;
    }

    public CampaignViewBuilder onKiosks(String... kioskIds) {
        this.scope = new CampaignView.Scope(false, Set.of(kioskIds));
        return this;
    }

    public CampaignView build() {
        boolean regular = type == CampaignView.Type.REGULAR;
        return new CampaignView(
                id,
                storeId,
                type,
                status,
                regular ? Optional.of(CampaignView.Priority.MEDIUM) : Optional.empty(),
                new CampaignView.Schedule(startDate, endDate, days, slot),
                scope,
                regular ? Optional.of(8) : Optional.empty(),
                regular ? Optional.of(UUID.randomUUID()) : Optional.empty(),
                Set.of(),
                List.of(new CampaignView.PlaylistItem("sha256:" + "a".repeat(64), 10)));
    }
}
