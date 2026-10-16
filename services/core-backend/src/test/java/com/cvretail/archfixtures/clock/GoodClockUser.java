package com.cvretail.archfixtures.clock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;

/** Fixture: uses only the overloads that take a clock, which the clock rule allows. */
public class GoodClockUser {
    Instant now = Instant.now(Clock.systemUTC());
    ZonedDateTime zoned = ZonedDateTime.now(Clock.systemUTC());
}
