-- ============================================================
-- 제철음식 관리 테이블 DDL
-- Table range: fp_330 ~ fp_339
-- DBMS: PostgreSQL
-- ============================================================

-- 개발 환경 재실행용 DROP. 운영 반영 시 제거하거나 별도 마이그레이션으로 관리한다.
DROP TABLE IF EXISTS fp_339_season_source CASCADE;
DROP TABLE IF EXISTS fp_338_ingredient_dish CASCADE;
DROP TABLE IF EXISTS fp_337_dish CASCADE;
DROP TABLE IF EXISTS fp_336_season_reason CASCADE;
DROP TABLE IF EXISTS fp_335_season_month_score CASCADE;
DROP TABLE IF EXISTS fp_334_season_window CASCADE;
DROP TABLE IF EXISTS fp_333_ingredient CASCADE;
DROP TABLE IF EXISTS fp_332_region CASCADE;
DROP TABLE IF EXISTS fp_331_food_category CASCADE;
DROP TABLE IF EXISTS fp_330_code CASCADE;

-- ------------------------------------------------------------
-- fp_330_code : 공통 코드
-- ------------------------------------------------------------
CREATE TABLE fp_330_code (
    code_group                 VARCHAR(50) NOT NULL,
    code                       VARCHAR(50) NOT NULL,
    code_nm                    VARCHAR(100) NOT NULL,
    description                TEXT,
    sort_order                 INTEGER NOT NULL DEFAULT 0,
    use_yn                     CHAR(1) NOT NULL DEFAULT 'Y',
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ,
    CONSTRAINT pk_fp_330_code PRIMARY KEY (code_group, code),
    CONSTRAINT ck_fp_330_code_use_yn CHECK (use_yn IN ('Y', 'N'))
);

COMMENT ON TABLE fp_330_code IS $$서비스 전반에서 사용하는 공통 코드 테이블. 제철 유형, 제철 이유, 신뢰도, 지역 유형, 요리 방식처럼 화면과 API에서 반복 사용되는 값을 일관되게 관리한다.$$;
COMMENT ON COLUMN fp_330_code.code_group IS $$코드 그룹. 예: SEASON_TYPE, REASON, CONFIDENCE, REGION_TYPE, COOKING_TYPE.$$;
COMMENT ON COLUMN fp_330_code.code IS $$코드 값. 서버와 클라이언트가 주고받는 안정적인 식별자다.$$;
COMMENT ON COLUMN fp_330_code.code_nm IS $$사용자 또는 관리자가 읽을 수 있는 코드명.$$;
COMMENT ON COLUMN fp_330_code.description IS $$해당 코드의 의미, 적용 기준, 사용 예시를 설명한다. 관리 화면에서 혼동을 줄이는 주석 역할도 한다.$$;
COMMENT ON COLUMN fp_330_code.sort_order IS $$동일 코드 그룹 안에서 노출 순서를 제어한다.$$;
COMMENT ON COLUMN fp_330_code.use_yn IS $$사용 여부. N이면 기존 데이터 보존용으로만 남기고 신규 입력에는 사용하지 않는다.$$;
COMMENT ON COLUMN fp_330_code.created_at IS $$최초 등록 일시.$$;
COMMENT ON COLUMN fp_330_code.updated_at IS $$마지막 수정 일시. 애플리케이션 또는 트리거에서 갱신한다.$$;


-- ------------------------------------------------------------
-- fp_331_food_category : 식재료 분류
-- ------------------------------------------------------------
CREATE TABLE fp_331_food_category (
    category_id                VARCHAR(30) NOT NULL,
    parent_category_id         VARCHAR(30),
    category_nm                VARCHAR(100) NOT NULL,
    category_level             SMALLINT NOT NULL,
    category_type_cd           VARCHAR(50) NOT NULL DEFAULT 'INGREDIENT',
    description                TEXT,
    sort_order                 INTEGER NOT NULL DEFAULT 0,
    use_yn                     CHAR(1) NOT NULL DEFAULT 'Y',
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ,
    CONSTRAINT pk_fp_331_food_category PRIMARY KEY (category_id),
    CONSTRAINT fk_fp_331_parent_category FOREIGN KEY (parent_category_id) REFERENCES fp_331_food_category (category_id),
    CONSTRAINT ck_fp_331_category_level CHECK (category_level BETWEEN 1 AND 5),
    CONSTRAINT ck_fp_331_use_yn CHECK (use_yn IN ('Y', 'N'))
);

COMMENT ON TABLE fp_331_food_category IS $$제철음식에서 사용하는 식재료 분류 마스터. 농산물, 수산물, 과일류, 어류, 패류처럼 홈 화면 필터와 탐색 트리를 구성한다.$$;
COMMENT ON COLUMN fp_331_food_category.category_id IS $$분류 ID. 예: CAT_AGRI, CAT_SEAFOOD, CAT_FRUIT.$$;
COMMENT ON COLUMN fp_331_food_category.parent_category_id IS $$상위 분류 ID. NULL이면 최상위 분류다.$$;
COMMENT ON COLUMN fp_331_food_category.category_nm IS $$분류명. 사용자 화면에 노출될 수 있는 이름이다.$$;
COMMENT ON COLUMN fp_331_food_category.category_level IS $$분류 깊이. 1은 대분류, 2는 중분류, 3 이상은 세분류로 사용한다.$$;
COMMENT ON COLUMN fp_331_food_category.category_type_cd IS $$분류 용도 코드. 기본은 식재료 분류이며, 향후 요리 분류 등으로 확장할 수 있다.$$;
COMMENT ON COLUMN fp_331_food_category.description IS $$분류의 범위와 포함 기준을 설명한다. 예: 과일류는 서비스 정책상 과일로 노출하는 항목을 포함한다.$$;
COMMENT ON COLUMN fp_331_food_category.sort_order IS $$동일 레벨에서의 노출 순서.$$;
COMMENT ON COLUMN fp_331_food_category.use_yn IS $$사용 여부.$$;
COMMENT ON COLUMN fp_331_food_category.created_at IS $$최초 등록 일시.$$;
COMMENT ON COLUMN fp_331_food_category.updated_at IS $$마지막 수정 일시.$$;

CREATE INDEX idx_fp_331_parent_category ON fp_331_food_category (parent_category_id);

-- ------------------------------------------------------------
-- fp_332_region : 지역 마스터
-- ------------------------------------------------------------
CREATE TABLE fp_332_region (
    region_id                  VARCHAR(30) NOT NULL,
    parent_region_id           VARCHAR(30),
    region_nm                  VARCHAR(100) NOT NULL,
    region_type_cd             VARCHAR(50) NOT NULL,
    description                TEXT,
    sort_order                 INTEGER NOT NULL DEFAULT 0,
    use_yn                     CHAR(1) NOT NULL DEFAULT 'Y',
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ,
    CONSTRAINT pk_fp_332_region PRIMARY KEY (region_id),
    CONSTRAINT fk_fp_332_parent_region FOREIGN KEY (parent_region_id) REFERENCES fp_332_region (region_id),
    CONSTRAINT ck_fp_332_use_yn CHECK (use_yn IN ('Y', 'N'))
);

