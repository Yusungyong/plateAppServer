# PlateApp 백엔드/DB용 스키마 문서 (Draft)

이 문서는 **백엔드 개발/DB 정비(FK/인덱스/제약)와 운영 디버깅**을 위한 레퍼런스입니다.

---

# Tables

## public.fp_100
**설명**: 회원 마스터: 계정/프로필/권한/FCM 토큰/비공개 설정 등 사용자 기본 정보

### Columns
```text
public	fp_100	1	username	character varying(20)	false		로그인 아이디(유니크 권장)
public	fp_100	2	password	character varying(255)	true		비밀번호(해시 저장). 소셜 로그인만 사용 시 NULL 가능
public	fp_100	3	email	character varying(50)	true		이메일
public	fp_100	4	phone	character varying(12)	true		전화번호
public	fp_100	5	role	character varying(3)	true		권한/역할
public	fp_100	6	created_at	date	true		가입일(생성일)
public	fp_100	7	updated_at	date	true		수정일
public	fp_100	8	active_region	character varying(255)	true		활동 지역(문자열/코드). fp_005 또는 지역 텍스트와 연동 가능
public	fp_100	9	profile_image_url	character varying(255)	true		프로필 이미지 경로/URL
public	fp_100	10	nick_name	character varying(255)	true		닉네임
public	fp_100	11	code	character varying(6)	true		회원 상태/구분 코드(공통코드와 연동 가능)
public	fp_100	12	fcm_token	text	true		푸시 알림용 FCM 토큰
public	fp_100	13	is_private	boolean	true	false	비공개 계정 여부(true=비공개)
public	fp_100	14	user_id	integer	false	nextval('fp_100_user_id_seq'::regclass)	회원 내부 ID (PK)
```

### Constraints
```text
public	fp_100	PRIMARY KEY	fp_100_pkey	user_id	public	fp_100	user_id
public	fp_100	UNIQUE	fp_100_username_uk	username	public	fp_100	username
```

### Indexes
```text
public	fp_100	fp_100_pkey	CREATE UNIQUE INDEX fp_100_pkey ON public.fp_100 USING btree (user_id)
public	fp_100	fp_100_username_uk	CREATE UNIQUE INDEX fp_100_username_uk ON public.fp_100 USING btree (username)
public	fp_100	ix_users_lower_username	CREATE INDEX ix_users_lower_username ON public.fp_100 USING btree (lower((username)::text))
```

### TODO / Notes
-

---

## public.fp_103
**설명**: 리프레시 토큰 관리: 사용자/디바이스 단위로 refresh_token과 만료(expiry_date) 저장

### Columns
```text
public	fp_103	1	id	integer	false	nextval('fp_103_id_seq'::regclass)	토큰 레코드 ID (PK)
public	fp_103	2	username	character varying(255)	false		토큰 소유자 username
public	fp_103	3	refresh_token	text	false		리프레시 토큰 값
public	fp_103	4	expiry_date	timestamp with time zone	false		만료 시각(타임존 포함)
public	fp_103	5	device_id	character varying(255)	true		디바이스 식별자(선택)
public	fp_103	6	created_at	timestamp with time zone	true	CURRENT_TIMESTAMP	발급/저장 시각
```

### Constraints
```text
public	fp_103	PRIMARY KEY	fp_103_pkey	id	public	fp_103	id
public	fp_103	UNIQUE	uq_fp103_username_device	username	public	fp_103	username
public	fp_103	UNIQUE	uq_fp103_username_device	device_id	public	fp_103	device_id
public	fp_103	UNIQUE	uq_username_device	username	public	fp_103	device_id
public	fp_103	UNIQUE	uq_username_device	device_id	public	fp_103	device_id
```

### Indexes
```text
public	fp_103	fp_103_pkey	CREATE UNIQUE INDEX fp_103_pkey ON public.fp_103 USING btree (id)
public	fp_103	fp_103_username_idx	CREATE INDEX fp_103_username_idx ON public.fp_103 USING btree (username)
public	fp_103	uq_fp103_username_device	CREATE UNIQUE INDEX uq_fp103_username_device ON public.fp_103 USING btree (username, device_id)
public	fp_103	uq_username_device	CREATE UNIQUE INDEX uq_username_device ON public.fp_103 USING btree (username, device_id)
```

### TODO / Notes
-

---

## public.fp_105
**설명**: 로그인 이력: 로그인 시도 결과(성공/실패), IP/디바이스/OS/앱버전 등 메타 기록

### Columns
```text
public	fp_105	1	login_id	bigint	false	nextval('member_login_history_login_id_seq'::regclass)	로그인 이력 ID (PK)
public	fp_105	2	username	character varying(100)	false		대상 username
public	fp_105	3	login_datetime	timestamp without time zone	true	CURRENT_TIMESTAMP	로그인 시각
public	fp_105	4	ip_address	character varying(45)	true		접속 IP
public	fp_105	5	login_status	character varying(10)	false		로그인 상태(성공/실패 등)
public	fp_105	6	fail_reason	text	true		실패 사유(실패인 경우)
public	fp_105	7	device_model	character varying(100)	true		디바이스 모델
public	fp_105	8	os	character varying(20)	true		OS 종류(iOS/Android 등)
public	fp_105	9	os_version	character varying(20)	true		OS 버전
public	fp_105	10	app_version	character varying(20)	true		앱 버전
public	fp_105	11	device_id	character varying(100)	true		디바이스 ID(식별자)
public	fp_105	12	created_at	timestamp without time zone	true	CURRENT_TIMESTAMP	레코드 생성 시각
```

### Constraints
```text
public	fp_105	PRIMARY KEY	member_login_history_pkey	login_id	public	fp_105	login_id
```

### Indexes
```text
public	fp_105	idx_login_device	CREATE INDEX idx_login_device ON public.fp_105 USING btree (device_id)
public	fp_105	idx_login_ip	CREATE INDEX idx_login_ip ON public.fp_105 USING btree (ip_address)
public	fp_105	member_login_history_pkey	CREATE UNIQUE INDEX member_login_history_pkey ON public.fp_105 USING btree (login_id)
```

### TODO / Notes
-

---

## public.fp_120
**설명**: 이메일 인증 코드: 이메일 인증 코드 발급/검증/만료 관리

### Columns
```text
public	fp_120	1	id	integer	false	nextval('fp_120_id_seq'::regclass)	이메일 인증 레코드 ID (PK)
public	fp_120	2	email	character varying(255)	false		대상 이메일
public	fp_120	3	verification_code	character varying(6)	false		인증 코드
public	fp_120	4	is_verified	boolean	true	false	인증 완료 여부
public	fp_120	5	created_at	timestamp without time zone	true	CURRENT_TIMESTAMP	코드 발급 시각
public	fp_120	6	verified_at	timestamp without time zone	true		인증 완료 시각
public	fp_120	7	expires_at	timestamp without time zone	true	(CURRENT_TIMESTAMP + '00:10:00'::interval)	만료 시각
```

### Constraints
```text
public	fp_120	PRIMARY KEY	fp_120_pkey	id	public	fp_120	id
public	fp_120	UNIQUE	fp_120_email_key	email	public	fp_120	email
```

### Indexes
```text
public	fp_120	fp_120_email_key	CREATE UNIQUE INDEX fp_120_email_key ON public.fp_120 USING btree (email)
public	fp_120	fp_120_pkey	CREATE UNIQUE INDEX fp_120_pkey ON public.fp_120 USING btree (id)
```

### TODO / Notes
-

---

