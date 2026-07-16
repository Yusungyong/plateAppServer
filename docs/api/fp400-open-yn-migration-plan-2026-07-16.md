# fp_400 이미지 공개 상태 migration 계획

- 상태: `Draft / Not Applied`
- 작성일: 2026-07-16
- 대상 파일: `V13__add_image_feed_visibility.sql`
- 운영·공유 DB 실행 여부: 실행하지 않음

## 목적

이미지 업로드 요청에 이미 존재하던 `openYn`을 실제 저장하고, 마이페이지 허브가 타인의 이미지 visibility를 추정하지 않도록 `fp_400.open_yn` 원천을 추가한다.

## 변경 범위

- nullable `char(1)` 컬럼 `open_yn` 추가
- 값이 존재할 때 `Y` 또는 `N`만 허용하는 `NOT VALID` check constraint 추가
- default, `NOT NULL`, 기존 행 backfill은 추가하지 않음
- 기존 행은 모두 `NULL`로 남겨 정책 미확정 상태를 보존

업로드에서 `openYn`이 명시되면 정규화한 `Y/N`을 저장한다. 구버전 요청처럼 값이 누락되면 DB에는 `NULL`을 저장하되 기존 성공 응답 호환을 위해 응답의 `openYn`은 계속 `Y`로 반환한다. 이 호환 동작은 legacy 행을 공개로 승인했다는 의미가 아니며 허브의 타인 visibility에서는 `NULL`을 제외한다.

## 적용 전 필수 확인

1. `fp_400` 전체 건수와 현재 모든 이미지 write/read path 목록
2. Android/iOS 각 배포 버전이 `openYn`을 보내는지와 실제 값 분포
3. 관리자, 홈, 그룹, 주변, 상세 API에서 `N` 및 계정 privacy를 우회하지 않는 공통 정책
4. S3 이미지 object 자체의 공개 여부와 URL 공유 위험
5. migration 수행 시간, lock 관측, 백업 및 복구 담당자
6. `MY_HUB_IMAGE_VISIBILITY_READY=false` 유지 확인

## 적용 절차

1. 배포 창에서 대상 DB와 Flyway history를 확인한다.
2. 백업 또는 시점 복구 가능 상태를 확인한다.
3. V13을 단독 적용하고 Flyway 성공 여부와 컬럼·constraint 존재를 확인한다.
4. 기존 행의 `open_yn IS NULL` 건수가 전체 기존 행 수와 일치하는지 확인한다.
5. 신규 테스트 계정이 아닌 승인된 검증 절차로 명시 `Y/N` write를 각각 확인한다.
6. 기존 앱·관리자 API 회귀와 `N` 우회 경로가 없음을 확인한다.
7. 정책 승인 전에는 backfill, default, `NOT NULL`, constraint validation을 수행하지 않는다.

## rollback

애플리케이션 배포 전 migration만 적용된 경우에 한해, 영향 확인 후 constraint와 컬럼 제거를 검토할 수 있다. 애플리케이션이 `open_yn` 값을 쓰기 시작한 뒤에는 컬럼 삭제가 데이터 손실이므로 즉시 drop하지 않고 애플리케이션 rollback 후 저장 건수와 복구 필요성을 먼저 판단한다.

## 활성화 조건

다음 조건을 모두 만족한 후에만 `MY_HUB_IMAGE_VISIBILITY_READY=true`를 검토한다.

- legacy null 정책 승인
- 기존 이미지 read path 전체의 공통 visibility 적용
- `Y/N/NULL`, 소유자·친구·비친구·guest, 양방향 차단·신고 테스트 통과
- PostgreSQL 실행 계획과 응답 시간 기준 통과
- 모바일 최소 버전 및 rollback 조건 확정
