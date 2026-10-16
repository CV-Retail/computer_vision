package com.cvretail.kiosko.rules.domain.valueobject;

/** Whether a campaign may run on a kiosk now, or the first reason why not (in the order of the checks). */
public enum Availability {
    APPLIES,
    STATUS_NOT_ELIGIBLE,
    BEFORE_START_DATE,
    AFTER_END_DATE,
    DAY_NOT_INCLUDED,
    OUTSIDE_TIME_SLOT,
    KIOSK_NOT_INCLUDED;

    public boolean applies() {
        return this == APPLIES;
    }
}
