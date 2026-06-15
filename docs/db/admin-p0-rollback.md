# Admin P0 migration rollback

## Default rollback

The P0 migrations are additive. If the application deployment fails, roll the
application back first and leave the new tables and `fp_100.token_version` in
place. The previous application version ignores them, so this is the safest
rollback and preserves approval, audit, and outbox records.

1. Stop the new application instances.
2. Deploy the previous application artifact.
3. Confirm login and existing public APIs.
4. Keep the Flyway history and additive schema unchanged.
5. Fix forward with a new migration version. Do not edit an applied migration.

## Full schema removal

Use full removal only before production traffic has written P0 data. First run:

```sql
select
    (select count(*) from store_applications) as applications,
    (select count(*) from admin_audit_logs) as audit_logs,
    (select count(*) from admin_outbox_events) as outbox_events,
    (select count(*) from store_owners) as store_owners;
```

All four values must be `0`, and a database backup must exist. Then remove the
objects in dependency order:

```sql
drop table if exists store_application_reviews;
drop table if exists store_application_documents;
drop table if exists store_application_menus;
drop table if exists store_application_categories;
drop table if exists store_applications;
drop table if exists store_owners;
drop table if exists admin_outbox_events;
drop table if exists admin_audit_logs;
drop table if exists admin_user_permissions;
alter table fp_100 drop column if exists token_version;
```

Delete the matching Flyway history rows only as part of this pre-production
full removal. Never perform this procedure after approval or audit data exists.
