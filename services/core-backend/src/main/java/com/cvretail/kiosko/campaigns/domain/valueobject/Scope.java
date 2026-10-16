package com.cvretail.kiosko.campaigns.domain.valueobject;

import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Where a campaign runs: every kiosk of the store, or a non-empty set of kiosks. */
public sealed interface Scope permits Scope.AllKiosks, Scope.SelectedKiosks {

    static Scope all() {
        return new AllKiosks();
    }

    /** Every kiosk of the store. */
    record AllKiosks() implements Scope {
    }

    /** Only the listed kiosks. */
    record SelectedKiosks(Set<KioskId> kiosks) implements Scope {

        public SelectedKiosks {
            Require.notNull(kiosks, "kiosks");
            kiosks = Set.copyOf(kiosks);
            if (kiosks.isEmpty()) {
                throw new CampaignViolationException("scope", ViolationCodes.SCOPE_KIOSKS_EMPTY,
                        "at least one kiosk is required");
            }
        }

        /** Builds the scope and rejects repeated kiosks, which a plain set would hide. */
        public static SelectedKiosks of(Collection<KioskId> kiosks) {
            Require.notNull(kiosks, "kiosks");
            Set<KioskId> unique = new HashSet<>(kiosks);
            if (unique.size() != kiosks.size()) {
                throw new CampaignViolationException("scope", ViolationCodes.SCOPE_KIOSKS_DUPLICATE,
                        "a kiosk is listed more than once");
            }
            return new SelectedKiosks(unique);
        }
    }
}
