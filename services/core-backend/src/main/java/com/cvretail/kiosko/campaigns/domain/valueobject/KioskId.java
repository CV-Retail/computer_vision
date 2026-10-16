package com.cvretail.kiosko.campaigns.domain.valueobject;

import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import java.util.regex.Pattern;

/** A kiosk. Letters, digits, dot, underscore and hyphen; up to 64 characters. */
public record KioskId(String value) {

    private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

    public KioskId {
        Require.notNull(value, "kioskId");
        if (!FORMAT.matcher(value).matches()) {
            throw new CampaignViolationException(
                    "kioskId", ViolationCodes.KIOSK_INVALID, "kioskId must match " + FORMAT.pattern());
        }
    }
}