## public.fp_101
**설명**: 회원 정보 변경 이력: before/after 스냅샷과 변경 타입(change_tp)을 기록

### Columns
```text
public	fp_101	1	username	character varying(255)	false		대상 회원 username
public	fp_101	2	history_id	bigint	false	nextval('fp_101_history_id_seq'::regclass)	변경 이력 ID (PK)
public	fp_101	3	before_ex	text	true		변경 전 정보(텍스트/JSON 문자열 등)
public	fp_101	4	after_ex	text	true		변경 후 정보(텍스트/JSON 문자열 등)
public	fp_101	5	change_tp	character varying(50)	false		변경 유형/구분
public	fp_101	6	created_dt	timestamp without time zone	true	CURRENT_TIMESTAMP	기록 생성 시각
```

### Constraints
```text
public	fp_101	PRIMARY KEY	fp_101_pkey	history_id	public	fp_101	history_id
```

### Indexes
```text
public	fp_101	fp_101_pkey	CREATE UNIQUE INDEX fp_101_pkey ON public.fp_101 USING btree (history_id)
public	fp_101	idx_fp_101_username	CREATE INDEX idx_fp_101_username ON public.fp_101 USING btree (username)
```

### TODO / Notes
-

---

## public.fp_110
**설명**: 소셜로그인 매핑: user_id와 provider/provider_user_id를 연결하여 소셜 계정을 연동

### Columns
```text
public	fp_110	1	social_id	integer	false	nextval('fp_110_social_id_seq'::regclass)	소셜 매핑 ID (PK)
public	fp_110	2	user_id	integer	false		회원 ID (fp_100.user_id 참조)
public	fp_110	3	provider	character varying(30)	false		소셜 프로바이더(apple/kakao/google/naver 등)
public	fp_110	4	provider_user_id	character varying(255)	false		프로바이더에서 내려주는 유저 식별자
public	fp_110	5	email	character varying(255)	true		소셜 계정 이메일(선택)
public	fp_110	6	display_name	character varying(255)	true		소셜 표시 이름(선택)
public	fp_110	7	created_at	timestamp with time zone	true	now()	연동 생성 시각
```

### Constraints
```text
public	fp_110	FOREIGN KEY	fk_fp110_user	user_id	public	fp_100	user_id
public	fp_110	PRIMARY KEY	fp_110_pkey	social_id	public	fp_110	social_id
```

### Indexes
```text
public	fp_110	fp_110_pkey	CREATE UNIQUE INDEX fp_110_pkey ON public.fp_110 USING btree (social_id)
public	fp_110	uq_fp110_provider_user	CREATE UNIQUE INDEX uq_fp110_provider_user ON public.fp_110 USING btree (provider, provider_user_id)
```

### TODO / Notes
-

---

## public.fp_150
**설명**: 친구관리: 친구 요청/수락 상태(pending/accepted 등) 및 메시지 기록

### Columns
```text
public	fp_150	1	id	integer	false	nextval('fp_150_id_seq'::regclass)	친구 관계/요청 ID (PK)
public	fp_150	2	username	character varying(20)	false		대상(기준) 사용자 username
public	fp_150	3	friend_name	character varying(20)	false		상대 사용자 username
public	fp_150	4	status	character varying(20)	false	'pending'::character varying	상태(pending/accepted/rejected 등)
public	fp_150	5	created_at	timestamp without time zone	true	CURRENT_TIMESTAMP	요청 생성 시각
public	fp_150	6	updated_at	timestamp without time zone	true	CURRENT_TIMESTAMP	상태/메시지 수정 시각
public	fp_150	7	initiator_username	character varying(20)	true		요청 시작자 username(누가 신청했는지)
public	fp_150	8	message	text	true		요청 메시지(선택)
public	fp_150	9	accepted_at	timestamp without time zone	true		수락 시각(수락된 경우)
```

### Constraints
```text
public	fp_150	PRIMARY KEY	fp_150_pkey	id	public	fp_150	id
public	fp_150	UNIQUE	fp_150_requester_receiver_key	username	public	fp_150	friend_name
```

### Indexes
```text
public	fp_150	fp_150_pkey	CREATE UNIQUE INDEX fp_150_pkey ON public.fp_150 USING btree (id)
public	fp_150	fp_150_requester_receiver_key	CREATE UNIQUE INDEX fp_150_requester_receiver_key ON public.fp_150 USING btree (username, friend_name)
```

### TODO / Notes
-

---

## public.fp_160
**설명**: 차단 관계: blocker가 blocked를 차단한 기록

### Columns
```text
public	fp_160	1	id	integer	false	nextval('fp_160_id_seq'::regclass)	차단 레코드 ID (PK)
public	fp_160	2	blocker_username	character varying(50)	false		차단한 사용자 username
public	fp_160	3	blocked_username	character varying(50)	false		차단된 사용자 username
public	fp_160	4	blocked_at	timestamp without time zone	true	CURRENT_TIMESTAMP	차단 시각
```

### Constraints
```text
public	fp_160	PRIMARY KEY	fp_160_pkey	id	public	fp_160	id
```

### Indexes
```text
public	fp_160	fp_160_pkey	CREATE UNIQUE INDEX fp_160_pkey ON public.fp_160 USING btree (id)
public	fp_160	idx_fp_160_blocked	CREATE INDEX idx_fp_160_blocked ON public.fp_160 USING btree (blocked_username)
public	fp_160	idx_fp_160_blocker	CREATE INDEX idx_fp_160_blocker ON public.fp_160 USING btree (blocker_username)
```

### TODO / Notes
-

---

## public.fp_200
**설명**: 친구랑 방문한 곳: 방문 날짜/가게/메모를 기록(연관 feed_id 저장 가능)

### Columns
```text
public	fp_200	1	id	integer	false	nextval('fp_200_id_seq'::regclass)	방문 기록 ID (PK)
public	fp_200	2	username	character varying(100)	false		기록 작성자 username
public	fp_200	3	friend_name	character varying(100)	false		함께 방문한 친구 username
public	fp_200	4	store_id	integer	true		가게/스토어 ID(선택)
public	fp_200	5	store_name	character varying(255)	false		가게명
public	fp_200	6	memo	text	true		메모
public	fp_200	7	visit_date	date	false	CURRENT_DATE	방문 날짜
public	fp_200	8	created_at	timestamp without time zone	true	CURRENT_TIMESTAMP	생성 시각
public	fp_200	9	updated_at	timestamp without time zone	true	CURRENT_TIMESTAMP	수정 시각
public	fp_200	10	address	character varying(255)	true		주소(선택)
public	fp_200	11	feed_id	bigint	false	0	연관 이미지 피드 ID(선택/기본 0)
```

### Constraints
```text
public	fp_200	PRIMARY KEY	fp_200_pkey	id	public	fp_200	id
```

### Indexes
```text
public	fp_200	fp_200_pkey	CREATE UNIQUE INDEX fp_200_pkey ON public.fp_200 USING btree (id)
public	fp_200	idx_fp200_address	CREATE INDEX idx_fp200_address ON public.fp_200 USING btree (address)
public	fp_200	idx_fp200_store_name	CREATE INDEX idx_fp200_store_name ON public.fp_200 USING btree (store_name)
```

### TODO / Notes
-

---

## public.fp_20
**설명**: 알림: 수신자(receiver)에게 전달되는 인앱 알림(타입/메시지/참조ID, 읽음 여부 포함)

