# Database Reference

This file summarizes how the backend is currently configured to connect to the database and how to inspect it locally.

## Current Backend Database

Source files
- [application.yaml](C:/workspace/plate-main/src/main/resources/application.yaml)
- [application-local.yaml](C:/workspace/plate-main/src/main/resources/application-local.yaml)

Current default datasource values from `application.yaml`
- engine: `PostgreSQL`
- host: `plateappdb.cp6806wks0x5.ap-northeast-2.rds.amazonaws.com`
- port: `5432`
- database: `mainproject_2`
- username: `master_user`
- driver: `org.postgresql.Driver`

Current effective notes
- `application-local.yaml` currently overrides `spring.datasource.password`
- `application-local.yaml` does not override `url` or `username`
- so local runtime still points to the same host, port, database, and username unless profile handling is changed elsewhere

## Important Security Note

Sensitive values are currently stored in config files under `src/main/resources`.

That includes at least:
- datasource password
- OAuth client secrets
- AWS credentials
- JWT secret

Recommended follow-up
- move secrets to environment variables or external secret management
- keep only placeholders or `${ENV_NAME}` lookups in committed config

This document does not repeat the raw secret values.
If they must be checked, read them directly from:
- [application.yaml](C:/workspace/plate-main/src/main/resources/application.yaml)
- [application-local.yaml](C:/workspace/plate-main/src/main/resources/application-local.yaml)

## JDBC URL

Current JDBC URL:

```text
jdbc:postgresql://plateappdb.cp6806wks0x5.ap-northeast-2.rds.amazonaws.com:5432/mainproject_2
```

## psql Connection Example

If `psql` is installed locally, use this pattern:

```powershell
$env:PGPASSWORD = '<read-from-config-or-env>'
psql -h plateappdb.cp6806wks0x5.ap-northeast-2.rds.amazonaws.com -p 5432 -U master_user -d mainproject_2
```

If you only want a one-shot query:

```powershell
$env:PGPASSWORD = '<read-from-config-or-env>'
psql -h plateappdb.cp6806wks0x5.ap-northeast-2.rds.amazonaws.com -p 5432 -U master_user -d mainproject_2 -c "select now();"
```

## Useful Inspection Queries

### 1. List tables

```sql
select table_schema, table_name
from information_schema.tables
where table_schema = 'public'
order by table_name;
```

### 2. Check foreign keys referencing `fp_100`

```sql
select
    tc.table_name,
    kcu.column_name,
    ccu.table_name as referenced_table,
    ccu.column_name as referenced_column,
    tc.constraint_name
from information_schema.table_constraints tc
join information_schema.key_column_usage kcu
  on tc.constraint_name = kcu.constraint_name
 and tc.table_schema = kcu.table_schema
join information_schema.constraint_column_usage ccu
  on ccu.constraint_name = tc.constraint_name
 and ccu.table_schema = tc.table_schema
where tc.constraint_type = 'FOREIGN KEY'
  and ccu.table_name = 'fp_100'
order by tc.table_name, kcu.column_name;
```

### 3. Inspect a user in `fp_100`

```sql
select *
from fp_100
where username = 'example_user';
```

### 4. Check social mapping in `fp_110`

```sql
select *
from fp_110
where user_id = 39;
```

### 5. Check refresh tokens in `fp_103`

```sql
select *
from fp_103
where username = 'example_user';
```

### 6. Check delete-history records in `fp_101`

```sql
select history_id, username, change_tp, before_ex, after_ex, created_dt
from fp_101
where username = 'example_user'
order by created_dt desc;
```

## Queries Useful For Account Deletion Debugging

### Find rows in `fp_440` still referencing a user

```sql
select comment_id, user_id, username, content, created_at
from fp_440
where user_id = 39;
```

### Find rows in `fp_305` still referencing a user

```sql
select id, user_id, username, store_id, timestamp
from fp_305
where user_id = 39;
```

### Check remaining username-based content

```sql
select store_id, username, title, created_at
from fp_300
where username = 'example_user';
```

```sql
select feed_no, username, feed_title, created_at
from fp_400
where username = 'example_user';
```

## Recommended Local Workflow

1. Read datasource values from [application.yaml](C:/workspace/plate-main/src/main/resources/application.yaml).
2. Check whether [application-local.yaml](C:/workspace/plate-main/src/main/resources/application-local.yaml) overrides any datasource fields.
3. Export the DB password into `PGPASSWORD`.
4. Connect with `psql`.
5. Run inspection SQL before and after backend operations such as account deletion.

## Current Risk

The backend is configured against a non-local PostgreSQL host, not an embedded test DB.

That means:
- direct queries may hit a shared remote database
- inspection commands must be used carefully
- destructive SQL should not be run casually

Use read-only queries unless the change is intentional and reviewed.
