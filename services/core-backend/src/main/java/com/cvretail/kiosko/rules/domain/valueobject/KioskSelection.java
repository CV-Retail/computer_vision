package com.cvretail.kiosko.rules.domain.valueobject;

import java.util.Objects;
import java.util.Set;

/** The kiosks a campaign runs on: every kiosk of the store, or only the listed ones. */
public sealed interface KioskSelection permits KioskSelection.AllKiosks, KioskSelection.SelectedKiosks {

    boolean includes(KioskId kiosk);

    /** Every kiosk of the store. */
    record AllKiosks() implements KioskSelection {

        @Override
        public boolean includes(KioskId kiosk) {
            Objects.requireNonNull(kiosk, "kiosk");
            return true;
        }
    }

    /** Only the listed kiosks. */
    record SelectedKiosks(Set<KioskId> kiosks) implements KioskSelection {

        public SelectedKiosks {
            Objects.requireNonNull(kiosks, "kiosks");
            kiosks = Set.copyOf(kiosks);
            if (kiosks.isEmpty()) {
                throw new IllegalArgumentException("at least one kiosk is required");
            }
        }

        @Override
        public boolean includes(KioskId kiosk) {
            return kiosks.contains(Objects.requireNonNull(kiosk, "kiosk"));
        }
    }
}
