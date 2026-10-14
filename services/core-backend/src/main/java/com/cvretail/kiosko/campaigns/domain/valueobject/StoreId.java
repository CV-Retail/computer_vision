package com.cvretail.kiosko.campaigns.domain.valueobject;

import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import java.util.regex.Pattern;

/** A store. Letters, digits, dot, underscore and hyphen; up to 64 characters. */
public record StoreId(String value) {

    private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

    public StoreId {
        Require.notNull(value, "storeId");
        if (!FORMAT.matcher(value).matches()) {
            throw new CampaignViolationException(
                    "storeId", ViolationCodes.STORE_INVALID, "storeId must match " + FORMAT.pattern());
        }
    }
}
