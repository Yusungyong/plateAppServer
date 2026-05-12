-- =========================================================
-- fp_20 -> fp_21 ~ fp_23 migration draft (revised)
-- 실데이터 기준 재작성
--
-- 1차 포함:
-- - videoLike
-- - feedLike
-- - videoReply
-- - videoCommnent
-- - feedComment
-- - feedCommnent
-- - mention
-- - feed / video 중 message 패턴 판정 가능 row
--
-- 1차 보류:
-- - createFeed
-- - createVideo
-- - LIKE
--
-- 주의:
-- - receiver_id, sender_id 는 대부분 username 이다
-- - comment_id, reply_id 의 0은 NULL로 취급해야 한다
-- =========================================================

begin;

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

with source_rows as (
    select
        n.id as legacy_id,
        n.receiver_id,
        n.sender_id,
        n.type,
        n.reference_id,
        n.target_type,
        nullif(n.comment_id, 0) as comment_id,
        nullif(n.reply_id, 0) as reply_id,
        n.message,
        coalesce(n.is_read, false) as is_read,
        n.read_at,
        coalesce(n.created_at, now()) as created_at
    from fp_20 n
    where n.type in (
        'videoLike',
        'feedLike',
        'videoReply',
        'videoCommnent',
        'feedComment',
        'feedCommnent',
        'mention',
        'feed',
        'video'
    )
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
            case when s.receiver_id ~ '^[0-9]+$' then cast(s.receiver_id as integer) end,
            ur.user_id
        ) as recipient_user_id,
        coalesce(
            case when s.sender_id ~ '^[0-9]+$' then cast(s.sender_id as integer) end,
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
resolved as (
    select
        u.*,
        case
            when u.type = 'videoLike' then 'VIDEO_LIKE'
            when u.type = 'feedLike' then 'IMAGE_FEED_LIKE'
            when u.type = 'videoReply' then 'VIDEO_REPLY'
            when u.type = 'videoCommnent' then 'VIDEO_COMMENT'
            when u.type in ('feedComment', 'feedCommnent') then 'IMAGE_FEED_COMMENT'
            when u.type = 'mention' then 'MENTION'
            when u.type = 'feed' and u.message like '%회원님의 게시글을 좋아합니다%' then 'IMAGE_FEED_LIKE'
            when u.type = 'feed' and u.message like '%회원님의 게시글에 댓글을 작성했습니다%' then 'IMAGE_FEED_COMMENT'
            when u.type = 'feed' and u.message like '%새로운 피드를 등록했습니다%' then 'IMAGE_FEED_CREATED'
            when u.type = 'video' and u.message like '%회원님의 게시글을 좋아합니다%' then 'VIDEO_LIKE'
            when u.type = 'video' and u.message like '%회원님의 게시글에 댓글을 작성했습니다%' then 'VIDEO_COMMENT'
            when u.type = 'video' and u.message like '%새로운 동영상을 등록했습니다%' then 'VIDEO_CREATED'
            else null
        end as event_type,
        case
            when u.type = 'videoLike' then 'video'
            when u.type = 'feedLike' then 'image_feed'
            when u.type = 'videoReply' then 'reply'
            when u.type = 'videoCommnent' then 'comment'
            when u.type in ('feedComment', 'feedCommnent') then 'comment'
            when u.type = 'mention' then 'comment'
            when u.type = 'feed' and u.message like '%회원님의 게시글을 좋아합니다%' then 'image_feed'
            when u.type = 'feed' and u.message like '%회원님의 게시글에 댓글을 작성했습니다%' then 'comment'
            when u.type = 'feed' and u.message like '%새로운 피드를 등록했습니다%' then 'image_feed'
            when u.type = 'video' and u.message like '%회원님의 게시글을 좋아합니다%' then 'video'
            when u.type = 'video' and u.message like '%회원님의 게시글에 댓글을 작성했습니다%' then 'comment'
            when u.type = 'video' and u.message like '%새로운 동영상을 등록했습니다%' then 'video'
            else null
        end as object_type,
        case
            when u.type = 'videoLike' then 'video'
            when u.type = 'feedLike' then 'image_feed'
            when u.type = 'videoReply' then 'video_reply'
            when u.type = 'videoCommnent' then 'video_comment'
            when u.type in ('feedComment', 'feedCommnent') then 'image_feed_comment'
            when u.type = 'mention' then 'image_feed_comment'
            when u.type = 'feed' and u.message like '%회원님의 게시글을 좋아합니다%' then 'image_feed'
            when u.type = 'feed' and u.message like '%회원님의 게시글에 댓글을 작성했습니다%' then 'image_feed_comment'
            when u.type = 'feed' and u.message like '%새로운 피드를 등록했습니다%' then 'image_feed'
            when u.type = 'video' and u.message like '%회원님의 게시글을 좋아합니다%' then 'video'
            when u.type = 'video' and u.message like '%회원님의 게시글에 댓글을 작성했습니다%' then 'video_comment'
            when u.type = 'video' and u.message like '%새로운 동영상을 등록했습니다%' then 'video'
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
            when r.reference_id is null then 'REFERENCE_ID_MISSING'
            when r.resolved_target_type is null then 'TARGET_TYPE_NOT_RESOLVED'
            else null
        end as reject_reason
    from resolved r
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
            when a.event_type in ('VIDEO_COMMENT', 'IMAGE_FEED_COMMENT', 'MENTION') then 'comment'
            when a.event_type = 'VIDEO_REPLY' then 'reply'
            else null
        end,
        case
            when a.event_type in ('VIDEO_COMMENT', 'IMAGE_FEED_COMMENT', 'MENTION') then a.comment_id
            when a.event_type = 'VIDEO_REPLY' then a.reply_id
            else null
        end,
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
        cast(replace(dedupe_key, 'fp20:', '') as bigint) as legacy_id,
        event_id
    from inserted_events
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
            when a.event_type in ('VIDEO_COMMENT', 'IMAGE_FEED_COMMENT', 'MENTION') then a.comment_id
            when a.event_type = 'VIDEO_REPLY' then a.reply_id
            else null
        end,
        case
            when a.resolved_target_type = 'video' then 'plate://videos/' || a.reference_id
            when a.resolved_target_type = 'image_feed' then 'plate://image-feeds/' || a.reference_id
            when a.resolved_target_type = 'video_comment' then 'plate://videos/' || a.reference_id || '?commentId=' || a.comment_id
            when a.resolved_target_type = 'video_reply' then 'plate://videos/' || a.reference_id || '?replyId=' || a.reply_id
            when a.resolved_target_type = 'image_feed_comment' then 'plate://image-feeds/' || a.reference_id || '?commentId=' || a.comment_id
            else null
        end,
        case
            when a.resolved_target_type = 'video' then '/videos/' || a.reference_id
            when a.resolved_target_type = 'image_feed' then '/image-feeds/' || a.reference_id
            when a.resolved_target_type = 'video_comment' then '/videos/' || a.reference_id
            when a.resolved_target_type = 'video_reply' then '/videos/' || a.reference_id
            when a.resolved_target_type = 'image_feed_comment' then '/image-feeds/' || a.reference_id
            else null
        end,
        case
            when a.resolved_target_type = 'video' then 'VideoDetail'
            when a.resolved_target_type = 'image_feed' then 'ImageFeedDetail'
            when a.resolved_target_type = 'video_comment' then 'VideoDetail'
            when a.resolved_target_type = 'video_reply' then 'VideoDetail'
            when a.resolved_target_type = 'image_feed_comment' then 'ImageFeedDetail'
            else null
        end,
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

select *
from fp_27
order by migrated_at desc, legacy_id desc;

select *
from fp_26
order by logged_at desc, legacy_id desc;

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
