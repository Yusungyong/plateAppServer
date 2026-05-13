-- =========================================================
-- Home Video Recommendation DDL
-- - Existing tables are kept as-is.
-- - New recommendation tables use fp_370 ~ fp_375.
-- - PostgreSQL baseline.
-- =========================================================

-- =========================================================
-- fp_370: video behavior event log
-- Stores raw user/guest behavior used by personalization.
-- Existing fp_303/fp_305 can be backfilled into this table later.
-- =========================================================
create table if not exists fp_370 (
    event_id              bigserial primary key,
    event_uid             varchar(100),

    user_id               integer,
    username              varchar(50),
    is_guest              boolean not null default false,
    guest_id              varchar(100),
    session_id            varchar(150),
    device_id             varchar(200),

    store_id              integer not null,
    place_id              varchar(255),
    creator_username      varchar(50),

    event_type            varchar(40) not null,
    event_source          varchar(40) not null default 'HOME',
    request_id            varchar(100),
    algorithm_version     varchar(50),

    impression_position   integer,
    play_position_ms      integer,
    watch_duration_ms     integer,
    video_duration_ms     integer,
    completion_ratio      numeric(6,5),

    client_event_at       timestamp,
    server_event_at       timestamp not null default now(),
    ip_address            varchar(64),
    user_agent            text,
    metadata              jsonb,

    constraint ck_fp_370_actor_present
        check (
            user_id is not null
            or username is not null
            or guest_id is not null
        ),
    constraint ck_fp_370_event_type
        check (
            event_type in (
                'IMPRESSION',
                'CLICK',
                'PLAY_START',
                'PLAY_PROGRESS',
                'PLAY_COMPLETE',
                'SKIP',
                'LIKE',
                'UNLIKE',
                'COMMENT',
                'SHARE',
                'HIDE',
                'NOT_INTERESTED',
                'REPORT'
            )
        ),
    constraint ck_fp_370_completion_ratio
        check (completion_ratio is null or (completion_ratio >= 0 and completion_ratio <= 1))
);

create unique index if not exists uq_fp_370_event_uid
    on fp_370(event_uid)
    where event_uid is not null;

create index if not exists idx_fp_370_user_time
    on fp_370(user_id, server_event_at desc)
    where user_id is not null;

create index if not exists idx_fp_370_username_time
    on fp_370(username, server_event_at desc)
    where username is not null;

create index if not exists idx_fp_370_guest_time
    on fp_370(guest_id, server_event_at desc)
    where guest_id is not null;

create index if not exists idx_fp_370_store_event_time
    on fp_370(store_id, event_type, server_event_at desc);

create index if not exists idx_fp_370_place_time
    on fp_370(place_id, server_event_at desc)
    where place_id is not null;

create index if not exists idx_fp_370_request
    on fp_370(request_id)
    where request_id is not null;


-- =========================================================
-- fp_371: user/guest video preference profile
-- Stores aggregated preference scores by subject.
-- Example subject_type values: PLACE, REGION, CREATOR, TAG, CATEGORY,
-- DURATION_BUCKET, TIME_BUCKET.
-- =========================================================
create table if not exists fp_371 (
    preference_id         bigserial primary key,

    user_id               integer,
    username              varchar(50),
    is_guest              boolean not null default false,
    guest_id              varchar(100),

    subject_type          varchar(40) not null,
    subject_key           varchar(255) not null,
    score                 numeric(12,6) not null default 0,
    positive_count        integer not null default 0,
    negative_count        integer not null default 0,
    impression_count      integer not null default 0,
    last_event_at         timestamp,

    model_version         varchar(50) not null default 'v1',
    created_at            timestamp not null default now(),
    updated_at            timestamp not null default now(),

    constraint ck_fp_371_actor_present
        check (
            user_id is not null
            or username is not null
            or guest_id is not null
        )
);

create unique index if not exists uq_fp_371_user_subject
    on fp_371(user_id, model_version, subject_type, subject_key)
    where user_id is not null;

create unique index if not exists uq_fp_371_username_subject
    on fp_371(username, model_version, subject_type, subject_key)
    where username is not null;

create unique index if not exists uq_fp_371_guest_subject
    on fp_371(guest_id, model_version, subject_type, subject_key)
    where guest_id is not null;

create index if not exists idx_fp_371_user_score
    on fp_371(user_id, model_version, subject_type, score desc)
    where user_id is not null;