COMMENT ON TABLE fp_332_region IS $$제철 정보에 적용되는 지역 마스터. 전국, 시도, 시군구, 산지, 해역 단위까지 저장하여 지역별 제철 차이를 표현한다.$$;
COMMENT ON COLUMN fp_332_region.region_id IS $$지역 ID. 예: REG_ALL, REG_JEJU, REG_SEONGJU, REG_SOUTH_SEA.$$;
COMMENT ON COLUMN fp_332_region.parent_region_id IS $$상위 지역 ID. NULL이면 최상위 지역이다.$$;
COMMENT ON COLUMN fp_332_region.region_nm IS $$지역명. 사용자 화면의 지역 필터나 상세 정보에 노출된다.$$;
COMMENT ON COLUMN fp_332_region.region_type_cd IS $$지역 유형 코드. 예: COUNTRY, PROVINCE, CITY, SEA_AREA, ORIGIN.$$;
COMMENT ON COLUMN fp_332_region.description IS $$지역의 적용 범위와 데이터 해석 기준. 예: 남해는 행정구역이 아니라 수산물 해역 기준으로 사용한다.$$;
COMMENT ON COLUMN fp_332_region.sort_order IS $$지역 목록 노출 순서.$$;
COMMENT ON COLUMN fp_332_region.use_yn IS $$사용 여부.$$;
COMMENT ON COLUMN fp_332_region.created_at IS $$최초 등록 일시.$$;
COMMENT ON COLUMN fp_332_region.updated_at IS $$마지막 수정 일시.$$;

CREATE INDEX idx_fp_332_parent_region ON fp_332_region (parent_region_id);

-- ------------------------------------------------------------
-- fp_333_ingredient : 식재료 마스터
-- ------------------------------------------------------------
CREATE TABLE fp_333_ingredient (
    ingredient_id              VARCHAR(30) NOT NULL,
    ingredient_nm              VARCHAR(100) NOT NULL,
    ingredient_alias           VARCHAR(300),
    category_id                VARCHAR(30) NOT NULL,
    representative_region_id   VARCHAR(30),
    description                TEXT,
    storage_tip                TEXT,
    thumbnail_url              VARCHAR(500),
    search_keywords            VARCHAR(500),
    sort_order                 INTEGER NOT NULL DEFAULT 0,
    use_yn                     CHAR(1) NOT NULL DEFAULT 'Y',
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ,
    CONSTRAINT pk_fp_333_ingredient PRIMARY KEY (ingredient_id),
    CONSTRAINT fk_fp_333_category FOREIGN KEY (category_id) REFERENCES fp_331_food_category (category_id),
    CONSTRAINT fk_fp_333_representative_region FOREIGN KEY (representative_region_id) REFERENCES fp_332_region (region_id),
    CONSTRAINT ck_fp_333_use_yn CHECK (use_yn IN ('Y', 'N'))
);

COMMENT ON TABLE fp_333_ingredient IS $$제철음식의 중심이 되는 식재료 마스터. 요리명이 아니라 참외, 굴, 방어처럼 원재료 단위로 관리하여 중복을 줄인다.$$;
COMMENT ON COLUMN fp_333_ingredient.ingredient_id IS $$식재료 ID. 예: ING_CHAMOE, ING_GUL. 외부 노출보다는 내부 식별자로 사용한다.$$;
COMMENT ON COLUMN fp_333_ingredient.ingredient_nm IS $$식재료명. 사용자에게 노출되는 기본 이름이다.$$;
COMMENT ON COLUMN fp_333_ingredient.ingredient_alias IS $$별칭 또는 검색 보조어. 쉼표로 구분한다. 예: 석화, 생굴.$$;
COMMENT ON COLUMN fp_333_ingredient.category_id IS $$식재료 분류 ID. fp_331_food_category를 참조한다.$$;
COMMENT ON COLUMN fp_333_ingredient.representative_region_id IS $$대표 산지 또는 대표 지역. 식재료 카드에서 보조 정보로 사용한다.$$;
COMMENT ON COLUMN fp_333_ingredient.description IS $$식재료 소개 설명. 상세 화면의 첫 문단 또는 관리용 설명으로 사용한다.$$;
COMMENT ON COLUMN fp_333_ingredient.storage_tip IS $$보관 팁. 식재료 상세 화면에서 선택적으로 노출한다.$$;
COMMENT ON COLUMN fp_333_ingredient.thumbnail_url IS $$대표 이미지 URL. CDN 또는 파일 서버 경로를 저장한다.$$;
COMMENT ON COLUMN fp_333_ingredient.search_keywords IS $$검색 키워드. 이름, 별칭, 산지, 대표 요리 등 검색 품질을 높이기 위한 보조 문자열이다.$$;
COMMENT ON COLUMN fp_333_ingredient.sort_order IS $$식재료 목록 기본 노출 순서.$$;
COMMENT ON COLUMN fp_333_ingredient.use_yn IS $$사용 여부.$$;
COMMENT ON COLUMN fp_333_ingredient.created_at IS $$최초 등록 일시.$$;
COMMENT ON COLUMN fp_333_ingredient.updated_at IS $$마지막 수정 일시.$$;

CREATE INDEX idx_fp_333_category ON fp_333_ingredient (category_id);
CREATE INDEX idx_fp_333_representative_region ON fp_333_ingredient (representative_region_id);

-- ------------------------------------------------------------
-- fp_334_season_window : 제철 기간
-- ------------------------------------------------------------
CREATE TABLE fp_334_season_window (
    season_id                  VARCHAR(30) NOT NULL,
    ingredient_id              VARCHAR(30) NOT NULL,
    region_id                  VARCHAR(30) NOT NULL DEFAULT 'REG_ALL',
    season_type_cd             VARCHAR(50) NOT NULL,
    start_month                SMALLINT NOT NULL,
    end_month                  SMALLINT NOT NULL,
    peak_month_text            VARCHAR(50),
    display_text               VARCHAR(200),
    confidence_cd              VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    description                TEXT,
    note                       TEXT,
    sort_order                 INTEGER NOT NULL DEFAULT 0,
    use_yn                     CHAR(1) NOT NULL DEFAULT 'Y',
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ,
    CONSTRAINT pk_fp_334_season_window PRIMARY KEY (season_id),
    CONSTRAINT fk_fp_334_ingredient FOREIGN KEY (ingredient_id) REFERENCES fp_333_ingredient (ingredient_id),
    CONSTRAINT fk_fp_334_region FOREIGN KEY (region_id) REFERENCES fp_332_region (region_id),
    CONSTRAINT ck_fp_334_start_month CHECK (start_month BETWEEN 1 AND 12),
    CONSTRAINT ck_fp_334_end_month CHECK (end_month BETWEEN 1 AND 12),
    CONSTRAINT ck_fp_334_use_yn CHECK (use_yn IN ('Y', 'N'))
);

