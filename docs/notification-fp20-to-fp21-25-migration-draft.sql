-- =========================================================
-- fp_20 -> fp_21 ~ fp_23 migration draft
-- 목적:
-- - 레거시 알림(fp_20)을 새 알림 구조(fp_21, fp_22, fp_23)로 1차 이관
-- - 안전한 이벤트만 우선 이관
-- - 애매한 row는 예외 테이블로 분리
--
-- 전제:
-- 1. fp_21 ~ fp_23 이 먼저 생성되어 있어야 함
-- 2. fp_20 에는 target_type, read_at 컬럼이 있다고 가정
-- 3. 최근 데이터는 receiver_id / sender_id 가 user_id 문자열일 수 있음
-- 4. 과거 데이터는 username 문자열일 수 있음
--
-- 범위:
-- - LIKE
-- - COMMENT
-- - REPLY
-- - FOLLOW
--
-- 보류:
-- - video, Video 등 비표준 type
-- - actor/recipient 변환 실패 row
-- - target 판정 불가 row
-- =========================================================

begin;

-- ---------------------------------------------------------
-- 0. 예외/감사 테이블
-- ---------------------------------------------------------
create table if not exists fp_26 (
    legacy_id             bigint primary key,
    receiver_id           varchar(50),
    sender_id             varchar(50),
    type                  varchar(50),
    reference_id          bigint,
    target_type           varchar(50),
    comment_id            bigint,
    reply_id              bigint,
    message               text,
    created_at            timestamp,
    reject_reason         varchar(200) not null,
    logged_at             timestamp not null default now()
);

create table if not exists fp_27 (
    legacy_id             bigint primary key,
    event_id              bigint not null,
    notification_id       bigint not null,
    migrated_at           timestamp not null default now()
);