create index if not exists idx_fp_371_guest_score
    on fp_371(guest_id, model_version, subject_type, score desc)
    where guest_id is not null;


-- =========================================================
-- fp_372: video recommendation feature snapshot
-- One row per video/store. Values can be refreshed from fp_300/fp_310/fp_350.
-- =========================================================
create table if not exists fp_372 (
    store_id              integer primary key,
    place_id              varchar(255),
    creator_username      varchar(50),

    store_name            varchar(255),
    title                 varchar(255),
    address               varchar(500),
    region_1              varchar(100),
    region_2              varchar(100),

    latitude              numeric(10,7),
    longitude             numeric(10,7),
    video_duration        integer,
    duration_bucket       varchar(40),

    tags                  jsonb,
    categories            jsonb,
    feature_vector        jsonb,

    like_count            bigint not null default 0,
    comment_count         bigint not null default 0,
    impression_count      bigint not null default 0,
    click_count           bigint not null default 0,
    play_count            bigint not null default 0,
    complete_count        bigint not null default 0,
    hide_count            bigint not null default 0,
    report_count          bigint not null default 0,

    popularity_score      numeric(12,6) not null default 0,
    quality_score         numeric(12,6) not null default 0,
    freshness_score       numeric(12,6) not null default 0,

    content_created_at    date,
    content_updated_at    date,
    feature_refreshed_at  timestamp not null default now(),
    created_at            timestamp not null default now(),
    updated_at            timestamp not null default now()
);

create index if not exists idx_fp_372_place
    on fp_372(place_id)
    where place_id is not null;

create index if not exists idx_fp_372_creator
    on fp_372(creator_username)
    where creator_username is not null;

create index if not exists idx_fp_372_region
    on fp_372(region_1, region_2);

create index if not exists idx_fp_372_popularity
    on fp_372(popularity_score desc, content_created_at desc);


-- =========================================================
-- fp_373: home recommendation candidate pool
-- Optional precomputed candidates for fast home feed serving.
-- Candidate rows are disposable and can expire.
-- =========================================================
create table if not exists fp_373 (
    candidate_id          bigserial primary key,
    batch_key             varchar(100) not null,

    user_id               integer,
    username              varchar(50),
    is_guest              boolean not null default false,
    guest_id              varchar(100),

    store_id              integer not null,
    candidate_source      varchar(40) not null,
    base_score            numeric(12,6) not null default 0,
    rank_score            numeric(12,6) not null default 0,
    reason_code           varchar(80),
    reason_payload        jsonb,

    algorithm_version     varchar(50) not null default 'v1',
    generated_at          timestamp not null default now(),
    expires_at            timestamp,

    constraint ck_fp_373_actor_present
        check (
            user_id is not null
            or username is not null
            or guest_id is not null
        ),
    constraint ck_fp_373_candidate_source
        check (
            candidate_source in (
                'PERSONALIZED',
                'NEARBY',
                'POPULAR',
                'RECENT',
                'SIMILAR_PLACE',
                'SIMILAR_CREATOR',
                'EXPLORATION',
                'FALLBACK'
            )
        )
);

create unique index if not exists uq_fp_373_user_batch_store_source
    on fp_373(batch_key, user_id, store_id, candidate_source)
    where user_id is not null;

create unique index if not exists uq_fp_373_username_batch_store_source
    on fp_373(batch_key, username, store_id, candidate_source)
    where username is not null;

create unique index if not exists uq_fp_373_guest_batch_store_source
    on fp_373(batch_key, guest_id, store_id, candidate_source)
    where guest_id is not null;

create index if not exists idx_fp_373_user_rank
    on fp_373(user_id, algorithm_version, rank_score desc)
    where user_id is not null;

create index if not exists idx_fp_373_guest_rank
    on fp_373(guest_id, algorithm_version, rank_score desc)
    where guest_id is not null;

create index if not exists idx_fp_373_expires
    on fp_373(expires_at)
    where expires_at is not null;


-- =========================================================
-- fp_374: recommendation serving request log
-- One row per home recommendation response.
-- =========================================================
create table if not exists fp_374 (
    serving_id            bigserial primary key,
    request_id            varchar(100) not null,

    user_id               integer,
    username              varchar(50),
    is_guest              boolean not null default false,
    guest_id              varchar(100),
    session_id            varchar(150),

    endpoint              varchar(100) not null default '/api/home/video-thumbnails',
    page_no               integer,
    page_size             integer,
    sort_type             varchar(40),
    latitude              numeric(10,7),
    longitude             numeric(10,7),
    radius_meters         numeric(10,2),
    place_ids             jsonb,

    algorithm_version     varchar(50) not null default 'v1',
    candidate_count       integer not null default 0,
    served_count          integer not null default 0,
    fallback_used         boolean not null default false,

    served_at             timestamp not null default now(),
    metadata              jsonb,

    constraint ck_fp_374_actor_present
        check (
            user_id is not null
            or username is not null
            or guest_id is not null
        )
);

