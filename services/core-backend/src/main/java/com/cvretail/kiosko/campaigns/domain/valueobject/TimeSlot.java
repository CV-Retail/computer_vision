package com.cvretail.kiosko.campaigns.domain.valueobject;

import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import java.time.LocalTime;

/** Time of day window in store-local time. A start later than the end crosses midnight. */
public record TimeSlot(LocalTime start, LocalTime end) {

    public TimeSlot {
        Require.notNull(start, "slot.start");
        Require.notNull(end, "slot.end");
        if (start.equals(end)) {
            throw new CampaignViolationException("slot", ViolationCodes.SCHEDULE_SLOT_EMPTY_RANGE,
                    "the start and the end of the time slot must differ");
        }
    }
}
