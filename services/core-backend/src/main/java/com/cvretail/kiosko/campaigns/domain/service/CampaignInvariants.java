package com.cvretail.kiosko.campaigns.domain.service;

import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.Violation;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignSnapshot;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignStatus;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignType;
import com.cvretail.kiosko.campaigns.domain.valueobject.Schedule;
import java.util.ArrayList;
import java.util.List;

/** Rules that depend on the type and on the status of a campaign, checked over a whole snapshot. */
public final class CampaignInvariants {

    /** A finished campaign accepts no change. */
    public static void requireNotFinished(CampaignSnapshot state) {
        if (state.status() == CampaignStatus.FINISHED) {
            throw new CampaignViolationException("status", ViolationCodes.STATUS_FINISHED,
                    "a finished campaign accepts no change");
        }
    }

    /** Checks the type rules and, for a scheduled or active campaign, completeness. Reports every violation. */
    public static void validate(CampaignSnapshot state) {
        List<Violation> found = new ArrayList<>();
        if (state.type() == CampaignType.DEFAULT) {
            checkDefault(state, found);
        } else {
            checkRegular(state, found);
        }
        if (state.status() == CampaignStatus.SCHEDULED || state.status() == CampaignStatus.ACTIVE) {
            checkComplete(state, found);
        }
        if (!found.isEmpty()) {
            throw new CampaignViolationException(found);
        }
    }

    private static void checkRegular(CampaignSnapshot state, List<Violation> found) {
        if (state.priority().isEmpty()) {
            add(found, "priority", ViolationCodes.PRIORITY_REQUIRED, "a regular campaign needs a priority");
        }
        if (state.minimumOnScreenTime().isEmpty()) {
            add(found, "minimumOnScreenTime", ViolationCodes.MINIMUM_ON_SCREEN_REQUIRED,
                    "a regular campaign needs a minimum on-screen time");
        }
    }

    private static void checkDefault(CampaignSnapshot state, List<Violation> found) {
        if (state.priority().isPresent()) {
            add(found, "priority", ViolationCodes.PRIORITY_NOT_ALLOWED_FOR_DEFAULT,
                    "the default campaign has no priority");
        }
        if (state.audienceRuleId().isPresent()) {
            add(found, "audienceRuleId", ViolationCodes.AUDIENCE_RULE_NOT_ALLOWED_FOR_DEFAULT,
                    "the default campaign has no audience rule");
        }
        if (state.minimumOnScreenTime().isPresent()) {
            add(found, "minimumOnScreenTime", ViolationCodes.MINIMUM_ON_SCREEN_NOT_ALLOWED_FOR_DEFAULT,
                    "the default campaign has no minimum on-screen time");
        }
        if (!state.manualVetoes().isEmpty()) {
            add(found, "manualVetoes", ViolationCodes.VETOES_NOT_ALLOWED_FOR_DEFAULT,
                    "the default campaign has no manual vetoes");
        }
        if (!state.schedule().equals(Schedule.always())) {
            add(found, "schedule", ViolationCodes.SCHEDULE_NOT_CHANGEABLE_FOR_DEFAULT,
                    "the schedule of the default campaign is always on");
        }
    }

    private static void checkComplete(CampaignSnapshot state, List<Violation> found) {
        if (state.playlist().isEmpty()) {
            add(found, "playlist", ViolationCodes.PLAYLIST_EMPTY, "the playlist needs at least one item");
        }
        if (state.type() == CampaignType.REGULAR) {
            if (state.audienceRuleId().isEmpty()) {
                add(found, "audienceRuleId", ViolationCodes.AUDIENCE_RULE_REQUIRED,
                        "a regular campaign needs an audience rule");
            }
            if (state.status() == CampaignStatus.SCHEDULED && state.schedule().startDate().isEmpty()) {
                add(found, "startDate", ViolationCodes.SCHEDULE_START_DATE_REQUIRED,
                        "a scheduled campaign needs a start date");
            }
        }
    }

    private static void add(List<Violation> found, String field, String code, String message) {
        found.add(new Violation(field, code, message));
    }

    private CampaignInvariants() {
    }
}
