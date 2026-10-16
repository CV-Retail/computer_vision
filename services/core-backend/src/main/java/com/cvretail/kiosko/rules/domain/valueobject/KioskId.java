package com.cvretail.kiosko.rules.domain.valueobject;

/** Identifier of a kiosk. Compared exactly, case sensitive. */
public record KioskId(String value) {

    public KioskId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("kiosk id must not be blank");
        }
    }
}
