package com.cvretail.kiosko.campaigns.domain.valueobject;

import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;

/** Name of a campaign: trimmed, 1 to 100 characters. */
public record CampaignName(String value) {

    public static final int MAX_LENGTH = 100;

    public CampaignName {
        Require.notNull(value, "name");
        value = value.trim();
        if (value.isEmpty()) {
            throw new CampaignViolationException("name", ViolationCodes.NAME_BLANK, "name must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new CampaignViolationException(
                    "name", ViolationCodes.NAME_TOO_LONG, "name must have at most " + MAX_LENGTH + " characters");
        }
    }
}
