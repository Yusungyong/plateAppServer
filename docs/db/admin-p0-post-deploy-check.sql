-- Run after Flyway V1 and V2. Every query should return the expected value
-- described above it.

-- Expected: token_version
select column_name
from information_schema.columns
where table_name = 'fp_100'
  and column_name = 'token_version';

-- Expected: 10 rows
select count(*) as required_table_count
from information_schema.tables
where table_schema = current_schema()
  and table_name in (
      'admin_user_permissions',
      'store_owners',
      'store_applications',
      'store_application_categories',
      'store_application_menus',
      'store_application_documents',
      'store_application_change_requests',
      'store_application_change_request_items',
      'admin_audit_logs',
      'admin_outbox_events'
  );

-- Expected: 0 rows
with required_permissions(permission) as (
    values
        ('ADMIN_ACCESS'),
        ('DASHBOARD_READ'),
        ('STORE_READ'),
        ('STORE_APPROVE'),
        ('STORE_UPDATE'),
        ('FEED_READ'),
        ('FEED_MODERATE'),
        ('FEED_FEATURE'),
        ('SEASONAL_READ'),
        ('SEASONAL_MANAGE'),
        ('REPORT_READ'),
        ('BANNER_MANAGE'),
        ('NOTICE_MANAGE'),
        ('SUPPORT_MANAGE'),
        ('ADMIN_ACCOUNT_MANAGE'),
        ('SETTING_MANAGE'),
        ('AUDIT_LOG_READ'),
        ('FAQ_MANAGE'),
        ('QNA_MANAGE'),
        ('MEMBER_MONITORING_READ'),
        ('RESTAURANT_MANAGE')
)
select u.user_id, u.username, required_permissions.permission
from fp_100 u
cross join required_permissions
where upper(coalesce(u.role, '')) in ('ADMIN', 'ADM', '993', 'SUPER_ADMIN', 'SUPERADMIN')
  and not exists (
      select 1
      from admin_user_permissions permission
      where permission.user_id = u.user_id
        and permission.permission = required_permissions.permission
        and permission.revoked_at is null
  )
order by u.user_id, required_permissions.permission;

-- Expected: 0 rows
select id, approval_status, verification_status
from store_applications
where approval_status not in ('draft', 'pending', 'on_hold', 'approved', 'rejected')
   or verification_status not in ('not_requested', 'reviewing', 'verified', 'rejected');

-- Expected: 0 rows
select id, status, attempt_count
from admin_outbox_events
where status not in ('pending', 'processing', 'processed', 'failed')
   or attempt_count < 0;