COMMENT ON TABLE fp_334_season_window IS $$식재료의 제철 기간을 정의하는 핵심 테이블. 특정 식재료가 어느 지역에서, 어떤 유형으로, 몇 월부터 몇 월까지 제철인지 저장한다.$$;
COMMENT ON COLUMN fp_334_season_window.season_id IS $$제철 기간 ID. 하나의 식재료가 지역, 성별, 크기, 제철 유형별로 여러 제철 기간을 가질 수 있다.$$;
COMMENT ON COLUMN fp_334_season_window.ingredient_id IS $$식재료 ID. fp_333_ingredient를 참조한다.$$;
COMMENT ON COLUMN fp_334_season_window.region_id IS $$제철 기간이 적용되는 지역 ID. 전국 공통이면 REG_ALL을 사용한다.$$;
COMMENT ON COLUMN fp_334_season_window.season_type_cd IS $$제철 유형 코드. 예: TASTE, PRODUCTION, PRICE, CULTURE, TASTE_PRODUCTION.$$;
COMMENT ON COLUMN fp_334_season_window.start_month IS $$제철 시작 월. 1부터 12까지 사용한다. 해를 넘기는 경우 start_month가 end_month보다 클 수 있다.$$;
COMMENT ON COLUMN fp_334_season_window.end_month IS $$제철 종료 월. 예: 굴 11월부터 3월까지는 start_month=11, end_month=3으로 저장한다.$$;
COMMENT ON COLUMN fp_334_season_window.peak_month_text IS $$절정월 표시 문자열. 단일 월, 복수 월, 범위를 모두 표현하기 위해 문자열로 둔다. 예: 6월, 12~2월.$$;
COMMENT ON COLUMN fp_334_season_window.display_text IS $$사용자 화면에 바로 보여줄 짧은 제철 문구. 예: 5~7월 제철 · 6월 절정.$$;
COMMENT ON COLUMN fp_334_season_window.confidence_cd IS $$데이터 신뢰도 코드. 예: HIGH, MEDIUM, LOW. 출처와 검수 상태에 따라 결정한다.$$;
COMMENT ON COLUMN fp_334_season_window.description IS $$해당 제철 기간의 의미를 설명한다. 사용자에게 노출 가능한 수준의 자연어 설명을 권장한다.$$;
COMMENT ON COLUMN fp_334_season_window.note IS $$관리자용 메모. 지역차, 하우스 재배, 양식, 금어기 등 단정하기 어려운 조건을 기록한다.$$;
COMMENT ON COLUMN fp_334_season_window.sort_order IS $$동일 식재료의 여러 제철 기간 중 기본 노출 순서.$$;
COMMENT ON COLUMN fp_334_season_window.use_yn IS $$사용 여부.$$;
COMMENT ON COLUMN fp_334_season_window.created_at IS $$최초 등록 일시.$$;
COMMENT ON COLUMN fp_334_season_window.updated_at IS $$마지막 수정 일시.$$;

CREATE INDEX idx_fp_334_ingredient ON fp_334_season_window (ingredient_id);
CREATE INDEX idx_fp_334_region ON fp_334_season_window (region_id);
CREATE INDEX idx_fp_334_type ON fp_334_season_window (season_type_cd);

-- ------------------------------------------------------------
-- fp_335_season_month_score : 월별 제철 점수
-- ------------------------------------------------------------
CREATE TABLE fp_335_season_month_score (
    season_id                  VARCHAR(30) NOT NULL,
    month_no                   SMALLINT NOT NULL,
    season_score               SMALLINT NOT NULL,
    is_peak_yn                 CHAR(1) NOT NULL DEFAULT 'N',
    score_label_cd             VARCHAR(50),
    description                TEXT,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ,
    CONSTRAINT pk_fp_335_season_month_score PRIMARY KEY (season_id, month_no),
    CONSTRAINT fk_fp_335_season_window FOREIGN KEY (season_id) REFERENCES fp_334_season_window (season_id) ON DELETE CASCADE,
    CONSTRAINT ck_fp_335_month_no CHECK (month_no BETWEEN 1 AND 12),
    CONSTRAINT ck_fp_335_score CHECK (season_score BETWEEN 0 AND 100),
    CONSTRAINT ck_fp_335_is_peak_yn CHECK (is_peak_yn IN ('Y', 'N'))
);

COMMENT ON TABLE fp_335_season_month_score IS $$제철을 월 단위로 정확히 자르는 대신, 월별 제철도를 0~100 점수로 저장하는 테이블. 추천 정렬, 월별 필터, 제철도 배지에 사용한다.$$;
COMMENT ON COLUMN fp_335_season_month_score.season_id IS $$제철 기간 ID. fp_334_season_window를 참조한다.$$;
COMMENT ON COLUMN fp_335_season_month_score.month_no IS $$월 번호. 1부터 12까지 사용한다.$$;
COMMENT ON COLUMN fp_335_season_month_score.season_score IS $$해당 월의 제철도 점수. 0은 비제철, 100은 절정에 가까운 상태로 해석한다.$$;
COMMENT ON COLUMN fp_335_season_month_score.is_peak_yn IS $$절정월 여부. Y이면 상세 화면과 목록에서 절정 배지로 표시할 수 있다.$$;
COMMENT ON COLUMN fp_335_season_month_score.score_label_cd IS $$점수 라벨 코드. 예: EARLY, GOOD, PEAK, LATE. 화면 문구를 세분화할 때 사용한다.$$;
COMMENT ON COLUMN fp_335_season_month_score.description IS $$해당 월 점수의 설명. 예: 출하량이 늘기 시작하나 절정은 아님.$$;
COMMENT ON COLUMN fp_335_season_month_score.created_at IS $$최초 등록 일시.$$;
COMMENT ON COLUMN fp_335_season_month_score.updated_at IS $$마지막 수정 일시.$$;

CREATE INDEX idx_fp_335_month_score ON fp_335_season_month_score (month_no, season_score DESC, is_peak_yn DESC);

