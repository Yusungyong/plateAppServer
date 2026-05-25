-- Social signup temporary session storage
-- Existing social identity table fp_110 is reused as-is.
-- New table fp_111 is required for signup_required -> signup_complete flow.

create table if not exists fp_111 (
    id bigserial primary key,
    signup_token varchar(255) not null,
    provider varchar(30) not null,
    provider_user_id varchar(255) not null,
    email varchar(255),
    nickname varchar(255),
    raw_profile_json text,
    expires_at timestamptz not null,
    consumed_at timestamptz,
    created_at timestamptz not null default now()
);

create unique index if not exists uk_fp111_signup_token
    on fp_111 (signup_token);

create index if not exists idx_fp111_provider_user
    on fp_111 (provider, provider_user_id);

create index if not exists idx_fp111_expires_at
    on fp_111 (expires_at);

create index if not exists idx_fp111_consumed_at
    on fp_111 (consumed_at);