### Columns
```text
public	fp_20	1	id	bigint	false	nextval('fp_20_id_seq'::regclass)	알림 ID (PK)
public	fp_20	2	receiver_id	character varying(50)	false		수신자 식별자(username 또는 user_id 문자열)
public	fp_20	3	sender_id	character varying(50)	true		발신자 식별자(username 또는 user_id 문자열, 선택)
public	fp_20	4	type	character varying(50)	false		알림 타입(예: like, comment, reply, follow 등)
public	fp_20	5	reference_id	bigint	true		타입별 참조 ID(예: feed_id/store_id/gb_id 등)
public	fp_20	6	message	text	false		알림 메시지
public	fp_20	7	is_read	boolean	true	false	읽음 여부
public	fp_20	8	created_at	timestamp without time zone	true	CURRENT_TIMESTAMP	생성 시각
public	fp_20	9	comment_id	bigint	true		연관 댓글 ID(선택)
public	fp_20	10	reply_id	bigint	true		연관 대댓글 ID(선택)
```

### Constraints
```text
public	fp_20	PRIMARY KEY	fp_20_pkey	id	public	fp_20	id
```

### Indexes
```text
public	fp_20	fp_20_pkey	CREATE UNIQUE INDEX fp_20_pkey ON public.fp_20 USING btree (id)
public	fp_20	idx_fp_20_is_read	CREATE INDEX idx_fp_20_is_read ON public.fp_20 USING btree (is_read)
public	fp_20	idx_fp_20_receiver_id	CREATE INDEX idx_fp_20_receiver_id ON public.fp_20 USING btree (receiver_id)
```

### TODO / Notes
-

---

## public.fp_310
**설명**: 장소/지오코딩 정보: place_id 기반 주소/위경도/타입을 저장하며 use_yn/deleted_at로 소프트 상태 관리

### Columns
```text
public	fp_310	1	id	integer	false	nextval('fp_310_id_seq'::regclass)	장소 레코드 ID (PK)
public	fp_310	2	formatted_address	character varying(255)	true		포맷된 전체 주소
public	fp_310	3	latitude	double precision	true		위도
public	fp_310	4	longitude	double precision	true		경도
public	fp_310	5	place_id	character varying(255)	true		외부 장소 식별자(예: Google place_id)
public	fp_310	6	types	character varying(255)[]	true		장소 타입 배열(예: restaurant 등)
public	fp_310	7	street_number	character varying(50)	true		번지
public	fp_310	8	route	character varying(255)	true		도로명
public	fp_310	9	locality	character varying(255)	true		도시/구역(locality)
public	fp_310	10	administrative_area_level_1	character varying(255)	true		광역 지역(시/도)
public	fp_310	11	administrative_area_level_2	character varying(255)	true		기초 지역(구/군)
public	fp_310	12	country	character varying(255)	true		국가
public	fp_310	13	postal_code	character varying(50)	true		우편번호
public	fp_310	15	use_yn	character(1)	false	'N'::bpchar	사용 여부(Y/N). N이면 미사용/삭제 상태일 수 있음
public	fp_310	16	deleted_at	date	true		삭제 처리 날짜(소프트 삭제)
```

### Constraints
```text
public	fp_310	PRIMARY KEY	fp_310_pkey	id	public	fp_310	id
```

### Indexes
```text
public	fp_310	fp_310_pkey	CREATE UNIQUE INDEX fp_310_pkey ON public.fp_310 USING btree (id)
public	fp_310	idx_fp_310_placeid	CREATE INDEX idx_fp_310_placeid ON public.fp_310 USING btree (place_id)
```

### TODO / Notes
-

---

## public.fp_005
**설명**: 사용자 활동지역 매핑(지역 마스터): region_name을 depth/parent_id로 계층(트리) 구성

### Columns
```text
public	fp_005	1	id	integer	false	nextval('fp_005_id_seq'::regclass)	지역 ID (PK)
public	fp_005	2	region_name	character varying(100)	false		지역명
public	fp_005	3	depth	smallint	false		지역 깊이(레벨). 예: 1=시/도, 2=구/군 등
public	fp_005	4	parent_id	integer	true		상위 지역 ID (fp_005.id 참조, 최상위는 NULL)
```

### Constraints
```text
public	fp_005	FOREIGN KEY	fk_parent	parent_id	public	fp_005	id
public	fp_005	PRIMARY KEY	fp_005_pkey	id	public	fp_005	id
```

### Indexes
```text
public	fp_005	fp_005_pkey	CREATE UNIQUE INDEX fp_005_pkey ON public.fp_005 USING btree (id)
```

### TODO / Notes
-

---

## public.fp_300
**설명**: 비디오 콘텐츠: 동영상 파일/썸네일, 장소(place_id)/주소/가게명, 공개(open_yn) 및 사용(use_yn)/삭제(deleted_at) 상태 포함

### Columns
```text
public	fp_300	1	store_id	integer	false		동영상(스토어) ID (PK 또는 도메인 키)
public	fp_300	2	title	character varying(255)	true		동영상 제목(선택)
public	fp_300	3	file_name	character varying(255)	true		동영상 파일명/키
public	fp_300	4	address	character varying(255)	true		주소(선택)
public	fp_300	5	username	character varying(20)	false		업로드 사용자 username
public	fp_300	9	updated_at	date	false	CURRENT_TIMESTAMP	수정일/업데이트 시각
public	fp_300	10	created_at	date	false	CURRENT_TIMESTAMP	생성일/등록 시각
public	fp_300	11	thumbnail	character varying(255)	true		썸네일 파일명/키(선택)
public	fp_300	14	store_name	character varying(50)	true		가게명(선택)
public	fp_300	16	open_yn	character(1)	true	'Y'::bpchar	공개 여부(Y/N). Y=공개
public	fp_300	19	use_yn	character(1)	false	'Y'::bpchar	사용 여부(Y/N). Y=사용, N=비활성/삭제
public	fp_300	20	deleted_at	date	true		삭제 처리 날짜(소프트 삭제)
public	fp_300	22	place_id	character varying(255)	true		장소 ID(place_id, fp_310.place_id와 연동 가능)
public	fp_300	23	video_duration	integer	true		영상 길이(초 단위 등 프로젝트 규칙)
public	fp_300	24	mute_yn	character varying(1)	true	'N'::character varying	기본 음소거 여부(Y/N)
public	fp_300	25	video_size	numeric(10,2)	true		영상 파일 크기(바이트/KB 등 프로젝트 규칙)
```

### Constraints
```text
public	fp_300	PRIMARY KEY	fp_300_pkey	store_id	public	fp_300	store_id
```

### Indexes
```text
public	fp_300	fp_300_pkey	CREATE UNIQUE INDEX fp_300_pkey ON public.fp_300 USING btree (store_id)
public	fp_300	idx_fp300_address	CREATE INDEX idx_fp300_address ON public.fp_300 USING btree (address)
public	fp_300	idx_fp300_store_name	CREATE INDEX idx_fp300_store_name ON public.fp_300 USING btree (store_name)
public	fp_300	idx_fp_300_openyn_placeid	CREATE INDEX idx_fp_300_openyn_placeid ON public.fp_300 USING btree (open_yn, place_id)
```

### TODO / Notes
-

---

## public.fp_301
**설명**: 비디오 정보 변경이력: store_id 대상 변경(before/after)과 변경 타입을 기록

### Columns
```text
public	fp_301	1	change_hist_seq	bigint	false	nextval('change_hist_seq'::regclass)	변경 이력 시퀀스 (PK)
public	fp_301	2	store_id	bigint	false		대상 동영상(스토어) ID
public	fp_301	3	change_name	character varying(255)	false		변경 항목명(예: title, open_yn 등)
public	fp_301	4	change_tp	character varying(50)	false		변경 타입/구분
public	fp_301	5	before_ex	text	true		변경 전 내용
public	fp_301	6	after_ex	text	true		변경 후 내용
public	fp_301	7	created_at	timestamp without time zone	true	CURRENT_TIMESTAMP	기록 생성 시각
```

