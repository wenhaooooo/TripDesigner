-- ============================================================
-- Global Travel Knowledge Base - V2 Migration
-- ============================================================
-- Creates the knowledge base schema supporting:
--   - Countries / Cities / POIs / Restaurants / Hotels / Routes
--   - Travel guides (multi-language content)
--   - Semantic knowledge chunks for RAG (pgvector embeddings)
--   - Source tracking & sync state for incremental crawls
--   - Knowledge graph relations & tags
--   - Image assets attached to any entity
-- Requires: pgvector (enabled in V1)
-- ============================================================

-- Extensions (idempotent)
CREATE EXTENSION IF NOT EXISTS vector;

-- ------------------------------------------------------------
-- kb_countries : Country reference data
-- ------------------------------------------------------------
CREATE TABLE kb_countries (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    iso_code2       VARCHAR(2),
    iso_code3       VARCHAR(3),
    continent       VARCHAR(32),
    capital         VARCHAR(128),
    currency_code   VARCHAR(3),
    languages       JSONB,
    metadata        JSONB,
    source          VARCHAR(32),
    source_id       VARCHAR(128),
    content_hash    VARCHAR(64),
    last_synced_at  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_kb_countries_name        ON kb_countries(name);
CREATE INDEX idx_kb_countries_iso2        ON kb_countries(iso_code2);
CREATE INDEX idx_kb_countries_iso3        ON kb_countries(iso_code3);
CREATE INDEX idx_kb_countries_continent   ON kb_countries(continent);
CREATE INDEX idx_kb_countries_source      ON kb_countries(source, source_id);
CREATE INDEX idx_kb_countries_content_hash ON kb_countries(content_hash);
CREATE INDEX idx_kb_countries_metadata_gin ON kb_countries USING GIN (metadata);
CREATE INDEX idx_kb_countries_languages_gin ON kb_countries USING GIN (languages);

-- ------------------------------------------------------------
-- kb_cities : City reference data
-- ------------------------------------------------------------
CREATE TABLE kb_cities (
    id              BIGSERIAL PRIMARY KEY,
    country_id      BIGINT,
    name            VARCHAR(128) NOT NULL,
    name_local      VARCHAR(128),
    timezone        VARCHAR(64),
    population      INTEGER,
    latitude        DECIMAL(9,6),
    longitude       DECIMAL(9,6),
    metadata        JSONB,
    source          VARCHAR(32),
    source_id       VARCHAR(128),
    content_hash    VARCHAR(64),
    last_synced_at  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_kb_cities_country_id   ON kb_cities(country_id);
CREATE INDEX idx_kb_cities_name         ON kb_cities(name);
CREATE INDEX idx_kb_cities_timezone     ON kb_cities(timezone);
CREATE INDEX idx_kb_cities_source       ON kb_cities(source, source_id);
CREATE INDEX idx_kb_cities_content_hash ON kb_cities(content_hash);
CREATE INDEX idx_kb_cities_metadata_gin ON kb_cities USING GIN (metadata);

-- ------------------------------------------------------------
-- kb_pois : Points of Interest
-- ------------------------------------------------------------
CREATE TABLE kb_pois (
    id              BIGSERIAL PRIMARY KEY,
    city_id         BIGINT,
    name            VARCHAR(256) NOT NULL,
    name_local      VARCHAR(256),
    category        VARCHAR(32),
    subcategory     VARCHAR(64),
    description     TEXT,
    latitude        DECIMAL(9,6),
    longitude       DECIMAL(9,6),
    address         VARCHAR(512),
    opening_hours   JSONB,
    price_info      JSONB,
    contact_info    JSONB,
    rating          DECIMAL(3,2),
    review_count    INTEGER,
    metadata        JSONB,
    source          VARCHAR(32),
    source_id       VARCHAR(128),
    content_hash    VARCHAR(64),
    last_synced_at  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_kb_pois_city_id        ON kb_pois(city_id);
CREATE INDEX idx_kb_pois_name           ON kb_pois(name);
CREATE INDEX idx_kb_pois_category       ON kb_pois(category);
CREATE INDEX idx_kb_pois_subcategory    ON kb_pois(subcategory);
CREATE INDEX idx_kb_pois_source         ON kb_pois(source, source_id);
CREATE INDEX idx_kb_pois_content_hash   ON kb_pois(content_hash);
CREATE INDEX idx_kb_pois_metadata_gin   ON kb_pois USING GIN (metadata);
CREATE INDEX idx_kb_pois_opening_gin    ON kb_pois USING GIN (opening_hours);

-- ------------------------------------------------------------
-- kb_travel_guides : Multi-language travel guide content
-- ------------------------------------------------------------
CREATE TABLE kb_travel_guides (
    id              BIGSERIAL PRIMARY KEY,
    city_id         BIGINT,
    country_id      BIGINT,
    title           VARCHAR(256) NOT NULL,
    language        VARCHAR(10),
    content         TEXT,
    summary         TEXT,
    sections        JSONB,
    metadata        JSONB,
    source          VARCHAR(32),
    source_id       VARCHAR(128),
    content_hash    VARCHAR(64),
    last_synced_at  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_kb_travel_guides_city_id     ON kb_travel_guides(city_id);
CREATE INDEX idx_kb_travel_guides_country_id  ON kb_travel_guides(country_id);
CREATE INDEX idx_kb_travel_guides_language     ON kb_travel_guides(language);
CREATE INDEX idx_kb_travel_guides_source      ON kb_travel_guides(source, source_id);
CREATE INDEX idx_kb_travel_guides_content_hash ON kb_travel_guides(content_hash);
CREATE INDEX idx_kb_travel_guides_sections_gin ON kb_travel_guides USING GIN (sections);
CREATE INDEX idx_kb_travel_guides_metadata_gin ON kb_travel_guides USING GIN (metadata);

-- ------------------------------------------------------------
-- kb_restaurants : Restaurant reference data
-- ------------------------------------------------------------
CREATE TABLE kb_restaurants (
    id              BIGSERIAL PRIMARY KEY,
    city_id         BIGINT,
    name            VARCHAR(256) NOT NULL,
    name_local      VARCHAR(256),
    cuisine_type    VARCHAR(128),
    price_range     VARCHAR(4),
    address         VARCHAR(512),
    latitude        DECIMAL(9,6),
    longitude       DECIMAL(9,6),
    opening_hours   JSONB,
    contact_info    JSONB,
    rating          DECIMAL(3,2),
    review_count    INTEGER,
    metadata        JSONB,
    source          VARCHAR(32),
    source_id       VARCHAR(128),
    content_hash    VARCHAR(64),
    last_synced_at  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_kb_restaurants_city_id       ON kb_restaurants(city_id);
CREATE INDEX idx_kb_restaurants_name          ON kb_restaurants(name);
CREATE INDEX idx_kb_restaurants_cuisine        ON kb_restaurants(cuisine_type);
CREATE INDEX idx_kb_restaurants_price_range   ON kb_restaurants(price_range);
CREATE INDEX idx_kb_restaurants_source        ON kb_restaurants(source, source_id);
CREATE INDEX idx_kb_restaurants_content_hash  ON kb_restaurants(content_hash);
CREATE INDEX idx_kb_restaurants_metadata_gin  ON kb_restaurants USING GIN (metadata);
CREATE INDEX idx_kb_restaurants_opening_gin   ON kb_restaurants USING GIN (opening_hours);

-- ------------------------------------------------------------
-- kb_hotels : Hotel reference data
-- ------------------------------------------------------------
CREATE TABLE kb_hotels (
    id              BIGSERIAL PRIMARY KEY,
    city_id         BIGINT,
    name            VARCHAR(256) NOT NULL,
    name_local      VARCHAR(256),
    category        VARCHAR(32),
    star_rating     INTEGER,
    address         VARCHAR(512),
    latitude        DECIMAL(9,6),
    longitude       DECIMAL(9,6),
    amenities       JSONB,
    room_info       JSONB,
    contact_info    JSONB,
    rating          DECIMAL(3,2),
    review_count    INTEGER,
    price_from      DECIMAL(10,2),
    currency_code   VARCHAR(3),
    metadata        JSONB,
    source          VARCHAR(32),
    source_id       VARCHAR(128),
    content_hash    VARCHAR(64),
    last_synced_at  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_kb_hotels_city_id        ON kb_hotels(city_id);
CREATE INDEX idx_kb_hotels_name           ON kb_hotels(name);
CREATE INDEX idx_kb_hotels_category       ON kb_hotels(category);
CREATE INDEX idx_kb_hotels_star_rating    ON kb_hotels(star_rating);
CREATE INDEX idx_kb_hotels_source         ON kb_hotels(source, source_id);
CREATE INDEX idx_kb_hotels_content_hash   ON kb_hotels(content_hash);
CREATE INDEX idx_kb_hotels_metadata_gin  ON kb_hotels USING GIN (metadata);
CREATE INDEX idx_kb_hotels_amenities_gin ON kb_hotels USING GIN (amenities);

-- ------------------------------------------------------------
-- kb_routes : Travel routes between cities
-- ------------------------------------------------------------
CREATE TABLE kb_routes (
    id                      BIGSERIAL PRIMARY KEY,
    from_city_id            BIGINT,
    to_city_id              BIGINT,
    route_type              VARCHAR(32),
    distance_km             DECIMAL(10,2),
    estimated_duration      JSONB,
    transportation_methods  JSONB,
    description             TEXT,
    metadata                JSONB,
    source                  VARCHAR(32),
    source_id               VARCHAR(128),
    content_hash            VARCHAR(64),
    last_synced_at          TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                 INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_kb_routes_from_city_id   ON kb_routes(from_city_id);
CREATE INDEX idx_kb_routes_to_city_id     ON kb_routes(to_city_id);
CREATE INDEX idx_kb_routes_type           ON kb_routes(route_type);
CREATE INDEX idx_kb_routes_source         ON kb_routes(source, source_id);
CREATE INDEX idx_kb_routes_content_hash   ON kb_routes(content_hash);
CREATE INDEX idx_kb_routes_metadata_gin   ON kb_routes USING GIN (metadata);
CREATE INDEX idx_kb_routes_transport_gin  ON kb_routes USING GIN (transportation_methods);

-- ------------------------------------------------------------
-- kb_knowledge_chunks : Semantic chunks for RAG retrieval
-- ------------------------------------------------------------
CREATE TABLE kb_knowledge_chunks (
    id              BIGSERIAL PRIMARY KEY,
    entity_type     VARCHAR(32) NOT NULL,
    entity_id       BIGINT NOT NULL,
    chunk_type      VARCHAR(32),
    chunk_index     INT NOT NULL DEFAULT 0,
    title           VARCHAR(256),
    content         TEXT NOT NULL,
    content_hash    VARCHAR(64),
    embedding       vector(1536),
    metadata        JSONB,
    language        VARCHAR(10),
    token_count     INTEGER,
    source          VARCHAR(32),
    source_id       VARCHAR(128),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_kb_chunks_entity        ON kb_knowledge_chunks(entity_type, entity_id);
CREATE INDEX idx_kb_chunks_chunk_type     ON kb_knowledge_chunks(chunk_type);
CREATE INDEX idx_kb_chunks_language       ON kb_knowledge_chunks(language);
CREATE INDEX idx_kb_chunks_content_hash   ON kb_knowledge_chunks(content_hash);
CREATE INDEX idx_kb_chunks_source         ON kb_knowledge_chunks(source, source_id);
CREATE INDEX idx_kb_chunks_metadata_gin  ON kb_knowledge_chunks USING GIN (metadata);
CREATE INDEX idx_kb_chunks_embedding_hnsw ON kb_knowledge_chunks USING hnsw (embedding vector_cosine_ops);

-- ------------------------------------------------------------
-- kb_knowledge_sources : Source tracking & sync state
-- ------------------------------------------------------------
CREATE TABLE kb_knowledge_sources (
    id              BIGSERIAL PRIMARY KEY,
    source_type     VARCHAR(32) NOT NULL,
    source_url      VARCHAR(1024),
    source_id       VARCHAR(128),
    entity_type     VARCHAR(32),
    entity_id       BIGINT,
    raw_content     TEXT,
    content_hash    VARCHAR(64),
    fetched_at      TIMESTAMPTZ,
    etag            VARCHAR(256),
    last_modified   VARCHAR(64),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message   TEXT,
    retry_count     INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_kb_sources_source_type   ON kb_knowledge_sources(source_type);
CREATE INDEX idx_kb_sources_entity         ON kb_knowledge_sources(entity_type, entity_id);
CREATE INDEX idx_kb_sources_status         ON kb_knowledge_sources(status);
CREATE INDEX idx_kb_sources_content_hash  ON kb_knowledge_sources(content_hash);
CREATE INDEX idx_kb_sources_source_id     ON kb_knowledge_sources(source_type, source_id);

-- ------------------------------------------------------------
-- kb_knowledge_tags : Tags for knowledge entities
-- ------------------------------------------------------------
CREATE TABLE kb_knowledge_tags (
    id              BIGSERIAL PRIMARY KEY,
    entity_type     VARCHAR(32) NOT NULL,
    entity_id       BIGINT NOT NULL,
    tag             VARCHAR(64) NOT NULL,
    tag_type        VARCHAR(32),
    language        VARCHAR(10),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_kb_tags_entity       ON kb_knowledge_tags(entity_type, entity_id);
CREATE INDEX idx_kb_tags_tag           ON kb_knowledge_tags(tag);
CREATE INDEX idx_kb_tags_tag_type      ON kb_knowledge_tags(tag_type);
CREATE INDEX idx_kb_tags_language      ON kb_knowledge_tags(language);

-- ------------------------------------------------------------
-- kb_knowledge_relations : Knowledge graph relations
-- ------------------------------------------------------------
CREATE TABLE kb_knowledge_relations (
    id                  BIGSERIAL PRIMARY KEY,
    from_entity_type    VARCHAR(32) NOT NULL,
    from_entity_id      BIGINT NOT NULL,
    to_entity_type      VARCHAR(32) NOT NULL,
    to_entity_id        BIGINT NOT NULL,
    relation_type       VARCHAR(32) NOT NULL,
    weight              DECIMAL(3,2) NOT NULL DEFAULT 1.00,
    metadata            JSONB,
    source              VARCHAR(32),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    version             INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_kb_relations_from    ON kb_knowledge_relations(from_entity_type, from_entity_id);
CREATE INDEX idx_kb_relations_to      ON kb_knowledge_relations(to_entity_type, to_entity_id);
CREATE INDEX idx_kb_relations_type     ON kb_knowledge_relations(relation_type);
CREATE INDEX idx_kb_relations_metadata_gin ON kb_knowledge_relations USING GIN (metadata);

-- ------------------------------------------------------------
-- kb_images : Images attached to any knowledge entity
-- ------------------------------------------------------------
CREATE TABLE kb_images (
    id              BIGSERIAL PRIMARY KEY,
    entity_type     VARCHAR(32) NOT NULL,
    entity_id       BIGINT NOT NULL,
    url             VARCHAR(1024) NOT NULL,
    url_thumb       VARCHAR(1024),
    caption         VARCHAR(512),
    alt_text        VARCHAR(512),
    width           INTEGER,
    height          INTEGER,
    metadata        JSONB,
    source          VARCHAR(32),
    source_id       VARCHAR(128),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_kb_images_entity       ON kb_images(entity_type, entity_id);
CREATE INDEX idx_kb_images_source       ON kb_images(source, source_id);
CREATE INDEX idx_kb_images_metadata_gin ON kb_images USING GIN (metadata);
