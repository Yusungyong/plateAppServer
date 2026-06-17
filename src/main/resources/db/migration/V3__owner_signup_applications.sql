create table if not exists business_profiles (
    id bigserial primary key,
    user_id integer not null,
    owner_name varchar(100) not null,
    owner_phone varchar(40),
    owner_email varchar(320),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create unique index if not exists uq_business_profiles_user
    on business_profiles (user_id);

alter table store_applications
    add column if not exists business_name varchar(150);

alter table store_applications
    drop constraint if exists ck_store_application_approval_status;

alter table store_applications
    add constraint ck_store_application_approval_status
        check (approval_status in ('draft', 'pending', 'on_hold', 'approved', 'rejected'));

create unique index if not exists uq_store_application_documents_type
    on store_application_documents (application_id, document_type);