### Constraints
```text
public	fp_301	PRIMARY KEY	fp_301_pkey	change_hist_seq	public	fp_301	change_hist_seq
```

### Indexes
```text
public	fp_301	fp_301_pkey	CREATE UNIQUE INDEX fp_301_pkey ON public.fp_301 USING btree (change_hist_seq)
```

### TODO / Notes
-

---

## public.fp_303
**설명**: 비디오 썸네일 시청이력: 사용자/게스트가 특정 동영상(store_id)을 조회/노출한 기록

### Columns
```text
public	fp_303	1	id	bigint	false	nextval('fp_303_id_seq'::regclass)	시청 이력 ID (PK)
public	fp_303	2	username	character varying(50)	false		사용자 username(선택: 게스트면 NULL 가능)
public	fp_303	3	store_id	bigint	false		대상 동영상(스토어) ID
public	fp_303	4	watched_at	timestamp without time zone	false	CURRENT_TIMESTAMP	조회/시청 시각
public	fp_303	5	guest_id	character varying(100)	true		게스트 식별자(선택)
public	fp_303	6	is_guest	boolean	false	false	게스트 여부(true=게스트)
```

### Constraints
```text
public	fp_303	PRIMARY KEY	fp_303_pkey	id	public	fp_303	id
```

### Indexes
```text
public	fp_303	fp_303_pkey	CREATE UNIQUE INDEX fp_303_pkey ON public.fp_303 USING btree (id)
```

### TODO / Notes
-

---

## public.fp_305
**설명**: 비디오 시청이력: 재생 세션 기반 시청 로그(재생시간/디바이스/품질/완료여부 등)

### Columns
```text
public	fp_305	1	id	integer	false	nextval('fp_305_id_seq'::regclass)	시청 로그 ID (PK)
public	fp_305	2	username	character varying(20)	false		사용자 username
public	fp_305	3	store_id	integer	false		대상 동영상(스토어) ID
public	fp_305	4	timestamp	timestamp with time zone	true	CURRENT_TIMESTAMP	로그 기록 시각(세션 타임스탬프)
public	fp_305	5	duration_watched	integer	true		시청한 시간(초 등 프로젝트 규칙)
public	fp_305	6	device_info	character varying(255)	true		디바이스 정보(선택)
public	fp_305	7	ip_address	character varying(45)	true		접속 IP(선택)
public	fp_305	8	session_id	character varying(255)	true		세션 식별자(선택)
public	fp_305	9	video_quality	character varying(10)	true		재생 품질(선택)
public	fp_305	10	completion_status	boolean	true		완료 시청 여부(선택)
public	fp_305	11	comments	text	true		비고/메모(선택)
public	fp_305	12	use_yn	character(1)	false	'N'::bpchar	사용 여부(Y/N). N이면 비활성/삭제 상태일 수 있음
public	fp_305	13	deleted_at	date	true		삭제 처리 날짜(소프트 삭제)
public	fp_305	14	user_id	integer	true		사용자 user_id(선택)
```

### Constraints
```text
public	fp_305	FOREIGN KEY	fp_305_store_id_fkey	store_id	public	fp_300	store_id
public	fp_305	FOREIGN KEY	fp_305_userid_fk	user_id	public	fp_100	user_id
public	fp_305	PRIMARY KEY	fp_305_pkey	id	public	fp_305	id
```

### Indexes
```text
public	fp_305	fp_305_pkey	CREATE UNIQUE INDEX fp_305_pkey ON public.fp_305 USING btree (id)
```

### TODO / Notes
-

---

## public.fp_50
**설명**: 동영상(비디오/스토어) 좋아요: 사용자가 특정 동영상(store_id)에 좋아요를 누른 기록. use_yn/deleted_at로 소프트 삭제/해제 처리.

### Columns
```text
public	fp_50	1	username	character varying(50)	false		좋아요를 누른 사용자 아이디(username, fp_100.username 참조)
public	fp_50	2	store_id	integer	false		좋아요 대상 동영상(스토어) ID (fp_300.store_id 참조)
public	fp_50	3	use_yn	character(1)	false	'N'::bpchar	사용 여부(Y/N). Y=좋아요 활성, N=좋아요 해제/비활성(소프트 상태)
public	fp_50	4	deleted_at	date	true		좋아요 해제/삭제 처리 시각(소프트 삭제용). NULL이면 활성일 수 있음
public	fp_50	5	created_at	timestamp without time zone	true		좋아요 생성 시각(최초 좋아요 시점)
public	fp_50	6	updated_at	timestamp without time zone	true		좋아요 상태 변경 시각(예: 해제 후 재좋아요 등)
```

### Constraints
```text
public	fp_50	PRIMARY KEY	fp_50_pkey	username	public	fp_50	store_id
```

### Indexes
```text
public	fp_50	fp_50_pkey	CREATE UNIQUE INDEX fp_50_pkey ON public.fp_50 USING btree (username, store_id)
public	fp_50	idx_fp_50_store_id	CREATE INDEX idx_fp_50_store_id ON public.fp_50 USING btree (store_id)
public	fp_50	idx_fp_50_store_like_y	CREATE INDEX idx_fp_50_store_like_y ON public.fp_50 USING btree (store_id) WHERE (use_yn = 'Y'::bpchar)
public	fp_50	idx_fp_50_storeid_username_useyn	CREATE INDEX idx_fp_50_storeid_username_useyn ON public.fp_50 USING btree (store_id, username, use_yn)
public	fp_50	idx_fp_50_username	CREATE INDEX idx_fp_50_username ON public.fp_50 USING btree (username)
```

### TODO / Notes
-

---

## public.fp_440
**설명**: 동영상(스토어) 댓글: 특정 동영상(store_id)에 사용자가 작성한 댓글. use_yn/deleted_at로 소프트 삭제 처리.

### Columns
```text
public	fp_440	1	comment_id	integer	false	nextval('fp_440_comment_id_seq'::regclass)	동영상 댓글 ID (PK)
public	fp_440	2	store_id	integer	false		댓글 대상 동영상(스토어) ID (fp_300.store_id 참조)
public	fp_440	3	username	character varying(255)	false		댓글 작성자 username (fp_100.username 참조)
public	fp_440	4	content	text	false		댓글 내용
public	fp_440	5	created_at	timestamp without time zone	true	CURRENT_TIMESTAMP	댓글 생성 시각
public	fp_440	6	updated_at	timestamp without time zone	true	CURRENT_TIMESTAMP	댓글 수정 시각
public	fp_440	8	use_yn	character varying(1)	false	'Y'::bpchar	사용 여부(Y/N). Y=노출, N=숨김/삭제(소프트)
public	fp_440	9	deleted_at	date	true		삭제 처리 시각(소프트 삭제)
public	fp_440	10	user_id	integer	true		댓글 작성자 user_id (fp_100.user_id 참조, 선택값일 수 있음)
```

### Constraints
```text
public	fp_440	FOREIGN KEY	fp_440_store_id_fkey	store_id	public	fp_300	store_id
public	fp_440	FOREIGN KEY	fp_440_userid_fk	user_id	public	fp_100	user_id
public	fp_440	PRIMARY KEY	fp_440_pkey	comment_id	public	fp_440	comment_id
```

