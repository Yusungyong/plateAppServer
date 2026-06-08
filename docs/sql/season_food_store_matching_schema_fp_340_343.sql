-- ============================================================
-- Season food store matching schema
-- Table range: fp_340 ~ fp_343
-- DBMS: PostgreSQL
-- Non-destructive migration draft. No DROP statements.
-- ============================================================

-- ------------------------------------------------------------
-- fp_340_season_match_keyword : season food matching keyword dictionary
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fp_340_season_match_keyword (
    keyword_id                 BIGSERIAL PRIMARY KEY,
    ingredient_id              VARCHAR(30) NOT NULL,
    keyword                    VARCHAR(100) NOT NULL,
    keyword_type_cd            VARCHAR(50) NOT NULL,
    match_weight               NUMERIC(6,3) NOT NULL DEFAULT 1.000,
    exact_match_yn             CHAR(1) NOT NULL DEFAULT 'N',
    description                TEXT,
    use_yn                     CHAR(1) NOT NULL DEFAULT 'Y',
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ,
    CONSTRAINT fk_fp_340_ingredient FOREIGN KEY (ingredient_id) REFERENCES fp_333_ingredient (ingredient_id),
    CONSTRAINT ck_fp_340_keyword_type CHECK (keyword_type_cd IN ('BASE', 'ALIAS', 'DISH', 'MENU', 'EXCLUDE')),
    CONSTRAINT ck_fp_340_match_weight CHECK (match_weight >= 0),
    CONSTRAINT ck_fp_340_exact_match_yn CHECK (exact_match_yn IN ('Y', 'N')),
    CONSTRAINT ck_fp_340_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE INDEX IF NOT EXISTS idx_fp_340_ingredient ON fp_340_season_match_keyword (ingredient_id);
CREATE INDEX IF NOT EXISTS idx_fp_340_keyword_type ON fp_340_season_match_keyword (keyword_type_cd);
CREATE UNIQUE INDEX IF NOT EXISTS ux_fp_340_active_keyword
    ON fp_340_season_match_keyword (ingredient_id, lower(keyword), keyword_type_cd)
    WHERE use_yn = 'Y';

-- ------------------------------------------------------------
-- fp_341_season_store_match : cached store-level season food match
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fp_341_season_store_match (
    match_id                   BIGSERIAL PRIMARY KEY,
    ingredient_id              VARCHAR(30) NOT NULL,
    season_id                  VARCHAR(30),
    store_id                   INTEGER,
    restaurant_id              BIGINT,
    place_id                   VARCHAR(255),
    store_name                 VARCHAR(150),
    address                    VARCHAR(300),
    representative_menu_name   VARCHAR(150),
    representative_image_url   TEXT,
    match_score                NUMERIC(8,3) NOT NULL DEFAULT 0,
    season_score               SMALLINT,
    evidence_count             INTEGER NOT NULL DEFAULT 0,
    match_status_cd            VARCHAR(50) NOT NULL DEFAULT 'AUTO',
    match_source_cd            VARCHAR(50) NOT NULL,
    matched_keywords           VARCHAR(500),
    reason_text                VARCHAR(300),
    use_yn                     CHAR(1) NOT NULL DEFAULT 'Y',
    generated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at                 TIMESTAMPTZ,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ,
    CONSTRAINT fk_fp_341_ingredient FOREIGN KEY (ingredient_id) REFERENCES fp_333_ingredient (ingredient_id),
    CONSTRAINT fk_fp_341_season FOREIGN KEY (season_id) REFERENCES fp_334_season_window (season_id),
    CONSTRAINT ck_fp_341_match_status CHECK (match_status_cd IN ('AUTO', 'CONFIRMED', 'REJECTED')),
    CONSTRAINT ck_fp_341_match_source CHECK (match_source_cd IN ('FP320_MENU', 'RESTAURANT_MENU', 'FEED', 'VIDEO', 'MIXED', 'MANUAL')),
    CONSTRAINT ck_fp_341_use_yn CHECK (use_yn IN ('Y', 'N')),
    CONSTRAINT ck_fp_341_identity CHECK (store_id IS NOT NULL OR restaurant_id IS NOT NULL OR place_id IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_fp_341_ingredient_status ON fp_341_season_store_match (ingredient_id, match_status_cd, use_yn);
CREATE INDEX IF NOT EXISTS idx_fp_341_store ON fp_341_season_store_match (store_id);
CREATE INDEX IF NOT EXISTS idx_fp_341_restaurant ON fp_341_season_store_match (restaurant_id);
CREATE INDEX IF NOT EXISTS idx_fp_341_place ON fp_341_season_store_match (place_id);
CREATE INDEX IF NOT EXISTS idx_fp_341_score ON fp_341_season_store_match (season_score DESC, match_score DESC);
CREATE UNIQUE INDEX IF NOT EXISTS ux_fp_341_match_identity
    ON fp_341_season_store_match (
        ingredient_id,
        COALESCE(store_id, -1),
        COALESCE(restaurant_id, -1),
        COALESCE(place_id, '')
    )
    WHERE use_yn = 'Y';

-- ------------------------------------------------------------
-- fp_342_season_match_evidence : original text evidence for a store match
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fp_342_season_match_evidence (
    evidence_id                BIGSERIAL PRIMARY KEY,
    match_id                   BIGINT NOT NULL,
    target_type_cd             VARCHAR(50) NOT NULL,
    target_id                  VARCHAR(100) NOT NULL,
    matched_field              VARCHAR(50) NOT NULL,
    matched_keyword            VARCHAR(100) NOT NULL,
    matched_text               TEXT,
    field_weight               NUMERIC(6,3) NOT NULL DEFAULT 1.000,
    keyword_weight             NUMERIC(6,3) NOT NULL DEFAULT 1.000,
    score                      NUMERIC(8,3) NOT NULL DEFAULT 0,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_fp_342_match FOREIGN KEY (match_id) REFERENCES fp_341_season_store_match (match_id) ON DELETE CASCADE,
    CONSTRAINT ck_fp_342_target_type CHECK (target_type_cd IN ('FP320_MENU', 'RESTAURANT_MENU', 'FEED', 'VIDEO'))
);

CREATE INDEX IF NOT EXISTS idx_fp_342_match ON fp_342_season_match_evidence (match_id);
CREATE INDEX IF NOT EXISTS idx_fp_342_target ON fp_342_season_match_evidence (target_type_cd, target_id);

-- ------------------------------------------------------------
-- fp_343_season_store_match_override : admin correction history
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fp_343_season_store_match_override (
    override_id                BIGSERIAL PRIMARY KEY,
    ingredient_id              VARCHAR(30) NOT NULL,
    store_id                   INTEGER,
    restaurant_id              BIGINT,
    place_id                   VARCHAR(255),
    override_status_cd         VARCHAR(50) NOT NULL,
    representative_menu_name   VARCHAR(150),
    representative_image_url   TEXT,
    note                       TEXT,
    created_by                 VARCHAR(100),
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ,
    CONSTRAINT fk_fp_343_ingredient FOREIGN KEY (ingredient_id) REFERENCES fp_333_ingredient (ingredient_id),
    CONSTRAINT ck_fp_343_override_status CHECK (override_status_cd IN ('CONFIRMED', 'REJECTED', 'PINNED')),
    CONSTRAINT ck_fp_343_identity CHECK (store_id IS NOT NULL OR restaurant_id IS NOT NULL OR place_id IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_fp_343_ingredient ON fp_343_season_store_match_override (ingredient_id);
CREATE INDEX IF NOT EXISTS idx_fp_343_store ON fp_343_season_store_match_override (store_id);
CREATE INDEX IF NOT EXISTS idx_fp_343_restaurant ON fp_343_season_store_match_override (restaurant_id);
CREATE INDEX IF NOT EXISTS idx_fp_343_place ON fp_343_season_store_match_override (place_id);

-- ------------------------------------------------------------
-- Optional starter keywords generated from existing ingredient names, aliases, and representative dishes.
-- Run after fp_330 ~ fp_339 seed data is present.
-- ------------------------------------------------------------
INSERT INTO fp_340_season_match_keyword (ingredient_id, keyword, keyword_type_cd, match_weight, exact_match_yn, description)
SELECT ingredient_id, ingredient_nm, 'BASE', 1.000, 'N', 'Generated from fp_333_ingredient.ingredient_nm'
FROM fp_333_ingredient
WHERE use_yn = 'Y'
ON CONFLICT DO NOTHING;

INSERT INTO fp_340_season_match_keyword (ingredient_id, keyword, keyword_type_cd, match_weight, exact_match_yn, description)
SELECT i.ingredient_id, trim(alias_keyword), 'ALIAS', 1.200, 'N', 'Generated from fp_333_ingredient.ingredient_alias'
FROM fp_333_ingredient i
CROSS JOIN LATERAL regexp_split_to_table(COALESCE(i.ingredient_alias, ''), ',') AS alias_keyword
WHERE i.use_yn = 'Y'
  AND trim(alias_keyword) <> ''
ON CONFLICT DO NOTHING;

INSERT INTO fp_340_season_match_keyword (ingredient_id, keyword, keyword_type_cd, match_weight, exact_match_yn, description)
SELECT id.ingredient_id, d.dish_nm, 'DISH', 1.500, 'N', 'Generated from fp_337_dish.dish_nm'
FROM fp_338_ingredient_dish id
JOIN fp_337_dish d
  ON d.dish_id = id.dish_id
WHERE d.use_yn = 'Y'
ON CONFLICT DO NOTHING;