-- ---------------------------------------------------------
-- 1. 대상 row 정규화
-- ---------------------------------------------------------
with source_rows as (
    select
        n.id as legacy_id,
        n.receiver_id,
        n.sender_id,
        n.type,
        n.reference_id,
        n.target_type,
        n.comment_id,
        n.reply_id,
        n.message,
        coalesce(n.is_read, false) as is_read,
        n.read_at,
        coalesce(n.created_at, now()) as created_at
    from fp_20 n
    where upper(n.type) in ('LIKE', 'COMMENT', 'REPLY', 'FOLLOW')
      and not exists (
          select 1
          from fp_27 m
          where m.legacy_id = n.id
      )
),
normalized_users as (
    select
        s.*,
        coalesce(
            case
                when s.receiver_id ~ '^[0-9]+$' then cast(s.receiver_id as integer)
            end,
            ur.user_id
        ) as recipient_user_id,
        coalesce(
            case
                when s.sender_id ~ '^[0-9]+$' then cast(s.sender_id as integer)
            end,
            ua.user_id
        ) as actor_user_id
    from source_rows s
    left join fp_100 ur
        on s.receiver_id !~ '^[0-9]+$'
       and ur.username = s.receiver_id
    left join fp_100 ua
        on s.sender_id !~ '^[0-9]+$'
       and ua.username = s.sender_id
),
resolved_type as (
    select
        u.*,
        lower(coalesce(nullif(u.target_type, ''), '')) as normalized_target_type,
        case
            when upper(u.type) = 'COMMENT' then 'VIDEO_COMMENT'
            when upper(u.type) = 'REPLY' then 'VIDEO_REPLY'
            when upper(u.type) = 'FOLLOW' then 'FRIEND_REQUEST'
            when upper(u.type) = 'LIKE' and lower(coalesce(u.target_type, '')) in ('video') then 'VIDEO_LIKE'
            when upper(u.type) = 'LIKE' and lower(coalesce(u.target_type, '')) in ('image', 'image_feed') then 'IMAGE_FEED_LIKE'
            when upper(u.type) = 'LIKE' and exists (
                select 1 from fp_300 v where cast(v.store_id as bigint) = u.reference_id
            ) then 'VIDEO_LIKE'
            when upper(u.type) = 'LIKE' and exists (
                select 1 from fp_400 f where cast(f.feed_no as bigint) = u.reference_id
            ) then 'IMAGE_FEED_LIKE'
            else null
        end as event_type,
        case
            when upper(u.type) = 'COMMENT' then 'comment'
            when upper(u.type) = 'REPLY' then 'reply'
            when upper(u.type) = 'FOLLOW' then 'friend'
            when upper(u.type) = 'LIKE' and lower(coalesce(u.target_type, '')) in ('video') then 'video'
            when upper(u.type) = 'LIKE' and lower(coalesce(u.target_type, '')) in ('image', 'image_feed') then 'image_feed'
            when upper(u.type) = 'LIKE' and exists (
                select 1 from fp_300 v where cast(v.store_id as bigint) = u.reference_id
            ) then 'video'
            when upper(u.type) = 'LIKE' and exists (
                select 1 from fp_400 f where cast(f.feed_no as bigint) = u.reference_id
            ) then 'image_feed'
            else null
        end as object_type,
        case
            when upper(u.type) = 'COMMENT' then 'video_comment'
            when upper(u.type) = 'REPLY' then 'video_reply'
            when upper(u.type) = 'FOLLOW' then 'friend_request'
            when upper(u.type) = 'LIKE' and lower(coalesce(u.target_type, '')) in ('video') then 'video'
            when upper(u.type) = 'LIKE' and lower(coalesce(u.target_type, '')) in ('image', 'image_feed') then 'image_feed'
            when upper(u.type) = 'LIKE' and exists (
                select 1 from fp_300 v where cast(v.store_id as bigint) = u.reference_id
            ) then 'video'
            when upper(u.type) = 'LIKE' and exists (
                select 1 from fp_400 f where cast(f.feed_no as bigint) = u.reference_id
            ) then 'image_feed'
            else null
        end as resolved_target_type
    from normalized_users u
),
classified as (
    select
        r.*,
        case
            when r.recipient_user_id is null then 'RECIPIENT_NOT_RESOLVED'
            when r.actor_user_id is null then 'ACTOR_NOT_RESOLVED'
            when r.event_type is null then 'EVENT_TYPE_NOT_RESOLVED'
            when r.object_type is null then 'OBJECT_TYPE_NOT_RESOLVED'
            when r.reference_id is null and upper(r.type) in ('LIKE', 'COMMENT', 'REPLY') then 'REFERENCE_ID_MISSING'
            when r.resolved_target_type is null then 'TARGET_TYPE_NOT_RESOLVED'
            else null
        end as reject_reason
    from resolved_type r
),
rejected as (
    insert into fp_26 (
        legacy_id,
        receiver_id,
        sender_id,
        type,
        reference_id,
        target_type,
        comment_id,
        reply_id,
        message,
        created_at,
        reject_reason
    )
    select
        c.legacy_id,
        c.receiver_id,
        c.sender_id,
        c.type,
        c.reference_id,
        c.target_type,
        c.comment_id,
        c.reply_id,
        c.message,
        c.created_at,
        c.reject_reason
    from classified c
    where c.reject_reason is not null
    on conflict (legacy_id) do update
    set receiver_id   = excluded.receiver_id,
        sender_id     = excluded.sender_id,
        type          = excluded.type,
        reference_id  = excluded.reference_id,
        target_type   = excluded.target_type,
        comment_id    = excluded.comment_id,
        reply_id      = excluded.reply_id,
        message       = excluded.message,
        created_at    = excluded.created_at,
        reject_reason = excluded.reject_reason,
        logged_at     = now()
    returning legacy_id
),
accepted as (
    select *
    from classified
    where reject_reason is null
),
inserted_events as (
    insert into fp_21 (
        event_type,
        actor_user_id,
        object_type,
        object_id,
        parent_object_type,
        parent_object_id,
        message_template,
        message_params,
        dedupe_key,
        created_at
    )
    select
        a.event_type,
        a.actor_user_id,
        a.object_type,
        a.reference_id,
        case
            when upper(a.type) = 'COMMENT' then 'comment'
            when upper(a.type) = 'REPLY' then 'reply'
            else null
        end as parent_object_type,
        case
            when upper(a.type) = 'COMMENT' then a.comment_id
            when upper(a.type) = 'REPLY' then a.reply_id
            else null
        end as parent_object_id,
        a.message,
        jsonb_build_object(
            'legacyId', a.legacy_id,
            'legacyType', a.type,
            'legacyTargetType', a.target_type
        ),
        'fp20:' || a.legacy_id,
        a.created_at
    from accepted a
    returning event_id, dedupe_key
),
event_mapping as (
    select
        cast(replace(e.dedupe_key, 'fp20:', '') as bigint) as legacy_id,
        e.event_id
    from inserted_events e
),
inserted_recipients as (
    insert into fp_22 (
        event_id,
        recipient_user_id,
        inbox_status,
        is_read,
        read_at,
        is_deleted,
        deleted_at,
        delivered_at,
        created_at
    )
    select
        m.event_id,
        a.recipient_user_id,
        'ACTIVE',
        a.is_read,
        a.read_at,
        false,
        null,
        null,
        a.created_at
    from accepted a
    join event_mapping m
      on m.legacy_id = a.legacy_id
    returning notification_id, event_id
),
inserted_targets as (
    insert into fp_23 (
        event_id,
        target_type,
        target_id,
        target_sub_id,
        deep_link,
        web_path,
        app_route,
        created_at
    )
    select
        m.event_id,
        a.resolved_target_type,
        a.reference_id,
        case
            when upper(a.type) = 'COMMENT' then a.comment_id
            when upper(a.type) = 'REPLY' then a.reply_id
            else null
        end as target_sub_id,
        case
            when a.resolved_target_type = 'video' then 'plate://videos/' || a.reference_id
            when a.resolved_target_type = 'image_feed' then 'plate://image-feeds/' || a.reference_id
            when a.resolved_target_type = 'video_comment' then 'plate://videos/' || a.reference_id || '?commentId=' || a.comment_id
            when a.resolved_target_type = 'video_reply' then 'plate://videos/' || a.reference_id || '?replyId=' || a.reply_id
            when a.resolved_target_type = 'friend_request' then 'plate://friends/requests'
            else null
        end as deep_link,
        case
            when a.resolved_target_type = 'video' then '/videos/' || a.reference_id
            when a.resolved_target_type = 'image_feed' then '/image-feeds/' || a.reference_id
            when a.resolved_target_type = 'video_comment' then '/videos/' || a.reference_id
            when a.resolved_target_type = 'video_reply' then '/videos/' || a.reference_id
            when a.resolved_target_type = 'friend_request' then '/friends/requests'
            else null
        end as web_path,
        case
            when a.resolved_target_type = 'video' then 'VideoDetail'
            when a.resolved_target_type = 'image_feed' then 'ImageFeedDetail'
            when a.resolved_target_type = 'video_comment' then 'VideoDetail'
            when a.resolved_target_type = 'video_reply' then 'VideoDetail'
            when a.resolved_target_type = 'friend_request' then 'FriendRequests'
            else null
        end as app_route,
        a.created_at
    from accepted a
    join event_mapping m
      on m.legacy_id = a.legacy_id
),
final_mapping as (
    insert into fp_27 (
        legacy_id,
        event_id,
        notification_id
    )
    select
        a.legacy_id,
        m.event_id,
        r.notification_id
    from accepted a
    join event_mapping m
      on m.legacy_id = a.legacy_id
    join inserted_recipients r
      on r.event_id = m.event_id
    returning legacy_id
)
select
    (select count(*) from final_mapping) as migrated_count,
    (select count(*) from rejected) as rejected_count;

commit;


-- =========================================================
-- Post-check queries
-- =========================================================

-- 1. 이번 실행으로 옮겨진 legacy id 목록
select *
from fp_27
order by migrated_at desc, legacy_id desc;

-- 2. reject row 확인
select *
from fp_26
order by logged_at desc, legacy_id desc;

-- 3. event / recipient / target 연결 검증
select
    m.legacy_id,
    e.event_id,
    e.event_type,
    e.actor_user_id,
    e.object_type,
    e.object_id,
    r.notification_id,
    r.recipient_user_id,
    r.is_read,
    t.target_type,
    t.target_id,
    t.target_sub_id
from fp_27 m
join fp_21 e
  on e.event_id = m.event_id
join fp_22 r
  on r.notification_id = m.notification_id
left join fp_23 t
  on t.event_id = e.event_id
order by m.migrated_at desc, m.legacy_id desc;