### Indexes
```text
public	fp_440	fp_440_pkey	CREATE UNIQUE INDEX fp_440_pkey ON public.fp_440 USING btree (comment_id)
```

### TODO / Notes
-

---

## public.fp_450
**설명**: 동영상(스토어) 대댓글: fp_440 댓글(comment_id)에 달리는 답글. use_yn/deleted_at로 소프트 삭제 처리.

### Columns
```text
public	fp_450	1	reply_id	integer	false	nextval('fp_450_reply_id_seq'::regclass)	동영상 대댓글 ID (PK)
public	fp_450	2	content	text	false		대댓글 내용
public	fp_450	3	username	character varying(50)	false		대댓글 작성자 username (fp_100.username 참조)
public	fp_450	4	comment_id	integer	false		부모 댓글 ID (fp_440.comment_id 참조)
public	fp_450	5	created_at	timestamp without time zone	true	CURRENT_TIMESTAMP	대댓글 생성 시각
public	fp_450	6	updated_at	timestamp without time zone	true	CURRENT_TIMESTAMP	대댓글 수정 시각
public	fp_450	7	use_yn	character varying(1)	true	'Y'::bpchar	사용 여부(Y/N). Y=노출, N=숨김/삭제(소프트)
public	fp_450	8	deleted_at	timestamp without time zone	true		삭제 처리 시각(소프트 삭제)
```

### Constraints
```text
public	fp_450	FOREIGN KEY	fk_comment	comment_id	public	fp_440	comment_id
public	fp_450	PRIMARY KEY	fp_450_pkey	reply_id	public	fp_450	reply_id
```

### Indexes
```text
public	fp_450	fp_450_pkey	CREATE UNIQUE INDEX fp_450_pkey ON public.fp_450 USING btree (reply_id)
```

### TODO / Notes
-

---

## public.fp_320
**설명**: 메뉴 정보: 가게/장소 기반 메뉴 항목(가격/설명/이미지, feed_id/place_id 연동 가능)

### Columns
```text
public	fp_320	1	item_id	character varying	false	nextval('fp_320_item_id_seq'::regclass)	메뉴 항목 ID (PK)
public	fp_320	2	store_id	integer	true		가게/스토어 ID(선택)
public	fp_320	3	item_name	character varying(255)	false		메뉴명
public	fp_320	4	price	character varying(7)	false		가격(문자열: 통화/표기 포함 가능)
public	fp_320	5	description	text	true		설명(선택)
public	fp_320	6	menu_image	character varying(255)	true		메뉴 이미지 파일/URL(선택)
public	fp_320	7	created_at	timestamp without time zone	true	CURRENT_TIMESTAMP	생성 시각
public	fp_320	8	updated_at	timestamp without time zone	true	CURRENT_TIMESTAMP	수정 시각
public	fp_320	9	use_yn	character(1)	false	'N'::bpchar	사용 여부(Y/N). N이면 비활성/삭제 상태일 수 있음
public	fp_320	10	deleted_at	date	true		삭제 처리 날짜(소프트 삭제)
public	fp_320	11	feed_id	integer	true		연관 이미지 피드 ID(선택)
public	fp_320	12	menu_title	character varying(255)	true		메뉴 타이틀/그룹명(선택)
public	fp_320	13	place_id	character varying(255)	true		연관 장소 ID(place_id, 선택)
public	fp_320	14	store_name	character varying(255)	true		가게명(선택)
```

### Constraints
```text
public	fp_320	FOREIGN KEY	menu_items_store_id_fkey	store_id	public	fp_300	store_id
public	fp_320	PRIMARY KEY	menu_items_pkey	item_id	public	fp_320	item_id
```

### Indexes
```text
public	fp_320	menu_items_pkey	CREATE UNIQUE INDEX menu_items_pkey ON public.fp_320 USING btree (item_id)
```

### TODO / Notes
- [TODO] `item_id`가 varchar인데 sequence default로 보임. 실제 타입/기본값 재확인.

---

## public.fp_400
**설명**: 이미지 피드 게시물: 다중 이미지(images), 장소(place_id)/가게명, 썸네일 포함. use_yn으로 노출/비노출 관리

### Columns
```text
public	fp_400	1	feed_no	integer	false	nextval('fp_400_feed_no_seq'::regclass)	이미지 피드 ID (PK)
public	fp_400	2	username	character varying(50)	false		작성자 username
public	fp_400	3	content	text	false		본문 내용
public	fp_400	4	images	text	true		이미지 목록(텍스트: JSON/CSV 등 프로젝트 규칙)
public	fp_400	5	created_at	timestamp without time zone	true	CURRENT_TIMESTAMP	생성 시각
public	fp_400	6	updated_at	timestamp without time zone	true	CURRENT_TIMESTAMP	수정 시각
public	fp_400	7	feed_title	character varying(255)	true		피드 제목(선택)
public	fp_400	8	location	character varying(50)	true		위치 텍스트(선택)
public	fp_400	9	store_name	character varying(50)	true		가게명(선택)
public	fp_400	10	place_id	character varying(255)	true		장소 ID(place_id, fp_310.place_id와 연동 가능)
public	fp_400	11	use_yn	character varying(1)	false	'Y'::character varying	사용 여부(Y/N). Y=노출, N=숨김/삭제
public	fp_400	12	thumbnail	character varying(255)	true		대표 썸네일 이미지(선택)
```

### Constraints
```text
public	fp_400	PRIMARY KEY	fp_400_pkey	feed_no	public	fp_400	feed_no
```

### Indexes
```text
public	fp_400	fp_400_pkey	CREATE UNIQUE INDEX fp_400_pkey ON public.fp_400 USING btree (feed_no)
```

### TODO / Notes
-

---

## public.fp_401
**설명**: 이미지 피드 변경 이력: feed_id 대상 변경 코드(change_code)와 before/after JSON 스냅샷 기록

### Columns
```text
public	fp_401	1	history_id	bigint	false	nextval('fp_401_history_id_seq'::regclass)	변경 이력 ID (PK)
public	fp_401	2	feed_id	bigint	false		대상 이미지 피드 ID (fp_400.feed_no 참조)
public	fp_401	3	username	text	false		변경 수행자 username
public	fp_401	4	change_code	smallint	false		변경 코드(구분값)
public	fp_401	5	before_info	jsonb	false		변경 전 정보(JSONB)
public	fp_401	6	after_info	jsonb	false		변경 후 정보(JSONB)
public	fp_401	7	ip_addr	inet	true		변경 요청 IP(선택)
public	fp_401	8	created_at	timestamp with time zone	false	now()	기록 생성 시각(타임존 포함)
```

### Constraints
```text
public	fp_401	PRIMARY KEY	fp_401_pkey	history_id	public	fp_401	history_id
```

### Indexes
```text
public	fp_401	fp_401_pkey	CREATE UNIQUE INDEX fp_401_pkey ON public.fp_401 USING btree (history_id)
public	fp_401	idx_fp_401_created_at	CREATE INDEX idx_fp_401_created_at ON public.fp_401 USING btree (created_at DESC)
public	fp_401	idx_fp_401_feed_created	CREATE INDEX idx_fp_401_feed_created ON public.fp_401 USING btree (feed_id, created_at DESC)
public	fp_401	idx_fp_401_feed_id	CREATE INDEX idx_fp_401_feed_id ON public.fp_401 USING btree (feed_id)
```

### TODO / Notes
-

---

## public.fp_60
**설명**: 이미지 피드 좋아요: 사용자가 특정 이미지 피드(feed_id)에 좋아요를 누른 기록. use_yn/deleted_at로 소프트 삭제/해제 처리.

