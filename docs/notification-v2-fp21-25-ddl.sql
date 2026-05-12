-- =========================================================
-- Notification V2 DDL
-- - Legacy table `fp_20` is kept as-is.
-- - New notification system uses `fp_21` ~ `fp_25`.
-- - PostgreSQL 기준
-- =========================================================

-- =========================================================
-- fp_21: notification event source
-- 이벤트 원본 저장
-- =========================================================
create table if not exists fp_21 (
    event_id              bigserial primary key,
    event_type            varchar(50) not null,
    actor_user_id         integer not null,
    object_type           varchar(50) not null,
    object_id             bigint not null,
    parent_object_type    varchar(50),
    parent_object_id      bigint,
    message_template      varchar(100),
    message_params        jsonb,
    dedupe_key            varchar(200),
    created_at            timestamp not null default now()
);

create index if not exists idx_fp_21_actor_user_id
    on fp_21(actor_user_id);

create index if not exists idx_fp_21_object
    on fp_21(object_type, object_id);

create unique index if not exists uq_fp_21_dedupe_key
    on fp_21(dedupe_key)
    where dedupe_key is not null;


-- =========================================================
-- fp_22: notification recipient inbox
-- 실제 사용자별 수신함
-- event 1건 -> recipient 여러 건 가능
-- =========================================================
create table if not exists fp_22 (
    notification_id       bigserial primary key,
    event_id              bigint not null,
    recipient_user_id     integer not null,
    inbox_status          varchar(20) not null default 'ACTIVE',
    is_read               boolean not null default false,
    read_at               timestamp,
    is_deleted            boolean not null default false,
    deleted_at            timestamp,
    delivered_at          timestamp,
    created_at            timestamp not null default now(),
    constraint fk_fp_22_event
        foreign key (event_id) references fp_21(event_id)
        on delete cascade
);

create index if not exists idx_fp_22_recipient_created
    on fp_22(recipient_user_id, created_at desc);

create index if not exists idx_fp_22_recipient_read
    on fp_22(recipient_user_id, is_read, is_deleted);

create unique index if not exists uq_fp_22_event_recipient
    on fp_22(event_id, recipient_user_id);


-- =========================================================
-- fp_23: notification target
-- 클릭 이동 / 딥링크 대상
-- event 와 1:1
-- =========================================================
create table if not exists fp_23 (
    event_id              bigint primary key,
    target_type           varchar(50) not null,
    target_id             bigint not null,
    target_sub_id         bigint,
    deep_link             varchar(500),
    web_path              varchar(500),
    app_route             varchar(200),
    created_at            timestamp not null default now(),
    constraint fk_fp_23_event
        foreign key (event_id) references fp_21(event_id)
        on delete cascade
);

create index if not exists idx_fp_23_target
    on fp_23(target_type, target_id);


-- =========================================================
-- fp_24: user push token
-- 기기별 푸시 토큰 저장
-- 기존 fp_100.fcm_token 대체용
-- =========================================================
create table if not exists fp_24 (
    token_id              bigserial primary key,
    user_id               integer not null,
    device_id             varchar(200) not null,
    platform              varchar(20) not null,
    push_token            varchar(500) not null,
    token_status          varchar(20) not null default 'ACTIVE',
    last_seen_at          timestamp,
    created_at            timestamp not null default now(),
    updated_at            timestamp not null default now()
);

create unique index if not exists uq_fp_24_user_device
    on fp_24(user_id, device_id);

create unique index if not exists uq_fp_24_push_token
    on fp_24(push_token);

create index if not exists idx_fp_24_user_status
    on fp_24(user_id, token_status);


-- =========================================================
-- fp_25: push delivery log
-- 실제 푸시 발송 이력
-- =========================================================
create table if not exists fp_25 (
    delivery_id           bigserial primary key,
    notification_id       bigint not null,
    token_id              bigint not null,
    provider              varchar(20) not null,
    provider_message_id   varchar(200),
    delivery_status       varchar(20) not null,
    error_code            varchar(100),
    error_message         text,
    attempted_at          timestamp not null default now(),
    constraint fk_fp_25_notification
        foreign key (notification_id) references fp_22(notification_id)
        on delete cascade,
    constraint fk_fp_25_token
        foreign key (token_id) references fp_24(token_id)
        on delete cascade
);

create index if not exists idx_fp_25_notification
    on fp_25(notification_id);

create index if not exists idx_fp_25_token
    on fp_25(token_id);

create index if not exists idx_fp_25_attempted_at
    on fp_25(attempted_at desc);


-- =========================================================
-- Recommended enum values (application-level standard)
-- =========================================================
-- fp_21.event_type
--   VIDEO_LIKE
--   IMAGE_FEED_LIKE
--   VIDEO_COMMENT
--   VIDEO_REPLY
--   FRIEND_REQUEST
--   FRIEND_ACCEPTED
--   SYSTEM_NOTICE
--
-- fp_21.object_type
--   video
--   image_feed
--   comment
--   reply
--   friend
--   notice
--
-- fp_23.target_type
--   video
--   image_feed
--   video_comment
--   video_reply
--   friend_request
--   notice
--
-- fp_24.token_status
--   ACTIVE
--   INVALID
--   DISABLED
--
-- fp_22.inbox_status
--   ACTIVE
--   HIDDEN
--   ARCHIVED
--
-- fp_25.delivery_status
--   SUCCESS
--   FAILED
--   SKIPPED


-- =========================================================
-- Optional FK phase
-- 주의:
-- - 현재 프로젝트는 FK가 약한 편이라 즉시 강제하지 않는 편이 안전함.
-- - user_id 마이그레이션이 안정화되면 아래 FK 추가 검토.
-- =========================================================
-- alter table fp_21
--     add constraint fk_fp_21_actor_user
--     foreign key (actor_user_id) references fp_100(user_id);
--
-- alter table fp_22
--     add constraint fk_fp_22_recipient_user
--     foreign key (recipient_user_id) references fp_100(user_id);
--
-- alter table fp_24
--     add constraint fk_fp_24_user
--     foreign key (user_id) references fp_100(user_id);

