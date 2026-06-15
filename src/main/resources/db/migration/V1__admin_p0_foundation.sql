alter table fp_100
    add column if not exists token_version integer not null default 0;

create table if not exists admin_user_permissions (
    id bigserial primary key,
    user_id integer not null,
    permission varchar(80) not null,
    granted_by integer,
    granted_at timestamptz not null default now(),
    revoked_at timestamptz
);

create unique index if not exists uq_admin_user_permissions_active
    on admin_user_permissions (user_id, permission)
    where revoked_at is null;

create index if not exists idx_admin_user_permissions_user
    on admin_user_permissions (user_id, revoked_at);

create table if not exists store_owners (
    id bigserial primary key,
    store_id bigint not null,
    user_id integer not null,
    owner_role varchar(30) not null default 'OWNER',
    created_at timestamptz not null default now(),
    revoked_at timestamptz
);

create unique index if not exists uq_store_owners_active
    on store_owners (store_id, user_id)
    where revoked_at is null;

create index if not exists idx_store_owners_user
    on store_owners (user_id, revoked_at);

create table if not exists store_applications (
    id bigserial primary key,
    parent_application_id bigint references store_applications(id),
    store_id bigint,
    applicant_user_id integer not null,
    store_name varchar(150) not null,
    region_code varchar(50) not null,
    address varchar(300) not null,
    phone varchar(40),
    email varchar(320),
    owner_name varchar(100) not null,
    business_number_encrypted bytea not null,
    business_number_hash varchar(128) not null,
    approval_status varchar(30) not null,
    verification_status varchar(30) not null,
    main_image_object_key text,
    description text,
    applied_at timestamptz not null,
    updated_at timestamptz not null,
    reviewed_at timestamptz,
    reviewed_by integer,
    version bigint not null default 0,
    constraint ck_store_application_approval_status
        check (approval_status in ('pending', 'on_hold', 'approved', 'rejected')),
    constraint ck_store_application_verification_status
        check (verification_status in ('not_requested', 'reviewing', 'verified', 'rejected'))
);

create index if not exists idx_store_applications_status_applied
    on store_applications (approval_status, applied_at desc);
create index if not exists idx_store_applications_verification
    on store_applications (verification_status);
create index if not exists idx_store_applications_applicant
    on store_applications (applicant_user_id, applied_at desc);
create index if not exists idx_store_applications_business_hash
    on store_applications (business_number_hash);

create table if not exists store_application_categories (
    id bigserial primary key,
    application_id bigint not null references store_applications(id) on delete cascade,
    category_code varchar(50) not null,
    display_order integer not null default 0
);

create index if not exists idx_store_application_categories_application
    on store_application_categories (application_id, display_order, id);

create table if not exists store_application_menus (
    id bigserial primary key,
    application_id bigint not null references store_applications(id) on delete cascade,
    name varchar(150) not null,
    price numeric(12, 2),
    description text,
    display_order integer not null default 0
);

create index if not exists idx_store_application_menus_application
    on store_application_menus (application_id, display_order, id);

create table if not exists store_application_documents (
    id bigserial primary key,
    application_id bigint not null references store_applications(id) on delete cascade,
    document_type varchar(50) not null,
    object_key text not null,
    original_name varchar(255) not null,
    mime_type varchar(120),
    file_size_bytes bigint,
    verification_status varchar(30) not null,
    created_at timestamptz not null,
    purge_at timestamptz,
    constraint ck_store_application_document_status
        check (verification_status in ('submitted', 'reviewing', 'verified', 'rejected'))
);

create index if not exists idx_store_application_documents_application
    on store_application_documents (application_id, created_at, id);

create table if not exists store_application_reviews (
    id bigserial primary key,
    application_id bigint not null references store_applications(id) on delete cascade,
    previous_status varchar(30) not null,
    next_status varchar(30) not null,
    reason_code varchar(80),
    reason text,
    comment text,
    reviewed_by integer not null,
    reviewed_at timestamptz not null,
    request_id varchar(100)
);

create index if not exists idx_store_application_reviews_application
    on store_application_reviews (application_id, reviewed_at desc, id desc);

create table if not exists admin_audit_logs (
    id bigserial primary key,
    occurred_at timestamptz not null,
    actor_user_id integer not null,
    actor_role varchar(50) not null,
    action varchar(100) not null,
    resource_type varchar(80) not null,
    resource_id varchar(100) not null,
    previous_value jsonb,
    next_value jsonb,
    reason_code varchar(80),
    reason text,
    ip_address varchar(64),
    user_agent varchar(512),
    request_id varchar(100)
);

create index if not exists idx_admin_audit_logs_resource
    on admin_audit_logs (resource_type, resource_id, occurred_at desc);
create index if not exists idx_admin_audit_logs_actor
    on admin_audit_logs (actor_user_id, occurred_at desc);
create index if not exists idx_admin_audit_logs_action
    on admin_audit_logs (action, occurred_at desc);

create table if not exists admin_outbox_events (
    id bigserial primary key,
    event_type varchar(100) not null,
    aggregate_type varchar(80) not null,
    aggregate_id varchar(100) not null,
    payload jsonb not null,
    status varchar(30) not null default 'pending',
    attempt_count integer not null default 0,
    available_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    processed_at timestamptz,
    last_error text,
    constraint ck_admin_outbox_status
        check (status in ('pending', 'processing', 'processed', 'failed'))
);

create index if not exists idx_admin_outbox_dispatch
    on admin_outbox_events (status, available_at, id);

insert into admin_user_permissions (user_id, permission, granted_by)
select u.user_id, p.permission, u.user_id
from fp_100 u
cross join (
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
        ('AUDIT_LOG_READ')
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
