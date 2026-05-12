# Notification V2 Migration Runbook

이 문서는 `fp_20` 레거시 알림을 `fp_21 ~ fp_25` 새 구조로 옮기기 전에 확인해야 할 순서와 판정 기준을 정리한 실행 문서입니다.

---

## 1. 실행 순서

1. [notification-v2-fp21-25-ddl.sql](/c:/workspace/plate-main/docs/notification-v2-fp21-25-ddl.sql)
2. [notification-fp20-precheck.sql](/c:/workspace/plate-main/docs/notification-fp20-precheck.sql)
3. precheck 결과 검토
4. [notification-fp20-to-fp21-25-migration-draft.sql](/c:/workspace/plate-main/docs/notification-fp20-to-fp21-25-migration-draft.sql)
5. post-check 결과 검토

권장:
- 운영 DB 직행 전에 스테이징/백업 DB에서 1회 검증

---

## 2. DDL 먼저 적용

목적:
- `fp_21 ~ fp_25`
- 예외/이관이력용 `fp_26`, `fp_27`
를 받을 구조를 먼저 만든다.

실행:
- [notification-v2-fp21-25-ddl.sql](/c:/workspace/plate-main/docs/notification-v2-fp21-25-ddl.sql)

확인:
- `fp_21`, `fp_22`, `fp_23`, `fp_24`, `fp_25` 생성 여부

---

## 3. Precheck 실행

실행:
- [notification-fp20-precheck.sql](/c:/workspace/plate-main/docs/notification-fp20-precheck.sql)

반드시 확인할 쿼리:
1. `type` 분포
2. `target_type` 분포
3. `receiver_id`, `sender_id` 숫자/문자 분포
4. `LIKE`의 `reference_id`가 `fp_300` / `fp_400` 어디에 매칭되는지
5. `COMMENT`, `REPLY`의 `reference_id`가 어디에 매칭되는지
6. 비표준 type 샘플

---

## 4. Precheck 판정 기준

### 4.1 바로 진행 가능한 조건

아래 조건이면 migration draft를 거의 그대로 써도 된다.

1. `type`이 대부분 `LIKE`, `COMMENT`, `REPLY`, `FOLLOW`
2. `receiver_id`가 대부분 숫자형이거나 `fp_100.username`으로 정상 변환 가능
3. `sender_id`도 대부분 숫자형이거나 `fp_100.username`으로 정상 변환 가능
4. `LIKE.reference_id`가 거의 명확하게 `fp_300` 또는 `fp_400` 한쪽으로 판정 가능
5. `COMMENT`, `REPLY`도 `reference_id`가 명확하게 콘텐츠와 매칭됨

### 4.2 보정이 필요한 조건

아래면 migration draft 수정 후 실행한다.

1. `FOLLOW`가 사실 친구수락까지 섞여 있음
2. `COMMENT`, `REPLY` 중 이미지 피드 댓글 알림이 섞여 있음
3. `LIKE`에서 `target_type` 없이 `reference_id` 충돌 케이스가 많음
4. `sender_id`, `receiver_id` 미해결 row가 많은 편
5. `video`, `Video` 같은 비표준 type가 생각보다 많음

---

## 5. Migration 실행 전 체크리스트

1. `fp_20` 백업 완료
2. `fp_21 ~ fp_27` 생성 완료
3. precheck 결과 저장 완료
4. `LIKE`, `COMMENT`, `REPLY`, `FOLLOW`의 의미 합의 완료
5. 예외 row를 `fp_26`에 보내는 정책 확인

---

## 6. Migration 실행

실행:
- [notification-fp20-to-fp21-25-migration-draft.sql](/c:/workspace/plate-main/docs/notification-fp20-to-fp21-25-migration-draft.sql)

현재 draft 성격:
- 1차 이관 전용
- `fp_24`, `fp_25`는 데이터 이관 대상이 아니라 신규 운영 테이블
- 실제 이관 대상은 `fp_21`, `fp_22`, `fp_23`

---

## 7. Post-check 판정 기준

### 7.1 정상

1. `fp_27`에 이관된 legacy id가 기록됨
2. `fp_26` 예외 row 수가 예상 범위 안
3. `fp_21.event_type`, `fp_22.recipient_user_id`, `fp_23.target_type/target_id`가 샘플 검증에서 맞음

### 7.2 중단

아래면 운영 반영 중단 후 SQL 보정:

1. `recipient_user_id`가 잘못 매핑됨
2. `LIKE`가 video/image_feed로 잘못 갈림
3. `COMMENT`, `REPLY`의 `target_sub_id`가 잘못 들어감
4. 예외 row가 과도하게 많음

---

## 8. 운영 적용 방식 권장

1. `fp_20`은 당분간 유지
2. 새 코드는 dual-write 또는 v2-only로 전환
3. 조회 API는 샘플 검증 후 `fp_22` 기반으로 변경
4. 일정 기간 뒤 `fp_20` read-only 처리

즉:
- 바로 폐기하지 말고
- 병행 운영 후 전환

---

## 9. 지금 바로 필요한 것

다음 순서로 진행하면 된다.

1. [notification-fp20-precheck.sql](/c:/workspace/plate-main/docs/notification-fp20-precheck.sql) 실행
2. 결과를 기준으로 migration draft 보정
3. 보정된 SQL로 이관 실행

현재 단계에서 가장 중요한 건:
- **실데이터 분포 확인 없이 migration draft를 바로 운영에 넣지 않는 것**