-- ------------------------------------------------------------
-- fp_336_season_reason : 제철 이유
-- ------------------------------------------------------------
CREATE TABLE fp_336_season_reason (
    reason_id                  VARCHAR(30) NOT NULL,
    season_id                  VARCHAR(30) NOT NULL,
    reason_cd                  VARCHAR(50) NOT NULL,
    reason_title               VARCHAR(100) NOT NULL,
    description                TEXT,
    sort_order                 INTEGER NOT NULL DEFAULT 0,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ,
    CONSTRAINT pk_fp_336_season_reason PRIMARY KEY (reason_id),
    CONSTRAINT fk_fp_336_season_window FOREIGN KEY (season_id) REFERENCES fp_334_season_window (season_id) ON DELETE CASCADE
);

COMMENT ON TABLE fp_336_season_reason IS $$제철이라고 판단하는 이유를 저장하는 테이블. 당도 상승, 지방 증가, 산란기 관련, 수확량 증가처럼 한 제철 기간에 여러 이유를 붙일 수 있다.$$;
COMMENT ON COLUMN fp_336_season_reason.reason_id IS $$제철 이유 ID.$$;
COMMENT ON COLUMN fp_336_season_reason.season_id IS $$제철 기간 ID. fp_334_season_window를 참조한다.$$;
COMMENT ON COLUMN fp_336_season_reason.reason_cd IS $$제철 이유 코드. 예: SUGAR_PEAK, FAT_PEAK, HARVEST_PEAK, SPAWN_RELATED.$$;
COMMENT ON COLUMN fp_336_season_reason.reason_title IS $$화면에 표시할 짧은 이유명. 예: 당도 상승, 살이 오름.$$;
COMMENT ON COLUMN fp_336_season_reason.description IS $$제철 이유에 대한 설명. 사용자에게 보여줄 수 있도록 단정적인 표현보다 근거 기반의 자연어를 권장한다.$$;
COMMENT ON COLUMN fp_336_season_reason.sort_order IS $$제철 이유 노출 순서.$$;
COMMENT ON COLUMN fp_336_season_reason.created_at IS $$최초 등록 일시.$$;
COMMENT ON COLUMN fp_336_season_reason.updated_at IS $$마지막 수정 일시.$$;

CREATE INDEX idx_fp_336_season ON fp_336_season_reason (season_id);

-- ------------------------------------------------------------
-- fp_337_dish : 요리 마스터
-- ------------------------------------------------------------
CREATE TABLE fp_337_dish (
    dish_id                    VARCHAR(30) NOT NULL,
    dish_nm                    VARCHAR(100) NOT NULL,
    cooking_type_cd            VARCHAR(50),
    description                TEXT,
    thumbnail_url              VARCHAR(500),
    sort_order                 INTEGER NOT NULL DEFAULT 0,
    use_yn                     CHAR(1) NOT NULL DEFAULT 'Y',
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ,
    CONSTRAINT pk_fp_337_dish PRIMARY KEY (dish_id),
    CONSTRAINT ck_fp_337_use_yn CHECK (use_yn IN ('Y', 'N'))
);

COMMENT ON TABLE fp_337_dish IS $$식재료를 활용하는 요리 마스터. 참외화채, 감자전, 굴국밥, 방어회처럼 식재료 상세 화면의 활용 요리에 사용한다.$$;
COMMENT ON COLUMN fp_337_dish.dish_id IS $$요리 ID.$$;
COMMENT ON COLUMN fp_337_dish.dish_nm IS $$요리명. 사용자 화면에 노출된다.$$;
COMMENT ON COLUMN fp_337_dish.cooking_type_cd IS $$요리 방식 코드. 예: RAW, SOUP, GRILLED, PAN_FRIED, DESSERT.$$;
COMMENT ON COLUMN fp_337_dish.description IS $$요리 설명. 식재료와의 궁합, 추천 섭취 방식 등을 기록한다.$$;
COMMENT ON COLUMN fp_337_dish.thumbnail_url IS $$요리 대표 이미지 URL.$$;
COMMENT ON COLUMN fp_337_dish.sort_order IS $$요리 목록 기본 노출 순서.$$;
COMMENT ON COLUMN fp_337_dish.use_yn IS $$사용 여부.$$;
COMMENT ON COLUMN fp_337_dish.created_at IS $$최초 등록 일시.$$;
COMMENT ON COLUMN fp_337_dish.updated_at IS $$마지막 수정 일시.$$;


-- ------------------------------------------------------------
-- fp_338_ingredient_dish : 식재료-요리 매핑
-- ------------------------------------------------------------
CREATE TABLE fp_338_ingredient_dish (
    ingredient_id              VARCHAR(30) NOT NULL,
    dish_id                    VARCHAR(30) NOT NULL,
    recommend_months           VARCHAR(50),
    is_represent_yn            CHAR(1) NOT NULL DEFAULT 'N',
    description                TEXT,
    sort_order                 INTEGER NOT NULL DEFAULT 0,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ,
    CONSTRAINT pk_fp_338_ingredient_dish PRIMARY KEY (ingredient_id, dish_id),
    CONSTRAINT fk_fp_338_ingredient FOREIGN KEY (ingredient_id) REFERENCES fp_333_ingredient (ingredient_id) ON DELETE CASCADE,
    CONSTRAINT fk_fp_338_dish FOREIGN KEY (dish_id) REFERENCES fp_337_dish (dish_id) ON DELETE CASCADE,
    CONSTRAINT ck_fp_338_is_represent_yn CHECK (is_represent_yn IN ('Y', 'N'))
);

COMMENT ON TABLE fp_338_ingredient_dish IS $$식재료와 요리의 다대다 매핑 테이블. 하나의 식재료가 여러 요리에 쓰이고 하나의 요리가 여러 식재료와 연결될 수 있다.$$;
COMMENT ON COLUMN fp_338_ingredient_dish.ingredient_id IS $$식재료 ID. fp_333_ingredient를 참조한다.$$;
COMMENT ON COLUMN fp_338_ingredient_dish.dish_id IS $$요리 ID. fp_337_dish를 참조한다.$$;
COMMENT ON COLUMN fp_338_ingredient_dish.recommend_months IS $$요리 추천 월. 쉼표 구분 문자열로 저장한다. 예: 6,7 또는 12,1,2.$$;
COMMENT ON COLUMN fp_338_ingredient_dish.is_represent_yn IS $$대표 요리 여부. Y이면 상세 화면 상단이나 카드에서 우선 노출한다.$$;
COMMENT ON COLUMN fp_338_ingredient_dish.description IS $$이 식재료를 해당 요리로 추천하는 이유. 예: 6월 참외는 생과와 화채로 먹기 좋음.$$;
COMMENT ON COLUMN fp_338_ingredient_dish.sort_order IS $$같은 식재료 내 활용 요리 노출 순서.$$;
COMMENT ON COLUMN fp_338_ingredient_dish.created_at IS $$최초 등록 일시.$$;
COMMENT ON COLUMN fp_338_ingredient_dish.updated_at IS $$마지막 수정 일시.$$;

CREATE INDEX idx_fp_338_dish ON fp_338_ingredient_dish (dish_id);