### Columns
```text
public	fp_60	1	username	character varying(255)	false		좋아요를 누른 사용자 아이디(username, fp_100.username 참조)
public	fp_60	2	feed_id	integer	false	nextval('fp_60_feed_id_seq'::regclass)	좋아요 대상 이미지 피드 ID (fp_400.feed_no 참조)
public	fp_60	3	use_yn	character(1)	false		사용 여부(Y/N). Y=좋아요 활성, N=좋아요 해제/비활성(소프트 상태)
public	fp_60	4	deleted_at	timestamp without time zone	true		좋아요 해제/삭제 처리 시각(소프트 삭제용). NULL이면 활성일 수 있음
public	fp_60	5	created_at	timestamp without time zone	false	CURRENT_TIMESTAMP	좋아요 생성 시각(최초 좋아요 시점)
public	fp_60	6	updated_at	timestamp without time zone	false	CURRENT_TIMESTAMP	좋아요 상태 변경 시각(예: 해제 후 재좋아요 등)
```

### Constraints
```text
public	fp_60	PRIMARY KEY	fp_60_pkey	feed_id	public	fp_60	username
```

### Indexes
```text
public	fp_60	fp_60_pkey	CREATE UNIQUE INDEX fp_60_pkey ON public.fp_60 USING btree (feed_id, username)
public	fp_60	uq_fp_60_like	CREATE UNIQUE INDEX uq_fp_60_like ON public.fp_60 USING btree (username, feed_id)
```

### TODO / Notes
- [TODO] `feed_id`가 시퀀스 default를 가짐(대상 ID면 default 불필요/위험).

---

## public.fp_460
**설명**: 이미지 피드 댓글: 특정 이미지 피드(feed_id)에 사용자가 작성한 댓글. use_yn/deleted_at로 소프트 삭제 처리.

### Columns
```text
public	fp_460	1	comment_id	integer	false	nextval('fp_460_comment_id_seq'::regclass)	이미지 댓글 ID (PK)
public	fp_460	2	feed_id	integer	false		댓글 대상 이미지 피드 ID (fp_400.feed_no 참조)
public	fp_460	3	username	character varying(255)	false		댓글 작성자 username (fp_100.username 참조)
public	fp_460	4	content	text	false		댓글 내용
public	fp_460	5	use_yn	character(1)	false	'Y'::bpchar	사용 여부(Y/N). Y=노출, N=숨김/삭제(소프트)
public	fp_460	6	deleted_at	timestamp without time zone	true		삭제 처리 시각(소프트 삭제)
public	fp_460	7	created_at	timestamp without time zone	false	CURRENT_TIMESTAMP	댓글 생성 시각
public	fp_460	8	updated_at	timestamp without time zone	false	CURRENT_TIMESTAMP	댓글 수정 시각
```

### Constraints
```text
public	fp_460	PRIMARY KEY	fp_460_pkey	comment_id	public	fp_460	comment_id
```

### Indexes
```text
public	fp_460	fp_460_pkey	CREATE UNIQUE INDEX fp_460_pkey ON public.fp_460 USING btree (comment_id)
```

### TODO / Notes
-

---

## public.fp_470
**설명**: 이미지 피드 대댓글: fp_460 댓글(comment_id)에 달리는 답글. use_yn/deleted_at로 소프트 삭제 처리.

### Columns
```text
public	fp_470	1	reply_id	integer	false	nextval('fp_470_reply_id_seq'::regclass)	이미지 대댓글 ID (PK)
public	fp_470	2	comment_id	integer	false		부모 댓글 ID (fp_460.comment_id 참조)
public	fp_470	3	username	character varying(255)	false		대댓글 작성자 username (fp_100.username 참조)
public	fp_470	4	content	text	false		대댓글 내용
public	fp_470	5	use_yn	character(1)	false	'Y'::bpchar	사용 여부(Y/N). Y=노출, N=숨김/삭제(소프트)
public	fp_470	6	deleted_at	timestamp without time zone	true		삭제 처리 시각(소프트 삭제)
public	fp_470	7	created_at	timestamp without time zone	false	CURRENT_TIMESTAMP	대댓글 생성 시각
public	fp_470	8	updated_at	timestamp without time zone	false	CURRENT_TIMESTAMP	대댓글 수정 시각
```

### Constraints
```text
public	fp_470	FOREIGN KEY	fk_fp_470	comment_id	public	fp_460	comment_id
public	fp_470	PRIMARY KEY	fp_470_pkey	reply_id	public	fp_470	reply_id
```

### Indexes
```text
public	fp_470	fp_470_pkey	CREATE UNIQUE INDEX fp_470_pkey ON public.fp_470 USING btree (reply_id)
```

### TODO / Notes
-

---

## public.fp_350
**설명**: 태그 저장: 가게(store_id) 또는 이미지 피드(feed_id)에 연결된 태그 문자열을 저장

### Columns
```text
public	fp_350	1	id	integer	false	nextval('fp_350_id_seq'::regclass)	태그 레코드 ID (PK)
public	fp_350	2	store_id	integer	true	0	연관 가게/스토어 ID(선택)
public	fp_350	3	feed_id	integer	true	0	연관 이미지 피드 ID(선택)
public	fp_350	4	tags	character varying(255)	false		태그 문자열(구분자 규칙은 프로젝트 정책)
public	fp_350	5	created_at	timestamp without time zone	false	now()	생성 시각
public	fp_350	6	updated_at	timestamp without time zone	false	now()	수정 시각
```

### Constraints
```text
public	fp_350	PRIMARY KEY	fp_350_pkey	id	public	fp_350	id
```

### Indexes
```text
public	fp_350	fp_350_pkey	CREATE UNIQUE INDEX fp_350_pkey ON public.fp_350 USING btree (id)
```

### TODO / Notes
-

---

## public.fp_360
**설명**: 방명록: 가게/피드/장소 기반 방명록 글(공개범위 visibility, 이미지 JSON, 숨김 is_hidden, 작성 메타 포함)

### Columns
```text
public	fp_360	1	gb_id	bigint	false	nextval('fp_360_gb_id_seq'::regclass)	방명록 ID (PK)
public	fp_360	2	store_id	bigint	true		가게/스토어 ID(선택)
public	fp_360	3	feed_id	bigint	false		연관 이미지 피드 ID
public	fp_360	4	username	character varying(50)	false		작성자 username
public	fp_360	5	content	text	false		내용
public	fp_360	6	custom_images	jsonb	false	'[]'::jsonb	첨부 이미지 목록(JSONB)
public	fp_360	7	visibility	guestbook_visibility	false	'public'::guestbook_visibility	공개 범위(ENUM: public 등)
public	fp_360	8	created_at	timestamp with time zone	false	now()	생성 시각
public	fp_360	9	updated_at	timestamp with time zone	false	now()	수정 시각
public	fp_360	10	target_username	text	true		대상 사용자 username(선택: 누군가에게 남기는 글)
public	fp_360	11	display_image	text	true		대표 이미지(선택)
public	fp_360	12	store_name	text	true		가게명(선택)
public	fp_360	13	place_id	text	true		장소 ID(place_id, 선택)
public	fp_360	14	is_hidden	boolean	false	false	숨김 여부(true=숨김)
public	fp_360	15	writer_profile_image	text	true		작성자 프로필 이미지(선택)
public	fp_360	16	write_ip	inet	true		작성 IP(선택)
public	fp_360	17	user_agent	text	true		User-Agent(선택)
```

