package com.cvretail.kiosko.campaigns.domain.valueobject;

import java.util.UUID;

/** Reference to a veto, which is another aggregate. */
public record VetoId(UUID value) {

    public VetoId {
        Require.notNull(value, "vetoId");
    }
}
