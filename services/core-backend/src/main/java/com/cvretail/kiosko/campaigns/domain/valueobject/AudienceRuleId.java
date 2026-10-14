package com.cvretail.kiosko.campaigns.domain.valueobject;

import java.util.UUID;

/** Reference to an audience rule, which is another aggregate. */
public record AudienceRuleId(UUID value) {

    public AudienceRuleId {
        Require.notNull(value, "audienceRuleId");
    }
}
