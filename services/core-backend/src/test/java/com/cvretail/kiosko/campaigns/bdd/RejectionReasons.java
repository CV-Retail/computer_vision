package com.cvretail.kiosko.campaigns.bdd;

import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import java.util.Map;

/** Translates the reasons written in the scenarios into the stable violation codes. */
final class RejectionReasons {

    private static final Map<String, String> CODES = Map.ofEntries(
            Map.entry("the playlist is empty", ViolationCodes.PLAYLIST_EMPTY),
            Map.entry("the audience rule is missing", ViolationCodes.AUDIENCE_RULE_REQUIRED),
            Map.entry("the start date is missing", ViolationCodes.SCHEDULE_START_DATE_REQUIRED),
            Map.entry("the campaign is finished", ViolationCodes.STATUS_FINISHED),
            Map.entry("the transition is not allowed", ViolationCodes.STATUS_TRANSITION_NOT_ALLOWED),
            Map.entry("the name is blank", ViolationCodes.NAME_BLANK),
            Map.entry("the playlist is full", ViolationCodes.PLAYLIST_TOO_MANY_ITEMS),
            Map.entry("the position does not exist", ViolationCodes.PLAYLIST_POSITION_OUT_OF_RANGE),
            Map.entry("the duration is out of range", ViolationCodes.PLAYLIST_ITEM_DURATION_OUT_OF_RANGE),
            Map.entry("the minimum time is out of range", ViolationCodes.MINIMUM_ON_SCREEN_OUT_OF_RANGE),
            Map.entry("the end date is before the start date", ViolationCodes.SCHEDULE_END_BEFORE_START),
            Map.entry("no days are selected", ViolationCodes.SCHEDULE_DAYS_EMPTY),
            Map.entry("the time slot is empty", ViolationCodes.SCHEDULE_SLOT_EMPTY_RANGE),
            Map.entry("no kiosks are selected", ViolationCodes.SCOPE_KIOSKS_EMPTY),
            Map.entry("a kiosk is listed twice", ViolationCodes.SCOPE_KIOSKS_DUPLICATE),
            Map.entry("the veto is already on the campaign", ViolationCodes.VETO_DUPLICATE),
            Map.entry("the veto is not on the campaign", ViolationCodes.VETO_NOT_FOUND),
            Map.entry("the default campaign cannot have a priority", ViolationCodes.PRIORITY_NOT_ALLOWED_FOR_DEFAULT),
            Map.entry("the default campaign cannot have an audience rule",
                    ViolationCodes.AUDIENCE_RULE_NOT_ALLOWED_FOR_DEFAULT),
            Map.entry("the default campaign cannot have a minimum time",
                    ViolationCodes.MINIMUM_ON_SCREEN_NOT_ALLOWED_FOR_DEFAULT),
            Map.entry("the default campaign cannot have manual vetoes", ViolationCodes.VETOES_NOT_ALLOWED_FOR_DEFAULT),
            Map.entry("the schedule of the default campaign cannot change",
                    ViolationCodes.SCHEDULE_NOT_CHANGEABLE_FOR_DEFAULT));

    static String codeOf(String reason) {
        String code = CODES.get(reason);
        if (code == null) {
            throw new IllegalArgumentException("Unknown rejection reason in the scenario: " + reason);
        }
        return code;
    }

    private RejectionReasons() {
    }
}
