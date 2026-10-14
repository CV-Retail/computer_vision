package com.cvretail.kiosko.campaigns.application;

import com.cvretail.kiosko.campaigns.CampaignView;
import com.cvretail.kiosko.campaigns.domain.entity.Campaign;
import com.cvretail.kiosko.campaigns.domain.valueobject.AudienceRuleId;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignSnapshot;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignStatus;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignType;
import com.cvretail.kiosko.campaigns.domain.valueobject.KioskId;
import com.cvretail.kiosko.campaigns.domain.valueobject.MinimumOnScreenTime;
import com.cvretail.kiosko.campaigns.domain.valueobject.Priority;
import com.cvretail.kiosko.campaigns.domain.valueobject.Schedule;
import com.cvretail.kiosko.campaigns.domain.valueobject.Scope;
import com.cvretail.kiosko.campaigns.domain.valueobject.VetoId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Builds the published {@link CampaignView} from a campaign. */
public final class CampaignViews {

    public static CampaignView from(Campaign campaign) {
        CampaignSnapshot s = campaign.snapshot();
        return new CampaignView(
                s.id().value(),
                s.storeId().value(),
                type(s.type()),
                status(s.status()),
                s.priority().map(CampaignViews::priority),
                schedule(s.schedule()),
                scope(s.scope()),
                s.minimumOnScreenTime().map(MinimumOnScreenTime::seconds),
                s.audienceRuleId().map(AudienceRuleId::value),
                s.manualVetoes().stream().map(VetoId::value).collect(Collectors.toUnmodifiableSet()),
                s.playlist().items().stream()
                        .map(item -> new CampaignView.PlaylistItem(item.media().value(), item.durationSeconds()))
                        .toList());
    }

    private static CampaignView.Type type(CampaignType type) {
        return switch (type) {
            case REGULAR -> CampaignView.Type.REGULAR;
            case DEFAULT -> CampaignView.Type.DEFAULT;
        };
    }

    private static CampaignView.Status status(CampaignStatus status) {
        return switch (status) {
            case DRAFT -> CampaignView.Status.DRAFT;
            case SCHEDULED -> CampaignView.Status.SCHEDULED;
            case ACTIVE -> CampaignView.Status.ACTIVE;
            case PAUSED -> CampaignView.Status.PAUSED;
            case FINISHED -> CampaignView.Status.FINISHED;
        };
    }

    private static CampaignView.Priority priority(Priority priority) {
        return switch (priority) {
            case HIGH -> CampaignView.Priority.HIGH;
            case MEDIUM -> CampaignView.Priority.MEDIUM;
            case LOW -> CampaignView.Priority.LOW;
        };
    }

    private static CampaignView.Schedule schedule(Schedule schedule) {
        return new CampaignView.Schedule(
                schedule.startDate(),
                schedule.endDate(),
                schedule.days(),
                schedule.slot().map(slot -> new CampaignView.TimeSlot(slot.start(), slot.end())));
    }

    private static CampaignView.Scope scope(Scope scope) {
        return switch (scope) {
            case Scope.AllKiosks all -> new CampaignView.Scope(true, Set.of());
            case Scope.SelectedKiosks selected -> new CampaignView.Scope(false,
                    selected.kiosks().stream().map(KioskId::value).collect(Collectors.toUnmodifiableSet()));
        };
    }

    private CampaignViews() {
    }
}
