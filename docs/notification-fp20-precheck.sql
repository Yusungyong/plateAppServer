-- =========================================================
-- fp_20 precheck SQL
-- 목적:
-- - 실제 데이터 분포 확인
-- - migration draft 실행 전 애매한 케이스 파악
-- =========================================================

-- 1. type 분포
select
    type,
    count(*) as cnt
from fp_20
group by type
order by cnt desc, type;


-- 2. target_type 분포
select
    coalesce(target_type, '(null)') as target_type,
    count(*) as cnt
from fp_20
group by target_type
order by cnt desc, target_type;


-- 3. type + target_type 조합 분포
select
    type,
    coalesce(target_type, '(null)') as target_type,
    count(*) as cnt
from fp_20
group by type, target_type
order by cnt desc, type, target_type;


-- 4. receiver_id 숫자/비숫자 분포
select
    case
        when receiver_id ~ '^[0-9]+$' then 'NUMERIC_USER_ID'
        else 'NON_NUMERIC_POSSIBLY_USERNAME'
    end as receiver_id_kind,
    count(*) as cnt
from fp_20
group by 1
order by cnt desc;


-- 5. sender_id 숫자/비숫자/null 분포
select
    case
        when sender_id is null or btrim(sender_id) = '' then 'NULL_OR_BLANK'
        when sender_id ~ '^[0-9]+$' then 'NUMERIC_USER_ID'
        else 'NON_NUMERIC_POSSIBLY_USERNAME'
    end as sender_id_kind,
    count(*) as cnt
from fp_20
group by 1
order by cnt desc;


-- 6. receiver_id -> fp_100.user_id 변환 실패 후보
select
    n.receiver_id,
    count(*) as cnt
from fp_20 n
left join fp_100 u
    on n.receiver_id !~ '^[0-9]+$'
   and u.username = n.receiver_id
where not (
        n.receiver_id ~ '^[0-9]+$'
        or u.user_id is not null
    )
group by n.receiver_id
order by cnt desc, n.receiver_id;


-- 7. sender_id -> fp_100.user_id 변환 실패 후보
select
    n.sender_id,
    count(*) as cnt
from fp_20 n
left join fp_100 u
    on n.sender_id is not null
   and n.sender_id !~ '^[0-9]+$'
   and u.username = n.sender_id
where n.sender_id is not null
  and btrim(n.sender_id) <> ''
  and not (
        n.sender_id ~ '^[0-9]+$'
        or u.user_id is not null
    )
group by n.sender_id
order by cnt desc, n.sender_id;


-- 8. LIKE 의 reference_id 가 video/image_feed 어디에 매칭되는지
select
    case
        when exists (select 1 from fp_300 v where cast(v.store_id as bigint) = n.reference_id)
         and exists (select 1 from fp_400 f where cast(f.feed_no as bigint) = n.reference_id)
            then 'MATCH_BOTH_VIDEO_AND_IMAGE'
        when exists (select 1 from fp_300 v where cast(v.store_id as bigint) = n.reference_id)
            then 'MATCH_VIDEO_ONLY'
        when exists (select 1 from fp_400 f where cast(f.feed_no as bigint) = n.reference_id)
            then 'MATCH_IMAGE_ONLY'
        else 'MATCH_NONE'
    end as like_reference_resolution,
    count(*) as cnt
from fp_20 n
where upper(n.type) = 'LIKE'
group by 1
order by cnt desc;


-- 9. COMMENT / REPLY 의 reference_id 매칭 확인
select
    upper(n.type) as type,
    case
        when exists (select 1 from fp_300 v where cast(v.store_id as bigint) = n.reference_id)
            then 'MATCH_VIDEO'
        when exists (select 1 from fp_400 f where cast(f.feed_no as bigint) = n.reference_id)
            then 'MATCH_IMAGE_FEED'
        else 'MATCH_NONE'
    end as reference_resolution,
    count(*) as cnt
from fp_20 n
where upper(n.type) in ('COMMENT', 'REPLY')
group by upper(n.type), 2
order by upper(n.type), cnt desc;


-- 10. reference_id 누락 분포
select
    type,
    count(*) as cnt
from fp_20
where reference_id is null
group by type
order by cnt desc, type;


-- 11. comment_id / reply_id 분포
select
    upper(type) as type,
    count(*) filter (where comment_id is not null) as comment_id_present,
    count(*) filter (where reply_id is not null) as reply_id_present,
    count(*) as total_cnt
from fp_20
group by upper(type)
order by total_cnt desc, upper(type);


-- 12. 비표준 type 샘플
select *
from fp_20
where upper(type) not in ('LIKE', 'COMMENT', 'REPLY', 'FOLLOW')
order by created_at desc nulls last, id desc
limit 100;


-- 13. 1차 이관 대상 예상 건수
select
    count(*) as candidate_cnt
from fp_20
where upper(type) in ('LIKE', 'COMMENT', 'REPLY', 'FOLLOW');


-- 14. 1차 이관 제외 예상 건수
select
    count(*) as non_candidate_cnt
from fp_20
where upper(type) not in ('LIKE', 'COMMENT', 'REPLY', 'FOLLOW');

