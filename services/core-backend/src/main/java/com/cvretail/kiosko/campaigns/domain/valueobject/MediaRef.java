package com.cvretail.kiosko.campaigns.domain.valueobject;

import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import java.util.regex.Pattern;

/** Content hash of a media file: {@code sha256:} followed by 64 lowercase hexadecimal characters. */
public record MediaRef(String value) {

    private static final Pattern FORMAT = Pattern.compile("sha256:[0-9a-f]{64}");

    public MediaRef {
        Require.notNull(value, "mediaRef");
        if (!FORMAT.matcher(value).matches()) {
            throw new CampaignViolationException(
                    "mediaRef", ViolationCodes.MEDIA_REF_INVALID, "mediaRef must match " + FORMAT.pattern());
        }
    }
}