-- ------------------------------------------------------------
-- fp_339_season_source : 근거 출처
-- ------------------------------------------------------------
CREATE TABLE fp_339_season_source (
    source_id                  VARCHAR(30) NOT NULL,
    season_id                  VARCHAR(30) NOT NULL,
    source_type_cd             VARCHAR(50) NOT NULL,
    source_nm                  VARCHAR(200) NOT NULL,
    source_url                 VARCHAR(1000),
    checked_at                 DATE,
    reliability_cd             VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    description                TEXT,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ,
    CONSTRAINT pk_fp_339_season_source PRIMARY KEY (source_id),
    CONSTRAINT fk_fp_339_season_window FOREIGN KEY (season_id) REFERENCES fp_334_season_window (season_id) ON DELETE CASCADE
);

COMMENT ON TABLE fp_339_season_source IS $$제철 기간 데이터의 근거 출처를 저장하는 테이블. 공공자료, 산지자료, 전문가 검수, 내부 운영자 입력 등을 추적하여 데이터 신뢰도를 관리한다.$$;
COMMENT ON COLUMN fp_339_season_source.source_id IS $$출처 ID.$$;
COMMENT ON COLUMN fp_339_season_source.season_id IS $$제철 기간 ID. fp_334_season_window를 참조한다.$$;
COMMENT ON COLUMN fp_339_season_source.source_type_cd IS $$출처 유형 코드. 예: PUBLIC, LOCAL, EXPERT, INTERNAL, MEDIA.$$;
COMMENT ON COLUMN fp_339_season_source.source_nm IS $$출처명. 기관명, 자료명, 검수자 역할 등을 기록한다.$$;
COMMENT ON COLUMN fp_339_season_source.source_url IS $$출처 URL. 공개 URL이 없으면 NULL로 둔다.$$;
COMMENT ON COLUMN fp_339_season_source.checked_at IS $$출처 확인일. 데이터 최신성을 판단하는 기준으로 사용한다.$$;
COMMENT ON COLUMN fp_339_season_source.reliability_cd IS $$출처 신뢰도 코드. 예: HIGH, MEDIUM, LOW.$$;
COMMENT ON COLUMN fp_339_season_source.description IS $$출처를 어떻게 해석했는지에 대한 설명. 자료가 지역 한정인지, 연도별 변동이 있는지 등을 기록한다.$$;
COMMENT ON COLUMN fp_339_season_source.created_at IS $$최초 등록 일시.$$;
COMMENT ON COLUMN fp_339_season_source.updated_at IS $$마지막 수정 일시.$$;

CREATE INDEX idx_fp_339_season ON fp_339_season_source (season_id);

-- ------------------------------------------------------------
-- Seed data - 기본 코드 및 샘플 데이터
-- 초기 화면 검증용이며 운영 반영 전 검수 필요
-- ------------------------------------------------------------
INSERT INTO fp_330_code (code_group, code, code_nm, description, sort_order) VALUES
('SEASON_TYPE', 'TASTE', '맛 제철', '맛, 향, 식감이 좋아지는 시기를 의미한다.', 10),
('SEASON_TYPE', 'PRODUCTION', '생산 제철', '수확량 또는 어획량이 늘어나는 시기를 의미한다.', 20),
('SEASON_TYPE', 'PRICE', '가격 제철', '공급이 안정되어 가격 접근성이 좋아지는 시기를 의미한다.', 30),
('SEASON_TYPE', 'CULTURE', '문화 제철', '명절, 지역 축제, 관습 등 문화적 소비 시기를 의미한다.', 40),
('SEASON_TYPE', 'TASTE_PRODUCTION', '맛·생산 제철', '맛과 생산량이 함께 좋아지는 대표 제철 시기를 의미한다.', 50),
('REASON', 'SUGAR_PEAK', '당도 상승', '과일이나 채소의 당도가 높아지는 시기다.', 10),
('REASON', 'FAT_PEAK', '지방 증가', '어류 등에 지방이 올라 맛이 진해지는 시기다.', 20),
('REASON', 'HARVEST_PEAK', '수확량 증가', '농산물 수확량이 늘어나는 시기다.', 30),
('REASON', 'CULTURAL_SEASON', '문화적 제철', '특정 계절에 많이 먹는 문화적 인식이 강하다.', 40),
('CONFIDENCE', 'HIGH', '높음', '다수 근거가 일치하거나 검수된 데이터다.', 10),
('CONFIDENCE', 'MEDIUM', '보통', '일반적 근거는 있으나 지역차 또는 연도별 변동 가능성이 있다.', 20),
('CONFIDENCE', 'LOW', '낮음', '보조 정보로만 사용하고 단정 표현을 피해야 한다.', 30),
('REGION_TYPE', 'COUNTRY', '전국', '전국 공통 기준이다.', 10),
('REGION_TYPE', 'PROVINCE', '시도', '광역 행정구역 기준이다.', 20),
('REGION_TYPE', 'CITY', '시군구', '기초 행정구역 또는 대표 산지 기준이다.', 30),
('REGION_TYPE', 'SEA_AREA', '해역', '동해, 서해, 남해 같은 수산물 해역 기준이다.', 40),
('COOKING_TYPE', 'RAW', '생식', '회, 생과처럼 가열하지 않고 먹는 방식이다.', 10),
('COOKING_TYPE', 'SOUP', '국물', '국, 탕, 국밥 계열이다.', 20),
('COOKING_TYPE', 'GRILLED', '구이', '구이 방식이다.', 30),
('COOKING_TYPE', 'DESSERT', '간식·디저트', '화채, 샐러드, 간식류다.', 40),
('SCORE_LABEL', 'EARLY', '초입', '제철이 시작되는 구간이다.', 10),
('SCORE_LABEL', 'GOOD', '좋음', '제철감이 충분한 구간이다.', 20),
('SCORE_LABEL', 'PEAK', '절정', '가장 추천하는 절정 구간이다.', 30),
('SCORE_LABEL', 'LATE', '끝자락', '제철이 마무리되는 구간이다.', 40),
('SOURCE_TYPE', 'PUBLIC', '공공자료', '공공기관 또는 공신력 있는 자료다.', 10),
('SOURCE_TYPE', 'LOCAL', '산지자료', '지역 산지 또는 생산자 자료다.', 20),
('SOURCE_TYPE', 'EXPERT', '전문가 검수', '전문가 검수나 자문을 거친 자료다.', 30),
('SOURCE_TYPE', 'INTERNAL', '내부 입력', '서비스 운영자가 내부 기준으로 입력한 자료다.', 40)
ON CONFLICT (code_group, code) DO UPDATE SET
    code_nm = EXCLUDED.code_nm,
    description = EXCLUDED.description,
    sort_order = EXCLUDED.sort_order,
    updated_at = NOW();

