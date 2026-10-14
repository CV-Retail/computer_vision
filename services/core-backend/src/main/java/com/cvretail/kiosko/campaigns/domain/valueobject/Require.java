package com.cvretail.kiosko.campaigns.domain.valueobject;

import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;

/** Argument checks shared by the value objects. */
final class Require {

    static <T> T notNull(T value, String field) {
        if (value == null) {
            throw new CampaignViolationException(
                    field, ViolationCodes.required(field), field + " is required");
        }
        return value;
    }

    static void inRange(int value, int min, int max, String field, String code) {
        if (value < min || value > max) {
            throw new CampaignViolationException(
                    field, code, field + " must be between " + min + " and " + max + " but was " + value);
        }
    }

    private Require() {
    }
}
