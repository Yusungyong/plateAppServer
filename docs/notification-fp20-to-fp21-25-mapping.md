# `fp_20` -> `fp_21~25` Mapping Rules

이 문서는 `fp_20` 실데이터 분포를 기준으로 새 알림 구조 `fp_21 ~ fp_25`로 옮길 때 사용하는 매핑 규칙입니다.

핵심 전제:
1. `receiver_id`, `sender_id`는 대부분 `username` 문자열이다.
2. `target_type`은 대부분 비어 있으므로 주 판정 기준이 아니다.
3. `comment_id`, `reply_id`는 `NULL`뿐 아니라 `0`도 "없음"으로 취급해야 한다.
4. `type` 값은 표준화되어 있지 않다.
5. 특히 `feed`, `video`는 한 타입 안에 여러 의미가 섞여 있으므로 `message` 패턴까지 같이 봐야 한다.

---

## 1. 테이블 역할 매핑

| 레거시 | 새 구조 | 설명 |
|---|---|---|
| `fp_20` 1 row | `fp_21` 1 row | 원본 이벤트 |
| `fp_20` 1 row | `fp_22` 1 row | 수신자 inbox |
| `fp_20` 1 row | `fp_23` 0 or 1 row | 클릭 대상 |
| 없음 | `fp_24` | 신규 토큰 운영 |
| 없음 | `fp_25` | 신규 발송 이력 운영 |

1차 이관 대상은 `fp_21`, `fp_22`, `fp_23`이다.

---

## 2. 사용자 식별자 해석 규칙

### 2.1 `receiver_id`

우선순위:
1. 숫자 문자열이면 `fp_100.user_id`
2. 아니면 `fp_100.username -> user_id`
3. 둘 다 실패하면 이관 제외

현재 precheck 결과:
- 대부분 `username`
- 실패 후보: `lazyduck`

즉 `lazyduck` 관련 row는 1차 이관에서 예외 처리하는 것이 맞다.

### 2.2 `sender_id`

우선순위:
1. 숫자 문자열이면 `fp_100.user_id`
2. 아니면 `fp_100.username -> user_id`
3. 실패하면 이관 제외

현재 precheck 결과:
- 변환 실패 후보 없음

---

## 3. 값 정규화 규칙

| 원값 | 정규화 규칙 |
|---|---|
| `comment_id = 0` | `NULL` |
| `reply_id = 0` | `NULL` |
| 빈 `target_type` | `NULL` |
| `type` 오타 `videoCommnent` | `videoComment` 의미로 해석 |
| `type` 오타 `feedCommnent` | `feedComment` 의미로 해석 |

---

## 4. 타입 분류 전략

`fp_20.type`만으로 충분하지 않다.

분류 우선순위:
1. 명확한 전용 타입 직접 매핑
2. `feed`, `video`는 `message` 패턴으로 재분기
3. `createFeed`, `createVideo`는 2차 이관 후보
4. 최근 소수 표준 row(`LIKE`)는 별도 분기

---

## 5. 1차 이관 대상 타입

### 5.1 바로 이관 가능

| `fp_20.type` | 새 `event_type` | `object_type` | `target_type` |
|---|---|---|---|
| `videoLike` | `VIDEO_LIKE` | `video` | `video` |
| `feedLike` | `IMAGE_FEED_LIKE` | `image_feed` | `image_feed` |
| `videoReply` | `VIDEO_REPLY` | `reply` | `video_reply` |
| `videoCommnent` | `VIDEO_COMMENT` | `comment` | `video_comment` |
| `feedComment` | `IMAGE_FEED_COMMENT` | `comment` | `image_feed_comment` |
| `feedCommnent` | `IMAGE_FEED_COMMENT` | `comment` | `image_feed_comment` |
| `mention` | `MENTION` | `comment` | `image_feed_comment` 우선 |

### 5.2 조건부 이관

`feed`, `video`는 `message` 기준으로 분기한다.

#### `type = feed`