INSERT INTO fp_331_food_category (category_id, parent_category_id, category_nm, category_level, category_type_cd, description, sort_order) VALUES
('CAT_AGRI', NULL, '농산물', 1, 'INGREDIENT', '밭과 과수에서 생산되는 식재료의 최상위 분류다.', 10),
('CAT_SEAFOOD', NULL, '수산물', 1, 'INGREDIENT', '어류, 패류, 갑각류, 해조류를 포함하는 최상위 분류다.', 20),
('CAT_FRUIT', 'CAT_AGRI', '과일류', 2, 'INGREDIENT', '과실을 중심으로 소비되는 식재료 분류다.', 10),
('CAT_ROOT', 'CAT_AGRI', '근채류', 2, 'INGREDIENT', '뿌리나 덩이줄기를 먹는 채소류다.', 20),
('CAT_FISH', 'CAT_SEAFOOD', '어류', 2, 'INGREDIENT', '생선류 식재료 분류다.', 10),
('CAT_SHELLFISH', 'CAT_SEAFOOD', '패류', 2, 'INGREDIENT', '굴, 꼬막, 바지락 등 조개류 식재료 분류다.', 20)
ON CONFLICT (category_id) DO UPDATE SET
    parent_category_id = EXCLUDED.parent_category_id,
    category_nm = EXCLUDED.category_nm,
    category_level = EXCLUDED.category_level,
    category_type_cd = EXCLUDED.category_type_cd,
    description = EXCLUDED.description,
    sort_order = EXCLUDED.sort_order,
    updated_at = NOW();

INSERT INTO fp_332_region (region_id, parent_region_id, region_nm, region_type_cd, description, sort_order) VALUES
('REG_ALL', NULL, '전국', 'COUNTRY', '전국 공통으로 적용하는 기본 지역이다.', 0),
('REG_GANGWON', 'REG_ALL', '강원', 'PROVINCE', '강원권 농산물과 산지 정보를 표현할 때 사용한다.', 10),
('REG_JEJU', 'REG_ALL', '제주', 'PROVINCE', '제주 산지 또는 제주 해역과 관련된 제철 정보를 표현할 때 사용한다.', 20),
('REG_SEONGJU', 'REG_ALL', '성주', 'CITY', '참외 대표 산지처럼 특정 산지를 표시할 때 사용한다.', 30),
('REG_SOUTH_SEA', 'REG_ALL', '남해', 'SEA_AREA', '수산물 해역 기준으로 남해권을 표현할 때 사용한다.', 40)
ON CONFLICT (region_id) DO UPDATE SET
    parent_region_id = EXCLUDED.parent_region_id,
    region_nm = EXCLUDED.region_nm,
    region_type_cd = EXCLUDED.region_type_cd,
    description = EXCLUDED.description,
    sort_order = EXCLUDED.sort_order,
    updated_at = NOW();

INSERT INTO fp_333_ingredient
(ingredient_id, ingredient_nm, ingredient_alias, category_id, representative_region_id, description, storage_tip, search_keywords, sort_order) VALUES
('ING_CHAMOE', '참외', '성주참외,참외 melon', 'CAT_FRUIT', 'REG_SEONGJU', '초여름부터 여름까지 많이 소비되는 과일로, 아삭한 식감과 높은 수분감이 특징이다.', '상처가 적고 향이 좋은 것을 골라 서늘한 곳이나 냉장 보관한다.', '참외 성주 여름과일 화채 생과', 10),
('ING_GAMJA', '감자', '햇감자,포슬감자', 'CAT_ROOT', 'REG_GANGWON', '초여름 햇감자로 많이 소비되는 근채류 식재료다.', '통풍이 잘되는 어두운 곳에 보관하고 싹이 난 부분은 제거한다.', '감자 햇감자 감자전 조림 강원', 20),
('ING_GUL', '굴', '석화,생굴', 'CAT_SHELLFISH', 'REG_SOUTH_SEA', '겨울철 대표 패류로 국밥, 찜, 전 등으로 활용된다.', '신선도가 중요하므로 구입 후 빠르게 조리한다.', '굴 석화 겨울 패류 굴국밥 굴전', 30),
('ING_BANGEO', '방어', '대방어,겨울방어', 'CAT_FISH', 'REG_JEJU', '겨울에 지방이 올라 회로 많이 즐기는 생선이다.', '회는 당일 섭취를 권장하며, 구이용은 냉장 보관 후 빠르게 조리한다.', '방어 대방어 겨울회 제주', 40),
('ING_MAESIL', '매실', '청매실,황매실', 'CAT_FRUIT', 'REG_ALL', '6월 전후로 매실청과 장아찌 재료로 많이 사용되는 과실이다.', '상처가 적은 것을 골라 세척 후 물기를 완전히 제거하고 사용한다.', '매실 매실청 장아찌 6월', 50),
('ING_JANGEO', '장어', '민물장어,바다장어', 'CAT_FISH', 'REG_SOUTH_SEA', '여름철 보양식 이미지가 강한 식재료로 구이로 많이 소비된다.', '구입 후 냉장 보관하고 가급적 빠르게 조리한다.', '장어 여름 보양 구이', 60)
ON CONFLICT (ingredient_id) DO UPDATE SET
    ingredient_nm = EXCLUDED.ingredient_nm,
    ingredient_alias = EXCLUDED.ingredient_alias,
    category_id = EXCLUDED.category_id,
    representative_region_id = EXCLUDED.representative_region_id,
    description = EXCLUDED.description,
    storage_tip = EXCLUDED.storage_tip,
    search_keywords = EXCLUDED.search_keywords,
    sort_order = EXCLUDED.sort_order,
    updated_at = NOW();

