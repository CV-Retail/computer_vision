package com.cvretail.kiosko.campaigns.domain.valueobject;

import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;

/** One medium of a playlist and how long it stays on screen, in whole seconds. */
public record PlaylistItem(MediaRef media, int durationSeconds) {

    public static final int MIN_DURATION_SECONDS = 1;
    public static final int MAX_DURATION_SECONDS = 3600;

    public PlaylistItem {
        Require.notNull(media, "media");
        Require.inRange(durationSeconds, MIN_DURATION_SECONDS, MAX_DURATION_SECONDS, "durationSeconds",
                ViolationCodes.PLAYLIST_ITEM_DURATION_OUT_OF_RANGE);
    }
}
