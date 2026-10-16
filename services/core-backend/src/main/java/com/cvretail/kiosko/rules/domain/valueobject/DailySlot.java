package com.cvretail.kiosko.rules.domain.valueobject;

import java.time.LocalTime;
import java.util.Objects;

/** Window of the day in local time. The start is included and the end is excluded. */
public record DailySlot(LocalTime start, LocalTime end) {

    public DailySlot {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (start.equals(end)) {
            throw new IllegalArgumentException("the start and the end of a slot must differ");
        }
    }

    /** A slot whose start is later than its end continues after midnight. */
    public boolean crossesMidnight() {
        return start.isAfter(end);
    }

    /** True if the time of day is inside the slot, wherever the day starts. */
    public boolean contains(LocalTime time) {
        if (crossesMidnight()) {
            return !time.isBefore(start) || time.isBefore(end);
        }
        return !time.isBefore(start) && time.isBefore(end);
    }
}