### Constraints
```text
public	fp_360	PRIMARY KEY	fp_360_pkey	gb_id	public	fp_360	gb_id
```

### Indexes
```text
public	fp_360	fp_360_pkey	CREATE UNIQUE INDEX fp_360_pkey ON public.fp_360 USING btree (gb_id)
public	fp_360	fp_360_store_created_idx	CREATE INDEX fp_360_store_created_idx ON public.fp_360 USING btree (store_id, created_at DESC)
public	fp_360	fp_360_user_created_idx	CREATE INDEX fp_360_user_created_idx ON public.fp_360 USING btree (username, created_at DESC)
public	fp_360	idx_fp360_target_created	CREATE INDEX idx_fp360_target_created ON public.fp_360 USING btree (target_username, created_at DESC, gb_id DESC)
public	fp_360	idx_fp360_target_feed_created	CREATE INDEX idx_fp360_target_feed_created ON public.fp_360 USING btree (target_username, feed_id, created_at DESC)
public	fp_360	idx_fp360_target_store_created	CREATE INDEX idx_fp360_target_store_created ON public.fp_360 USING btree (target_username, store_id, created_at DESC)
public	fp_360	idx_fp360_target_vis_created	CREATE INDEX idx_fp360_target_vis_created ON public.fp_360 USING btree (target_username, visibility, created_at DESC)
```

### TODO / Notes
-

---

## public.fp_40
**설명**: 신고: 사용자/콘텐츠 신고 접수 및 플래그 처리 상태 기록

### Columns
```text
public	fp_40	1	id	integer	false	nextval('fp_40_id_seq'::regclass)	신고 ID (PK)
public	fp_40	2	reporter_username	character varying(50)	false		신고자 username
public	fp_40	3	target_username	character varying(50)	false		대상 username(선택: 사용자 대상 신고일 때)
public	fp_40	4	target_type	character varying(30)	false		대상 타입(예: user, feed, comment 등)
public	fp_40	5	target_id	integer	false		대상 ID
public	fp_40	6	reason	text	true		신고 사유
public	fp_40	7	submitted_at	timestamp without time zone	true	CURRENT_TIMESTAMP	접수 시각
public	fp_40	8	target_flag	character(1)	false	'Y'::bpchar	플래그 여부(Y/N). Y=제재/표시 상태
public	fp_40	9	unflagged_at	timestamp without time zone	true		플래그 해제 시각(선택)
```

### Constraints
```text
public	fp_40	PRIMARY KEY	fp_40_pkey	id	public	fp_40	id
```

### Indexes
```text
public	fp_40	fp_40_pkey	CREATE UNIQUE INDEX fp_40_pkey ON public.fp_40 USING btree (id)
public	fp_40	idx_fp_40_reporter	CREATE INDEX idx_fp_40_reporter ON public.fp_40 USING btree (reporter_username)
public	fp_40	idx_fp_40_submitted_at	CREATE INDEX idx_fp_40_submitted_at ON public.fp_40 USING btree (submitted_at)
public	fp_40	idx_fp_40_target_type_id	CREATE INDEX idx_fp_40_target_type_id ON public.fp_40 USING btree (target_type, target_id)
public	fp_40	idx_fp_40_target_user	CREATE INDEX idx_fp_40_target_user ON public.fp_40 USING btree (target_username)
```

### TODO / Notes
-

---

## public.fp_410
**설명**: 유저 피드 필터 설정: 사용자별 현재 적용 중인 필터/정렬/기간/지역/소스 설정을 저장

### Columns
```text
public	fp_410	1	username	character varying(50)	false		대상 username
public	fp_410	2	filter_type	character varying(50)	false		필터 타입(프리셋 구분 등)
public	fp_410	3	image_yn	character varying(10)	false		이미지 포함 여부(Y/N 또는 플래그)
public	fp_410	4	time_filter	character varying(10)	true		시간 필터 조건(선택)
public	fp_410	5	region_filter	character varying(10)	true		지역 필터 조건(선택)
public	fp_410	6	post_sorted	character varying(10)	true		정렬 방식(선택)
public	fp_410	7	sort_code	integer	true		정렬 코드(선택)
public	fp_410	8	created_at	timestamp with time zone	true	now()	생성 시각
public	fp_410	9	updated_at	timestamp with time zone	true	now()	수정 시각
public	fp_410	10	post_source	character varying(20)	true		게시물 소스(예: friend/public 등, 선택)
```

### Constraints
```text
(FK/제약 정보 없음)
```

### Indexes
```text
public	fp_410	idx_fp_410_username	CREATE INDEX idx_fp_410_username ON public.fp_410 USING btree (username)
```

### TODO / Notes
-

---

## public.fp_411
**설명**: 유저 필터 조건 변경 이력: filter_type별 조건(condition_1~4)과 정렬코드 변경 히스토리

### Columns
```text
public	fp_411	1	history_id	integer	false	nextval('fp_411_history_id_seq'::regclass)	필터 이력 ID (PK)
public	fp_411	2	username	character varying(50)	false		대상 username
public	fp_411	3	filter_type	character varying(50)	false		필터 타입
public	fp_411	4	condition_1	character varying(10)	true		조건1(선택)
public	fp_411	5	condition_2	character varying(10)	true		조건2(선택)
public	fp_411	6	condition_3	character varying(10)	true		조건3(선택)
public	fp_411	7	condition_4	character varying(10)	true		조건4(선택)
public	fp_411	8	sort_code	integer	true		정렬 코드(선택)
public	fp_411	9	created_at	timestamp with time zone	true	now()	기록 생성 시각
```

### Constraints
```text
public	fp_411	PRIMARY KEY	fp_411_pkey	history_id	public	fp_411	history_id
```

### Indexes
```text
public	fp_411	fp_411_pkey	CREATE UNIQUE INDEX fp_411_pkey ON public.fp_411 USING btree (history_id)
public	fp_411	idx_fp_411_username	CREATE INDEX idx_fp_411_username ON public.fp_411 USING btree (username)
```

### TODO / Notes
-

---

## public.fp_340
**설명**: 절기/월 기반 제철 음식 목록(카테고리, 음식명)

### Columns
```text
public	fp_340	1	id	integer	false	nextval('fp_340_id_seq'::regclass)	제철 음식 ID (PK)
public	fp_340	2	seasonal_term	character varying(20)	false		절기명
public	fp_340	3	month	integer	false		월(1~12)
public	fp_340	4	category	character varying(20)	false		카테고리
public	fp_340	5	food_name	character varying(50)	false		음식명
```

### Constraints
```text
public	fp_340	PRIMARY KEY	fp_340_pkey	id	public	fp_340	id
```

### Indexes
```text
public	fp_340	fp_340_pkey	CREATE UNIQUE INDEX fp_340_pkey ON public.fp_340 USING btree (id)
public	fp_340	uq_fp_340	CREATE UNIQUE INDEX uq_fp_340 ON public.fp_340 USING btree (seasonal_term, category, food_name)
```

### TODO / Notes
-

---

## public.fp_341
**설명**: 절기 기간 정의(시작/종료 날짜)

### Columns
```text
public	fp_341	1	id	integer	false	nextval('fp_341_id_seq'::regclass)	절기 기간 ID (PK)
public	fp_341	2	seasonal_term	character varying(20)	false		절기명
public	fp_341	3	start_date	date	false		시작 날짜
public	fp_341	4	end_date	date	false		종료 날짜
```

### Constraints
```text
public	fp_341	PRIMARY KEY	fp_341_pkey	id	public	fp_341	id
```

