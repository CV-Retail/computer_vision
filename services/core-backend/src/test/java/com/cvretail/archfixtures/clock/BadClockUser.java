package com.cvretail.archfixtures.clock;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Fixture: reads the system clock and the default zone, which the clock rule forbids. */
public class BadClockUser {
    Instant now = Instant.now();
    LocalDateTime local = LocalDateTime.now();
    ZoneId zone = ZoneId.systemDefault();
}
