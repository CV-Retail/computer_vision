/**
 * Rules module: deterministic decision of what to play for an observed audience (KIO-25 to KIO-28).
 *
 * <p>The types in this package are the published surface of the module: the only ones other
 * modules may use. The sub-packages {@code domain}, {@code application} and
 * {@code infrastructure} are internal to the module.
 */
@ApplicationModule(displayName = "Rules")
package com.cvretail.kiosko.rules;

import org.springframework.modulith.ApplicationModule;
