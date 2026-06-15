insert into admin_user_permissions (user_id, permission, granted_by)
select u.user_id, p.permission, u.user_id
from fp_100 u
cross join (
    values
        ('FAQ_MANAGE'),
        ('QNA_MANAGE'),
        ('MEMBER_MONITORING_READ'),
        ('RESTAURANT_MANAGE')
) as p(permission)
where upper(coalesce(u.role, '')) in ('ADMIN', 'ADM', '993', 'SUPER_ADMIN', 'SUPERADMIN')
  and u.user_id is not null
  and not exists (
      select 1
      from admin_user_permissions existing
      where existing.user_id = u.user_id
        and existing.permission = p.permission
        and existing.revoked_at is null
  );
