create table if not exists store_application_change_requests (
    id bigserial primary key,
    application_id bigint not null references store_applications(id) on delete cascade,
    review_id bigint references store_application_reviews(id) on delete set null,
    applicant_message text not null,
    status varchar(30) not null default 'open',
    requested_by integer not null,
    requested_at timestamptz not null,
    resolved_at timestamptz,
    request_id varchar(100),
    constraint ck_store_application_change_request_status
        check (status in ('open', 'resubmitted', 'closed'))
);

create index if not exists idx_store_application_change_requests_application
    on store_application_change_requests (application_id, requested_at desc, id desc);

create index if not exists idx_store_application_change_requests_status
    on store_application_change_requests (application_id, status, requested_at desc);

create table if not exists store_application_change_request_items (
    id bigserial primary key,
    change_request_id bigint not null references store_application_change_requests(id) on delete cascade,
    field varchar(120) not null,
    label varchar(150) not null,
    reason_code varchar(80) not null,
    message text not null,
    edit_path text,
    display_order integer not null default 0
);

create index if not exists idx_store_application_change_request_items_request
    on store_application_change_request_items (change_request_id, display_order asc, id asc);
