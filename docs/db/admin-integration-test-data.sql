-- Run only in an isolated integration-test database.
-- The negative IDs make cleanup deterministic and avoid production-shaped identifiers.

insert into service_feedback
    (id, type, content, status, contact, created_at, updated_at, version)
values
    (-91001, 'bug', '관리자 통합 테스트 의견', 'received', null, now(), now(), 0)
on conflict (id) do nothing;

insert into content_verifications
    (id, target_type, target_id, status, review_reason, created_at, updated_at, version)
values
    (-92001, 'FEED', '-93001', 'pending', '관리자 통합 테스트 검수', now(), now(), 0)
on conflict (id) do nothing;

insert into seasonal_curations
    (id, title, description, status, display_order, created_by, updated_by, created_at, updated_at, version)
select -94001, '통합 테스트 큐레이션', '삭제 가능한 검증 데이터', 'DRAFT', 9999,
       u.user_id, u.user_id, now(), now(), 0
from fp_100 u
where upper(coalesce(u.role, '')) in ('ADMIN', 'SUPER_ADMIN')
order by u.user_id
limit 1
on conflict (id) do nothing;

-- Cleanup (run after the scenario)
-- delete from content_verification_history where verification_id = -92001;
-- delete from content_verifications where id = -92001;
-- delete from service_feedback where id = -91001;
-- delete from seasonal_curations where id = -94001;
-- delete from admin_audit_logs where resource_id in ('-91001', '-92001', '-94001');

