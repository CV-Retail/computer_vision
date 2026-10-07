# H2 migrations

Migrations that only work on H2 go here (`V<version>__<description>.sql`).
Flyway shares one version number space across `common` and the vendor folder, so never reuse a
version that exists in `common`. Never edit an applied migration.
