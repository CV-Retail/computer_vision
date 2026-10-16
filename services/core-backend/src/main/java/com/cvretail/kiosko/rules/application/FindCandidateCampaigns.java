package com.cvretail.kiosko.rules.application;

import com.cvretail.kiosko.campaigns.CampaignCatalog;
import com.cvretail.kiosko.campaigns.CampaignView;
import com.cvretail.kiosko.rules.domain.service.AvailabilityEvaluator;
import com.cvretail.kiosko.rules.domain.valueobject.Availability;
import com.cvretail.kiosko.rules.domain.valueobject.KioskId;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Step 1 of the rules engine: the campaigns of a store whose calendar and scope allow them to run on a kiosk
 * now. The moment and the time zone come only from the injected clock.
 */
public final class FindCandidateCampaigns {

    private final CampaignCatalog catalog;
    private final Clock clock;

    public FindCandidateCampaigns(CampaignCatalog catalog, Clock clock) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * The candidates for the kiosk: first the active campaigns and then the scheduled ones, each group in the
     * order of the catalog.
     */
    public List<CampaignView> candidatesFor(String storeId, String kioskId) {
        requireText(storeId, "storeId");
        KioskId kiosk = new KioskId(kioskId);
        LocalDateTime now = localNow();

        List<CampaignView> candidates = new ArrayList<>();
        for (CampaignView.Status status : List.of(CampaignView.Status.ACTIVE, CampaignView.Status.SCHEDULED)) {
            for (CampaignView view : catalog.findByStatus(storeId, status)) {
                if (view.storeId().equals(storeId) && evaluate(view, kiosk, now).applies()) {
                    candidates.add(view);
                }
            }
        }
        return List.copyOf(candidates);
    }

    /** Whether one campaign may run on the kiosk now, or the first reason why not. */
    public Availability availabilityOf(CampaignView campaign, String kioskId) {
        Objects.requireNonNull(campaign, "campaign");
        return evaluate(campaign, new KioskId(kioskId), localNow());
    }

    private static Availability evaluate(CampaignView view, KioskId kiosk, LocalDateTime now) {
        return AvailabilityEvaluator.evaluate(CampaignRunPlans.from(view), kiosk, now);
    }

    /** The local date and time of the clock's instant in the clock's zone; read once per call. */
    private LocalDateTime localNow() {
        return ZonedDateTime.now(clock).toLocalDateTime();
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