INSERT INTO fp_334_season_window
(season_id, ingredient_id, region_id, season_type_cd, start_month, end_month, peak_month_text, display_text, confidence_cd, description, note, sort_order) VALUES
('SEA_CHAMOE_ALL', 'ING_CHAMOE', 'REG_ALL', 'TASTE_PRODUCTION', 5, 7, '6월', '5~7월 제철 · 6월 절정', 'MEDIUM', '5월부터 출하와 소비가 늘고 6월 전후로 당도와 수분감이 좋은 대표 여름 과일로 노출한다.', '하우스 재배와 산지에 따라 출하 시기가 앞당겨질 수 있다.', 10),
('SEA_GAMJA_ALL', 'ING_GAMJA', 'REG_ALL', 'PRODUCTION', 6, 7, '6~7월', '6~7월 햇감자', 'MEDIUM', '초여름 햇감자 수확 시기를 중심으로 제철감을 부여한다.', '지역과 품종에 따라 수확 시기가 달라질 수 있다.', 20),
('SEA_GUL_ALL', 'ING_GUL', 'REG_ALL', 'TASTE', 11, 3, '12~2월', '11~3월 제철 · 12~2월 절정', 'MEDIUM', '겨울철 살이 차고 굴 특유의 풍미가 좋아지는 시기로 안내한다.', '수온과 산지, 양식 상태에 따라 품질 차이가 있다.', 30),
('SEA_BANGEO_JEJU', 'ING_BANGEO', 'REG_JEJU', 'TASTE', 11, 2, '12~1월', '11~2월 제철 · 12~1월 절정', 'MEDIUM', '겨울철 지방이 올라 회로 즐기기 좋은 생선으로 안내한다.', '크기와 산지에 따라 맛의 차이가 크다.', 40),
('SEA_MAESIL_ALL', 'ING_MAESIL', 'REG_ALL', 'PRODUCTION', 6, 6, '6월', '6월 중심 · 청 담그기 좋은 시기', 'MEDIUM', '6월 전후로 매실청과 장아찌를 담그는 소비가 집중되는 식재료로 관리한다.', '청매실과 황매실의 선호 시기가 다를 수 있다.', 50),
('SEA_JANGEO_ALL', 'ING_JANGEO', 'REG_ALL', 'CULTURE', 6, 8, '7월', '6~8월 여름 보양식', 'LOW', '여름 보양식 문화와 소비 패턴을 중심으로 제철감을 부여한다.', '생물학적 맛 제철이라기보다 문화적 소비 시기 성격이 강하므로 문구에 주의한다.', 60)
ON CONFLICT (season_id) DO UPDATE SET
    ingredient_id = EXCLUDED.ingredient_id,
    region_id = EXCLUDED.region_id,
    season_type_cd = EXCLUDED.season_type_cd,
    start_month = EXCLUDED.start_month,
    end_month = EXCLUDED.end_month,
    peak_month_text = EXCLUDED.peak_month_text,
    display_text = EXCLUDED.display_text,
    confidence_cd = EXCLUDED.confidence_cd,
    description = EXCLUDED.description,
    note = EXCLUDED.note,
    sort_order = EXCLUDED.sort_order,
    updated_at = NOW();

INSERT INTO fp_335_season_month_score (season_id, month_no, season_score, is_peak_yn, score_label_cd, description) VALUES
('SEA_CHAMOE_ALL', 5, 70, 'N', 'GOOD', '제철 초입으로 출하와 소비가 늘기 시작한다.'),
('SEA_CHAMOE_ALL', 6, 100, 'Y', 'PEAK', '당도와 수분감이 좋은 절정월로 노출한다.'),
('SEA_CHAMOE_ALL', 7, 80, 'N', 'GOOD', '여전히 제철감이 있으나 절정 이후로 본다.'),
('SEA_GAMJA_ALL', 6, 95, 'Y', 'PEAK', '햇감자 수확 시기로 추천도가 높다.'),
('SEA_GAMJA_ALL', 7, 85, 'Y', 'PEAK', '햇감자 소비가 이어지는 시기다.'),
('SEA_GUL_ALL', 11, 70, 'N', 'EARLY', '겨울 제철 초입으로 본다.'),
('SEA_GUL_ALL', 12, 95, 'Y', 'PEAK', '겨울철 풍미가 좋은 절정 구간이다.'),
('SEA_GUL_ALL', 1, 100, 'Y', 'PEAK', '굴 제철도 최고점으로 노출한다.'),
('SEA_GUL_ALL', 2, 95, 'Y', 'PEAK', '절정 구간이 이어지는 시기다.'),
('SEA_GUL_ALL', 3, 60, 'N', 'LATE', '제철 끝자락으로 노출한다.'),
('SEA_BANGEO_JEJU', 11, 70, 'N', 'EARLY', '겨울 방어 초입으로 본다.'),
('SEA_BANGEO_JEJU', 12, 100, 'Y', 'PEAK', '지방이 올라 회 추천도가 높다.'),
('SEA_BANGEO_JEJU', 1, 95, 'Y', 'PEAK', '절정 구간이 이어지는 시기다.'),
('SEA_BANGEO_JEJU', 2, 70, 'N', 'LATE', '제철 끝자락으로 본다.'),
('SEA_MAESIL_ALL', 6, 100, 'Y', 'PEAK', '매실청과 장아찌 재료로 가장 많이 찾는 달이다.'),
('SEA_JANGEO_ALL', 6, 75, 'N', 'GOOD', '여름 보양식 소비가 시작되는 시기다.'),
('SEA_JANGEO_ALL', 7, 90, 'Y', 'PEAK', '여름 보양식 이미지가 가장 강한 시기다.'),
('SEA_JANGEO_ALL', 8, 75, 'N', 'GOOD', '여름 소비가 이어지는 시기다.')
ON CONFLICT (season_id, month_no) DO UPDATE SET
    season_score = EXCLUDED.season_score,
    is_peak_yn = EXCLUDED.is_peak_yn,
    score_label_cd = EXCLUDED.score_label_cd,
    description = EXCLUDED.description,
    updated_at = NOW();

INSERT INTO fp_336_season_reason (reason_id, season_id, reason_cd, reason_title, description, sort_order) VALUES
('REA_CHAMOE_01', 'SEA_CHAMOE_ALL', 'SUGAR_PEAK', '당도 상승', '6월 전후로 참외 특유의 단맛과 향을 기대하기 좋은 시기로 안내한다.', 10),
('REA_CHAMOE_02', 'SEA_CHAMOE_ALL', 'HARVEST_PEAK', '출하량 증가', '초여름부터 출하와 유통량이 늘어 접근성이 좋아진다.', 20),
('REA_GAMJA_01', 'SEA_GAMJA_ALL', 'HARVEST_PEAK', '햇감자 수확', '초여름 햇감자 수확 시기를 중심으로 제철감을 부여한다.', 10),
('REA_GUL_01', 'SEA_GUL_ALL', 'TASTE_PEAK', '겨울 풍미', '겨울철 굴의 풍미와 식감을 기대하기 좋은 시기로 안내한다.', 10),
('REA_BANGEO_01', 'SEA_BANGEO_JEJU', 'FAT_PEAK', '지방 증가', '겨울철 지방이 올라 회로 즐기기 좋은 생선으로 소개한다.', 10),
('REA_MAESIL_01', 'SEA_MAESIL_ALL', 'HARVEST_PEAK', '매실청 시기', '6월 전후로 매실청과 장아찌를 담그는 소비가 집중된다.', 10),
('REA_JANGEO_01', 'SEA_JANGEO_ALL', 'CULTURAL_SEASON', '여름 보양식', '여름철 보양식으로 많이 소비되는 문화적 제철 성격이 강하다.', 10)
ON CONFLICT (reason_id) DO UPDATE SET
    season_id = EXCLUDED.season_id,
    reason_cd = EXCLUDED.reason_cd,
    reason_title = EXCLUDED.reason_title,
    description = EXCLUDED.description,
    sort_order = EXCLUDED.sort_order,
    updated_at = NOW();

