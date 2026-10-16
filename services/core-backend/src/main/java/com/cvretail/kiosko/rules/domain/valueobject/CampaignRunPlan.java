package com.cvretail.kiosko.rules.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/** What the schedule and scope rules need to know about a campaign. */
public record CampaignRunPlan(UUID id, CampaignLifecycle lifecycle, ScheduleWindow window, KioskSelection kiosks) {

    public CampaignRunPlan {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(lifecycle, "lifecycle");
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(kiosks, "kiosks");
    }
}
