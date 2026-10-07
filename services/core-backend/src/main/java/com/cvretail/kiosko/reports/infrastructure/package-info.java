/**
 * Infrastructure layer of the reports module: adapters for the database, Redis, HTTP and WebSocket
 * that implement the ports. All vendor-specific code lives here. Nothing outside this package
 * may depend on it, except the Spring wiring of the root package.
 */
package com.cvretail.kiosko.reports.infrastructure;
