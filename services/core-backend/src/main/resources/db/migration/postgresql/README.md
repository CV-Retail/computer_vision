# PostgreSQL migrations

Migrations that only work on PostgreSQL go here (`V<version>__<description>.sql`).
Flyway shares one version number space across `common` and the vendor folder, so never reuse a
version that exists in `common`. Never edit an applied migration.
