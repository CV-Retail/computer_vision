/**
 * Reports module: anonymous aggregates by time window and campaign (KIO-33).
 *
 * <p>The types in this package are the published surface of the module: the only ones other
 * modules may use. The sub-packages {@code domain}, {@code application} and
 * {@code infrastructure} are internal to the module.
 */
@ApplicationModule(displayName = "Reports")
package com.cvretail.kiosko.reports;

import org.springframework.modulith.ApplicationModule;