### Indexes
```text
public	fp_341	fp_341_pkey	CREATE UNIQUE INDEX fp_341_pkey ON public.fp_341 USING btree (id)
```

### TODO / Notes
-

---

## public.fp_99
**설명**: 음식 종류별 카테고리(음식/메뉴 분류 마스터)

### Columns
```text
public	fp_99	1	menu_id	integer	false	nextval('fp_99_menu_id_seq'::regclass)	카테고리/메뉴 ID (PK)
public	fp_99	2	name	character varying(100)	false		음식/메뉴명
public	fp_99	3	category	character varying(255)	true		분류/카테고리명
```

### Constraints
```text
public	fp_99	PRIMARY KEY	fp_99_pkey	menu_id	public	fp_99	menu_id
```

### Indexes
```text
public	fp_99	fp_99_pkey	CREATE UNIQUE INDEX fp_99_pkey ON public.fp_99 USING btree (menu_id)
```

### TODO / Notes
-

---

## public.fp_code
**설명**: 공통 코드: group_code 내 code를 정의하고 use_yn로 사용 여부 관리

### Columns
```text
public	fp_code	1	code	character varying(6)	false		코드 값
public	fp_code	2	group_code	character varying(3)	false		그룹 코드
public	fp_code	3	use_yn	character(1)	true	'Y'::bpchar	사용 여부(Y/N). Y=사용, N=미사용
public	fp_code	4	code_ex	character varying(100)	true		코드 설명
public	fp_code	5	group_code_ex	character varying(100)	true		그룹 코드 설명
public	fp_code	6	created_at	timestamp without time zone	true	CURRENT_TIMESTAMP	생성 시각
public	fp_code	7	updated_at	timestamp without time zone	true	CURRENT_TIMESTAMP	수정 시각
```

### Constraints
```text
public	fp_code	PRIMARY KEY	fp_code_pkey	code	public	fp_code	code
```

### Indexes
```text
public	fp_code	fp_code_pkey	CREATE UNIQUE INDEX fp_code_pkey ON public.fp_code USING btree (code, group_code)
```

### TODO / Notes
-

---

# ENUM

| schema | enum | value | order |
|---|---|---|---|
| public | guestbook_visibility | public | 1.0 |
| public | guestbook_visibility | friends | 2.0 |
| public | guestbook_visibility | private | 3.0 |

# fp_code values (Appendix)

## group_code=001 (기준정보 변경유형)
| code | use_yn | code_ex | created_at | updated_at |
|---|---|---|---|---|
| CD_001 | Y | 타이틀 변경 | 2024-11-24 10:00:39.601 | 2024-11-24 10:00:39.601 |
| CD_002 | Y | 주소 변경 | 2024-11-24 10:00:39.601 | 2024-11-24 10:00:39.601 |
| CD_003 | Y | 전화번호 변경 | 2024-11-24 10:00:39.601 | 2024-11-24 10:00:39.601 |
| CD_004 | Y | 카테고리 변경 | 2024-11-24 10:00:39.601 | 2024-11-24 10:00:39.601 |
| CD_005 | Y | 상세정보 변경 | 2024-11-24 10:00:39.601 | 2024-11-24 10:00:39.601 |
| CD_006 | Y | 주차정보 변경 | 2024-11-24 10:00:39.601 | 2024-11-24 10:00:39.601 |
| CD_007 | Y | 공개여부 변경 | 2024-11-24 10:00:39.601 | 2024-11-24 10:00:39.601 |

## group_code=002 (사용자정보 게시물필터)
| code | use_yn | code_ex | created_at | updated_at |
|---|---|---|---|---|
| CD_001 | Y | 최신순 | 2024-11-30 05:40:37.046 | 2024-11-30 05:40:37.046 |
| CD_002 | Y | 좋아요 높은 순 | 2024-11-30 05:41:17.347 | 2025-01-19 11:55:49.265 |
| CD_003 | Y | 댓글 많은 순 | 2024-11-30 05:41:32.086 | 2025-01-19 11:56:15.745 |
| CD_004 | Y | 조회수 높은 순 | 2024-11-30 05:41:53.723 | 2025-01-19 11:56:33.251 |

## group_code=003 (사용자 정보변경)
| code | use_yn | code_ex | created_at | updated_at |
|---|---|---|---|---|
| CD_001 | Y | 이메일 주소 변경 | 2024-12-08 13:00:50.685 | 2024-12-08 13:00:50.685 |
| CD_002 | Y | 전화번호 변경 | 2024-12-08 13:00:50.685 | 2024-12-08 13:00:50.685 |
| CD_003 | Y | 비밀번호 변경 | 2024-12-08 13:00:50.685 | 2024-12-08 13:00:50.685 |
| CD_004 | Y | 권한 변경 | 2024-12-08 13:00:50.685 | 2024-12-08 13:00:50.685 |
| CD_005 | Y | 활동지역 변경 | 2024-12-08 13:00:50.685 | 2024-12-08 13:00:50.685 |
| CD_006 | Y | 프로필이미지 변경 | 2024-12-08 13:00:50.685 | 2024-12-08 13:00:50.685 |
| CD_007 | Y | 회원탈퇴 | 2024-12-08 13:26:13.702 | 2025-03-24 19:00:20.074 |
| CD_008 | Y | 닉네임 변경 | 2025-01-19 22:53:33.728 | 2025-01-19 22:53:33.728 |
| CD_009 | Y | 닉네임 표출방식 변경 | 2025-01-19 22:53:50.720 | 2025-01-19 22:53:50.720 |
| CD_010 | Y | 회원가입 | 2025-03-24 19:07:03.905 | 2025-03-24 19:07:03.905 |

## group_code=004 (친구관계상태)
| code | use_yn | code_ex | created_at | updated_at |
|---|---|---|---|---|
| CD_001 | Y | 요청 | 2024-12-20 01:47:55.773 | 2024-12-20 01:47:55.773 |
| CD_002 | Y | 승인 | 2024-12-20 01:47:55.773 | 2024-12-20 01:47:55.773 |
| CD_003 | Y | 거절 | 2024-12-20 01:47:55.773 | 2024-12-20 01:47:55.773 |

## group_code=005 (사용자 계정 표출방식)
| code | use_yn | code_ex | created_at | updated_at |
|---|---|---|---|---|
| CD_001 | Y | 닉네임 | 2025-01-19 20:49:07.980 | 2025-01-19 20:49:07.980 |
| CD_002 | Y | 계정 | 2025-01-19 20:49:07.980 | 2025-01-19 20:49:07.980 |
| CD_003 | Y | 닉네임(계정) | 2025-01-19 20:49:07.980 | 2025-01-19 20:49:07.980 |
| CD_004 | Y | (계정)닉네임 | 2025-01-19 20:49:07.980 | 2025-01-19 20:49:07.980 |

## group_code=006 (홈화면표출방식코드)
| code | use_yn | code_ex | created_at | updated_at |
|---|---|---|---|---|
| CD_001 | Y | 최근 가장 Hot한 식당 | 2025-01-22 20:47:22.483 | 2025-01-22 20:47:22.483 |
| CD_002 | Y | 내 주변의 인기식당 | 2025-01-22 20:47:22.483 | 2025-01-22 20:47:22.483 |
| CD_003 | Y | 최근 좋아요가 가장 많은 식당 | 2025-01-22 20:47:22.483 | 2025-01-22 20:47:22.483 |
| CD_004 | Y | 주변에 예약 가능한 식당 | 2025-01-22 20:47:22.483 | 2025-01-22 20:47:22.483 |