| `message` 패턴 | 새 `event_type` | `target_type` |
|---|---|---|
| `%회원님의 게시글을 좋아합니다%` | `IMAGE_FEED_LIKE` | `image_feed` |
| `%회원님의 게시글에 댓글을 작성했습니다%` | `IMAGE_FEED_COMMENT` | `image_feed_comment` |
| `%새로운 피드를 등록했습니다%` | `IMAGE_FEED_CREATED` | `image_feed` |

#### `type = video`

| `message` 패턴 | 새 `event_type` | `target_type` |
|---|---|---|
| `%회원님의 게시글을 좋아합니다%` | `VIDEO_LIKE` | `video` |
| `%회원님의 게시글에 댓글을 작성했습니다%` | `VIDEO_COMMENT` | `video_comment` |
| `%새로운 동영상을 등록했습니다%` | `VIDEO_CREATED` | `video` |

### 5.3 보류

| `fp_20.type` | 이유 |
|---|---|
| `createFeed` | 수신 정책 재확인 필요 |
| `createVideo` | 수신 정책 재확인 필요 |
| `LIKE` | 최근 8건만 존재, `target_type` 혼재 |

---

## 6. `reference_id`, `comment_id`, `reply_id` 매핑 규칙

| 이벤트 | `fp_21.object_id` | `fp_23.target_id` | `fp_23.target_sub_id` |
|---|---|---|---|
| `VIDEO_LIKE` | `reference_id` | `reference_id` | `NULL` |
| `IMAGE_FEED_LIKE` | `reference_id` | `reference_id` | `NULL` |
| `VIDEO_COMMENT` | `reference_id` | `reference_id` | `NULLIF(comment_id, 0)` |
| `VIDEO_REPLY` | `reference_id` | `reference_id` | `NULLIF(reply_id, 0)` |
| `IMAGE_FEED_COMMENT` | `reference_id` | `reference_id` | `NULLIF(comment_id, 0)` |
| `VIDEO_CREATED` | `reference_id` | `reference_id` | `NULL` |
| `IMAGE_FEED_CREATED` | `reference_id` | `reference_id` | `NULL` |
| `MENTION` | `reference_id` | `reference_id` | `NULLIF(comment_id, 0)` 우선 |

---

## 7. `message` 처리 규칙

`fp_20.message`는 1차 이관에서 원문 그대로 보존한다.

권장:
- `fp_21.message_template = fp_20.message`
- `fp_21.message_params = jsonb_build_object(...)`

즉 1차 목표는:
1. 표시 문구 보존
2. 구조 데이터 정리

---

## 8. `fp_22` 상태 매핑

| `fp_20` | `fp_22` |
|---|---|
| `is_read = true` | `is_read = true` |
| `is_read = false` | `is_read = false` |
| `read_at` 존재 | `read_at` 복사 |
| 삭제 개념 없음 | `is_deleted = false` |

---

## 9. 이관 제외 조건

아래 row는 `fp_26` 예외 테이블로 보낸다.

1. `receiver_id`를 `user_id`로 변환할 수 없음
2. `sender_id`를 `user_id`로 변환할 수 없음
3. `type + message`로 `event_type`을 판정할 수 없음
4. `reference_id`가 없거나 target 판정이 불가능
5. `mention`이지만 실제 대상 콘텐츠 성격을 판별할 수 없음

---

## 10. 실제 1차 이관 범위

1차에 포함:
- `videoLike`
- `feedLike`
- `videoReply`
- `videoCommnent`
- `feedComment`
- `feedCommnent`
- `mention`
- `feed` 중 패턴 판정 가능한 row
- `video` 중 패턴 판정 가능한 row

1차 보류:
- `createFeed`
- `createVideo`
- `LIKE`

---

## 11. 결론

현재 실데이터 기준 핵심 규칙은 이겁니다.

1. `receiver_id`, `sender_id`는 username 기반 변환이 주 경로다.
2. `target_type`은 보조 정보일 뿐, 주 판정 기준이 아니다.
3. `feed`, `video`는 `message` 패턴 분기가 필수다.
4. `comment_id`, `reply_id`의 `0`은 `NULL`로 바꿔야 한다.
5. `createFeed`, `createVideo`, `LIKE`는 2차 이관 후보로 분리한다.

이 규칙으로 migration draft를 다시 작성한다.