INSERT INTO fp_337_dish (dish_id, dish_nm, cooking_type_cd, description, sort_order) VALUES
('DISH_CHAMOE_RAW', '참외 생과', 'RAW', '제철 참외를 그대로 먹는 가장 기본적인 방식이다.', 10),
('DISH_CHAMOE_HWACHAE', '참외화채', 'DESSERT', '수분감 있는 참외를 활용한 여름 간식이다.', 20),
('DISH_GAMJA_JEON', '감자전', 'PAN_FRIED', '햇감자의 포슬한 식감을 살리기 좋은 요리다.', 30),
('DISH_GUL_GUKBAP', '굴국밥', 'SOUP', '겨울 굴을 따뜻하게 즐기는 대표 요리다.', 40),
('DISH_BANGEO_RAW', '방어회', 'RAW', '겨울철 지방이 오른 방어를 즐기는 대표 방식이다.', 50),
('DISH_MAESIL_CHEONG', '매실청', 'DESSERT', '6월 매실을 설탕과 함께 숙성해 만드는 저장식이다.', 60),
('DISH_JANGEO_GRILLED', '장어구이', 'GRILLED', '여름 보양식 이미지가 강한 대표 장어 요리다.', 70)
ON CONFLICT (dish_id) DO UPDATE SET
    dish_nm = EXCLUDED.dish_nm,
    cooking_type_cd = EXCLUDED.cooking_type_cd,
    description = EXCLUDED.description,
    sort_order = EXCLUDED.sort_order,
    updated_at = NOW();

INSERT INTO fp_338_ingredient_dish (ingredient_id, dish_id, recommend_months, is_represent_yn, description, sort_order) VALUES
('ING_CHAMOE', 'DISH_CHAMOE_RAW', '5,6,7', 'Y', '제철 참외의 향과 당도를 가장 직접적으로 느낄 수 있다.', 10),
('ING_CHAMOE', 'DISH_CHAMOE_HWACHAE', '6,7', 'N', '더운 시기에 수분감 있는 간식으로 추천한다.', 20),
('ING_GAMJA', 'DISH_GAMJA_JEON', '6,7', 'Y', '햇감자의 식감을 활용하기 좋은 요리다.', 10),
('ING_GUL', 'DISH_GUL_GUKBAP', '12,1,2', 'Y', '겨울 굴을 따뜻한 국물로 즐기는 대표 방식이다.', 10),
('ING_BANGEO', 'DISH_BANGEO_RAW', '12,1', 'Y', '겨울 방어의 지방감을 즐기기 좋은 대표 요리다.', 10),
('ING_MAESIL', 'DISH_MAESIL_CHEONG', '6', 'Y', '6월 매실을 활용한 대표 저장식이다.', 10),
('ING_JANGEO', 'DISH_JANGEO_GRILLED', '6,7,8', 'Y', '여름 보양식으로 가장 익숙한 조리 방식이다.', 10)
ON CONFLICT (ingredient_id, dish_id) DO UPDATE SET
    recommend_months = EXCLUDED.recommend_months,
    is_represent_yn = EXCLUDED.is_represent_yn,
    description = EXCLUDED.description,
    sort_order = EXCLUDED.sort_order,
    updated_at = NOW();

INSERT INTO fp_339_season_source (source_id, season_id, source_type_cd, source_nm, source_url, checked_at, reliability_cd, description) VALUES
('SRC_CHAMOE_01', 'SEA_CHAMOE_ALL', 'INTERNAL', '초기 서비스 제철 기준', NULL, CURRENT_DATE, 'MEDIUM', '초기 화면 검증용 기준이다. 운영 전 공공자료 또는 산지자료로 검수 필요.'),
('SRC_GAMJA_01', 'SEA_GAMJA_ALL', 'INTERNAL', '초기 서비스 제철 기준', NULL, CURRENT_DATE, 'MEDIUM', '초기 화면 검증용 기준이다. 지역별 수확 시기 검수 필요.'),
('SRC_GUL_01', 'SEA_GUL_ALL', 'INTERNAL', '초기 서비스 제철 기준', NULL, CURRENT_DATE, 'MEDIUM', '초기 화면 검증용 기준이다. 산지 및 위생 정보와 별도 관리 필요.'),
('SRC_BANGEO_01', 'SEA_BANGEO_JEJU', 'INTERNAL', '초기 서비스 제철 기준', NULL, CURRENT_DATE, 'MEDIUM', '초기 화면 검증용 기준이다. 크기와 산지 기준을 추가 검수할 것.'),
('SRC_MAESIL_01', 'SEA_MAESIL_ALL', 'INTERNAL', '초기 서비스 제철 기준', NULL, CURRENT_DATE, 'MEDIUM', '초기 화면 검증용 기준이다. 청매실과 황매실 기준을 분리할 수 있다.'),
('SRC_JANGEO_01', 'SEA_JANGEO_ALL', 'INTERNAL', '초기 서비스 제철 기준', NULL, CURRENT_DATE, 'LOW', '문화적 소비 시기 성격이 강하므로 맛 제철처럼 단정하지 않는다.')
ON CONFLICT (source_id) DO UPDATE SET
    season_id = EXCLUDED.season_id,
    source_type_cd = EXCLUDED.source_type_cd,
    source_nm = EXCLUDED.source_nm,
    source_url = EXCLUDED.source_url,
    checked_at = EXCLUDED.checked_at,
    reliability_cd = EXCLUDED.reliability_cd,
    description = EXCLUDED.description,
    updated_at = NOW();

-- ------------------------------------------------------------
-- Query examples
-- ------------------------------------------------------------
-- 특정 월의 제철음식 목록
-- SELECT
--     i.ingredient_id,
--     i.ingredient_nm,
--     c.category_nm,
--     w.region_id,
--     r.region_nm,
--     w.display_text,
--     w.season_type_cd,
--     w.confidence_cd,
--     ms.month_no,
--     ms.season_score,
--     ms.is_peak_yn,
--     ms.score_label_cd
-- FROM fp_335_season_month_score ms
-- JOIN fp_334_season_window w ON w.season_id = ms.season_id
-- JOIN fp_333_ingredient i ON i.ingredient_id = w.ingredient_id
-- JOIN fp_331_food_category c ON c.category_id = i.category_id
-- JOIN fp_332_region r ON r.region_id = w.region_id
-- WHERE ms.month_no = 6
--   AND ms.season_score >= 60
--   AND i.use_yn = 'Y'
--   AND w.use_yn = 'Y'
-- ORDER BY ms.is_peak_yn DESC, ms.season_score DESC, i.sort_order ASC;
