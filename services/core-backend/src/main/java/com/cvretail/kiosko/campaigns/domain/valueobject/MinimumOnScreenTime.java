package com.cvretail.kiosko.campaigns.domain.valueobject;

import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;

/** Minimum time, in whole seconds, that a campaign stays on screen before it can be replaced. */
public record MinimumOnScreenTime(int seconds) {

    public static final int MIN_SECONDS = 1;
    public static final int MAX_SECONDS = 3600;
    public static final int DEFAULT_SECONDS = 8;

    public MinimumOnScreenTime {
        Require.inRange(seconds, MIN_SECONDS, MAX_SECONDS, "minimumOnScreenTime",
                ViolationCodes.MINIMUM_ON_SCREEN_OUT_OF_RANGE);
    }

    public static MinimumOnScreenTime defaultValue() {
        return new MinimumOnScreenTime(DEFAULT_SECONDS);
    }
}
