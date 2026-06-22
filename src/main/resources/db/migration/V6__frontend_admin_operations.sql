create table if not exists admin_store_operations (
    store_id bigint primary key references restaurants(id) on delete cascade,
    operation_status varchar(30) not null default 'operating',
    visibility_status varchar(30) not null default 'hidden',
    reason text,
    updated_by integer,
    updated_at timestamptz,
    version bigint not null default 0,
    constraint ck_admin_store_operation_status
        check (operation_status in ('operating','temporarily_closed','closed')),
    constraint ck_admin_store_visibility_status
        check (visibility_status in ('visible','hidden'))
);

create index if not exists idx_admin_store_operations_search
    on admin_store_operations (operation_status, visibility_status, updated_at desc);

create table if not exists admin_feed_moderation (
    feed_id integer primary key references fp_400(feed_no) on delete cascade,
    visibility_status varchar(30) not null default 'visible',
    recommended boolean not null default false,
    reason text,
    moderated_by integer,
    moderated_at timestamptz,
    version bigint not null default 0,
    constraint ck_admin_feed_visibility_status
        check (visibility_status in ('visible','hidden'))
);

create index if not exists idx_admin_feed_moderation_search
    on admin_feed_moderation (visibility_status, recommended, moderated_at desc);

create table if not exists service_feedback (
    id bigserial primary key,
    type varchar(40) not null,
    content text not null,
    status varchar(40) not null default 'received',
    contact varchar(320),
    contact_purge_at timestamptz,
    requester_user_id integer,
    assignee_user_id integer,
    internal_memo text,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint ck_service_feedback_status check (status in ('received','in_progress','resolved','improvement_candidate'))
);

create index if not exists idx_service_feedback_search
    on service_feedback (status, type, created_at desc);

create table if not exists content_verifications (
    id bigserial primary key,
    target_type varchar(40) not null,
    target_id varchar(100) not null,
    status varchar(40) not null default 'pending',
    requester_user_id integer,
    assignee_user_id integer,
    review_reason text,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint ck_content_verification_status check (status in ('pending','in_review','approved','rejected','changes_requested'))
);

create index if not exists idx_content_verifications_search
    on content_verifications (status, target_type, created_at desc);

create table if not exists content_verification_history (
    id bigserial primary key,
    verification_id bigint not null references content_verifications(id) on delete cascade,
    action varchar(40) not null,
    previous_status varchar(40),
    next_status varchar(40),
    actor_user_id integer not null,
    assignee_user_id integer,
    reason text,
    created_at timestamptz not null
);

create index if not exists idx_content_verification_history
    on content_verification_history (verification_id, created_at desc, id desc);

create table if not exists seasonal_curations (
    id bigserial primary key,
    title varchar(150) not null,
    description text,
    status varchar(30) not null default 'DRAFT',
    display_order integer not null default 0,
    starts_at timestamptz,
    ends_at timestamptz,
    store_ids text,
    menu_ids text,
    created_by integer not null,
    updated_by integer not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint ck_seasonal_curation_status check (status in ('DRAFT','SCHEDULED','PUBLISHED','ARCHIVED'))
);

create index if not exists idx_seasonal_curations_order
    on seasonal_curations (status, display_order, id);
