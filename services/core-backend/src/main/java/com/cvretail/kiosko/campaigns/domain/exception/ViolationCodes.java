package com.cvretail.kiosko.campaigns.domain.exception;

/** Stable codes of the violations. Codes are part of the API contract with the UI: do not rename. */
public final class ViolationCodes {

    public static final String STORE_INVALID = "store.invalid";
    public static final String KIOSK_INVALID = "kiosk.invalid";
    public static final String MEDIA_REF_INVALID = "media_ref.invalid";
    public static final String NAME_BLANK = "name.blank";
    public static final String NAME_TOO_LONG = "name.too_long";
    public static final String MINIMUM_ON_SCREEN_OUT_OF_RANGE = "minimum_on_screen.out_of_range";
    public static final String PLAYLIST_ITEM_DURATION_OUT_OF_RANGE = "playlist.item.duration_out_of_range";
    public static final String PLAYLIST_TOO_MANY_ITEMS = "playlist.too_many_items";
    public static final String PLAYLIST_POSITION_OUT_OF_RANGE = "playlist.position_out_of_range";
    public static final String SCHEDULE_END_BEFORE_START = "schedule.end_before_start";
    public static final String SCHEDULE_DAYS_EMPTY = "schedule.days.empty";
    public static final String SCHEDULE_SLOT_EMPTY_RANGE = "schedule.slot.empty_range";
    public static final String SCOPE_KIOSKS_EMPTY = "scope.kiosks.empty";
    public static final String SCOPE_KIOSKS_DUPLICATE = "scope.kiosks.duplicate";
    public static final String VETO_DUPLICATE = "veto.duplicate";
    public static final String VETO_NOT_FOUND = "veto.not_found";
    public static final String PRIORITY_NOT_ALLOWED_FOR_DEFAULT = "priority.not_allowed_for_default";
    public static final String AUDIENCE_RULE_NOT_ALLOWED_FOR_DEFAULT = "audience_rule.not_allowed_for_default";
    public static final String MINIMUM_ON_SCREEN_NOT_ALLOWED_FOR_DEFAULT = "minimum_on_screen.not_allowed_for_default";
    public static final String VETOES_NOT_ALLOWED_FOR_DEFAULT = "vetoes.not_allowed_for_default";
    public static final String SCHEDULE_NOT_CHANGEABLE_FOR_DEFAULT = "schedule.not_changeable_for_default";
    public static final String PLAYLIST_EMPTY = "playlist.empty";
    public static final String AUDIENCE_RULE_REQUIRED = "audience_rule.required";
    public static final String SCHEDULE_START_DATE_REQUIRED = "schedule.start_date_required";
    public static final String STATUS_TRANSITION_NOT_ALLOWED = "status.transition_not_allowed";
    public static final String STATUS_FINISHED = "status.finished";
    public static final String PRIORITY_REQUIRED = "priority.required";
    public static final String MINIMUM_ON_SCREEN_REQUIRED = "minimum_on_screen.required";

    /** Code of a mandatory value that is missing: {@code <field>.required}. */
    public static String required(String field) {
        return field + ".required";
    }

    private ViolationCodes() {
    }
}