create unique index if not exists uq_fp_374_request_id
    on fp_374(request_id);

create index if not exists idx_fp_374_user_time
    on fp_374(user_id, served_at desc)
    where user_id is not null;

create index if not exists idx_fp_374_guest_time
    on fp_374(guest_id, served_at desc)
    where guest_id is not null;


-- =========================================================
-- fp_375: recommendation serving item log
-- One row per video returned in a recommendation response.
-- =========================================================
create table if not exists fp_375 (
    serving_item_id       bigserial primary key,
    serving_id            bigint not null,
    request_id            varchar(100) not null,

    store_id              integer not null,
    position_no           integer not null,
    candidate_source      varchar(40),
    rank_score            numeric(12,6),
    reason_code           varchar(80),
    reason_payload        jsonb,

    created_at            timestamp not null default now(),

    constraint fk_fp_375_serving
        foreign key (serving_id) references fp_374(serving_id)
        on delete cascade
);

create unique index if not exists uq_fp_375_serving_position
    on fp_375(serving_id, position_no);

create unique index if not exists uq_fp_375_serving_store
    on fp_375(serving_id, store_id);

create index if not exists idx_fp_375_request
    on fp_375(request_id);

create index if not exists idx_fp_375_store
    on fp_375(store_id, created_at desc);


-- =========================================================
-- Recommended application-level values
-- =========================================================
-- fp_370.event_type
--   IMPRESSION, CLICK, PLAY_START, PLAY_PROGRESS, PLAY_COMPLETE,
--   SKIP, LIKE, UNLIKE, COMMENT, SHARE, HIDE, NOT_INTERESTED, REPORT
--
-- fp_370.event_source
--   HOME, VIDEO_FEED, SEARCH, PROFILE, SHARE, PUSH, DEEP_LINK
--
-- fp_371.subject_type
--   PLACE, REGION, CREATOR, TAG, CATEGORY, DURATION_BUCKET, TIME_BUCKET
--
-- fp_373.candidate_source
--   PERSONALIZED, NEARBY, POPULAR, RECENT, SIMILAR_PLACE,
--   SIMILAR_CREATOR, EXPLORATION, FALLBACK


-- =========================================================
-- Optional backfill phase
-- Review before running in production.
-- =========================================================
-- insert into fp_370 (
--     username,
--     is_guest,
--     guest_id,
--     store_id,
--     event_type,
--     event_source,
--     server_event_at,
--     metadata
-- )
-- select
--     h.username,
--     coalesce(h.is_guest, false),
--     h.guest_id,
--     h.store_id::integer,
--     'PLAY_START',
--     'LEGACY_FP_303',
--     h.watched_at,
--     jsonb_build_object('legacy_table', 'fp_303', 'legacy_id', h.id)
-- from fp_303 h
-- where h.store_id is not null
--   and h.watched_at is not null;


-- =========================================================
-- Optional FK phase
-- Add only after user_id migration and legacy data quality are verified.
-- =========================================================
-- alter table fp_370
--     add constraint fk_fp_370_user
--     foreign key (user_id) references fp_100(user_id);
--
-- alter table fp_370
--     add constraint fk_fp_370_store
--     foreign key (store_id) references fp_300(store_id);
--
-- alter table fp_371
--     add constraint fk_fp_371_user
--     foreign key (user_id) references fp_100(user_id);
--
-- alter table fp_372
--     add constraint fk_fp_372_store
--     foreign key (store_id) references fp_300(store_id);
--
-- alter table fp_373
--     add constraint fk_fp_373_user
--     foreign key (user_id) references fp_100(user_id);
--
-- alter table fp_373
--     add constraint fk_fp_373_store
--     foreign key (store_id) references fp_300(store_id);
--
-- alter table fp_374
--     add constraint fk_fp_374_user
--     foreign key (user_id) references fp_100(user_id);
--
-- alter table fp_375
--     add constraint fk_fp_375_store
--     foreign key (store_id) references fp_300(store_id);
