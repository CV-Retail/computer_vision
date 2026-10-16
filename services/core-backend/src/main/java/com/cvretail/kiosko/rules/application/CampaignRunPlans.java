package com.cvretail.kiosko.rules.application;

import com.cvretail.kiosko.campaigns.CampaignView;
import com.cvretail.kiosko.rules.domain.valueobject.CampaignLifecycle;
import com.cvretail.kiosko.rules.domain.valueobject.CampaignRunPlan;
import com.cvretail.kiosko.rules.domain.valueobject.DailySlot;
import com.cvretail.kiosko.rules.domain.valueobject.KioskId;
import com.cvretail.kiosko.rules.domain.valueobject.KioskSelection;
import com.cvretail.kiosko.rules.domain.valueobject.ScheduleWindow;
import java.util.stream.Collectors;

/** Translates the published {@link CampaignView} into the types of the rules domain. */
final class CampaignRunPlans {

    static CampaignRunPlan from(CampaignView view) {
        return new CampaignRunPlan(view.id(), lifecycle(view.status()), window(view.schedule()), kiosks(view.scope()));
    }

    private static CampaignLifecycle lifecycle(CampaignView.Status status) {
        return switch (status) {
            case DRAFT -> CampaignLifecycle.DRAFT;
            case SCHEDULED -> CampaignLifecycle.SCHEDULED;
            case ACTIVE -> CampaignLifecycle.ACTIVE;
            case PAUSED -> CampaignLifecycle.PAUSED;
            case FINISHED -> CampaignLifecycle.FINISHED;
        };
    }

    private static ScheduleWindow window(CampaignView.Schedule schedule) {
        return new ScheduleWindow(
                schedule.startDate(),
                schedule.endDate(),
                schedule.days(),
                schedule.slot().map(slot -> new DailySlot(slot.start(), slot.end())));
    }

    private static KioskSelection kiosks(CampaignView.Scope scope) {
        if (scope.allKiosks()) {
            return new KioskSelection.AllKiosks();
        }
        return new KioskSelection.SelectedKiosks(
                scope.kioskIds().stream().map(KioskId::new).collect(Collectors.toSet()));
    }

    private CampaignRunPlans() {
    }
}
