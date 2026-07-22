CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(128) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname      VARCHAR(64),
    status        SMALLINT     NOT NULL DEFAULT 1,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version       INT          NOT NULL DEFAULT 0,
    CONSTRAINT uk_users_email UNIQUE (email)
);
CREATE INDEX idx_users_email ON users(email);

CREATE TABLE trips (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT NOT NULL,
    title              VARCHAR(128) NOT NULL,
    description        TEXT,
    destination_name   VARCHAR(64),
    status             SMALLINT NOT NULL DEFAULT 0,
    start_date         DATE,
    end_date           DATE,
    budget             INTEGER,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    version            INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_trips_user_id ON trips(user_id);
CREATE INDEX idx_trips_status ON trips(status);

CREATE TABLE trip_days (
    id           BIGSERIAL PRIMARY KEY,
    trip_id      BIGINT NOT NULL,
    day_number   INT NOT NULL,
    date         DATE,
    title        VARCHAR(128),
    description  TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    version      INT NOT NULL DEFAULT 0,
    CONSTRAINT uk_trip_days_trip_number UNIQUE (trip_id, day_number)
);
CREATE INDEX idx_trip_days_trip_id ON trip_days(trip_id);

CREATE TABLE trip_activities (
    id           BIGSERIAL PRIMARY KEY,
    trip_day_id  BIGINT NOT NULL,
    name         VARCHAR(128) NOT NULL,
    description  TEXT,
    start_time   TIME,
    end_time     TIME,
    category     VARCHAR(32),
    place        VARCHAR(128),
    notes        TEXT,
    sort_order   INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    version      INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_trip_activities_day_id ON trip_activities(trip_day_id);

CREATE TABLE destinations (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    country     VARCHAR(64),
    region      VARCHAR(64),
    category    VARCHAR(32),
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    version     INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_destinations_country ON destinations(country);
CREATE INDEX idx_destinations_category ON destinations(category);

CREATE TABLE conversations (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    title            VARCHAR(128) NOT NULL DEFAULT 'New Conversation',
    status           SMALLINT NOT NULL DEFAULT 0,
    last_message_at  TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    version          INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_conversations_user_id ON conversations(user_id);
CREATE INDEX idx_conversations_status ON conversations(status);

CREATE TABLE conversation_messages (
    id               BIGSERIAL PRIMARY KEY,
    conversation_id  BIGINT NOT NULL,
    user_id          BIGINT NOT NULL,
    role             VARCHAR(16) NOT NULL,
    content          TEXT NOT NULL,
    metadata         JSONB,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    version          INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_conversation_messages_conversation_id ON conversation_messages(conversation_id);

CREATE TABLE workflow_sessions (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    error_message   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ
);
CREATE INDEX idx_workflow_sessions_conversation ON workflow_sessions(conversation_id);
CREATE INDEX idx_workflow_sessions_status ON workflow_sessions(status);

CREATE TABLE workflow_steps (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT NOT NULL,
    agent_name      VARCHAR(64) NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    input_context   TEXT,
    output_result   TEXT,
    error_message   TEXT,
    iteration       INT NOT NULL DEFAULT 1,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_workflow_steps_session ON workflow_steps(session_id);
CREATE INDEX idx_workflow_steps_agent ON workflow_steps(agent_name);

CREATE TABLE trip_experiences (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    trip_id         BIGINT,
    trip_day_id     BIGINT,
    trip_activity_id BIGINT,
    title           VARCHAR(200)  NOT NULL,
    content         TEXT,
    rating          SMALLINT      CHECK (rating >= 1 AND rating <= 5),
    tags            VARCHAR(500),
    media_urls      TEXT,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PUBLISHED',
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    version         INTEGER       NOT NULL DEFAULT 0
);
CREATE INDEX idx_experiences_user_id ON trip_experiences(user_id);
CREATE INDEX idx_experiences_trip_id ON trip_experiences(trip_id);
CREATE INDEX idx_experiences_trip_day_id ON trip_experiences(trip_day_id);
CREATE INDEX idx_experiences_status ON trip_experiences(status);

CREATE TABLE user_preferences (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT         NOT NULL,
    category    VARCHAR(50)    NOT NULL,
    data        JSONB          NOT NULL DEFAULT '{}',
    source      VARCHAR(20)    NOT NULL DEFAULT 'MANUAL',
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP      NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_user_preferences_user_id ON user_preferences(user_id);
CREATE INDEX idx_user_preferences_category ON user_preferences(category);

CREATE TABLE trip_memories (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT         NOT NULL,
    trip_id     BIGINT         NOT NULL,
    memory_type VARCHAR(50)    NOT NULL,
    content     TEXT           NOT NULL,
    tags        VARCHAR(500),
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_trip_memories_user_id ON trip_memories(user_id);
CREATE INDEX idx_trip_memories_trip_id ON trip_memories(trip_id);
CREATE INDEX idx_trip_memories_memory_type ON trip_memories(memory_type);

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS vector_store (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content     TEXT NOT NULL,
    metadata    JSONB NOT NULL DEFAULT '{}',
    embedding   vector(768) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_vector_store_embedding
    ON vector_store USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_vector_store_metadata_doc_type
    ON vector_store ((metadata->>'doc_type'));
CREATE INDEX IF NOT EXISTS idx_vector_store_metadata_user_id
    ON vector_store ((metadata->>'user_id'));
CREATE INDEX IF NOT EXISTS idx_vector_store_metadata_destination
    ON vector_store ((metadata->>'destination'));

CREATE TABLE trip_shares (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    owner_user_id BIGINT NOT NULL,
    share_token VARCHAR(64) NOT NULL UNIQUE,
    share_type VARCHAR(20) NOT NULL DEFAULT 'VIEW',
    max_views INTEGER,
    current_views INTEGER DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    version INTEGER DEFAULT 0
);
CREATE INDEX idx_trip_shares_token ON trip_shares(share_token);
CREATE INDEX idx_trip_shares_trip ON trip_shares(trip_id);

CREATE TABLE price_monitors (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    trip_id BIGINT,
    destination VARCHAR(255) NOT NULL,
    monitor_type VARCHAR(20) NOT NULL DEFAULT 'FLIGHT',
    target_price DECIMAL(10, 2),
    current_price DECIMAL(10, 2),
    lowest_price DECIMAL(10, 2),
    price_history JSONB,
    notification_sent BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    version INTEGER DEFAULT 0,
    departure VARCHAR(64),
    arrival VARCHAR(64),
    ticket_class VARCHAR(32),
    departure_time TIME,
    arrival_time TIME
);
CREATE INDEX idx_price_monitors_user ON price_monitors(user_id);
CREATE INDEX idx_price_monitors_status ON price_monitors(status);

CREATE TABLE multimodal_uploads (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    file_size BIGINT,
    storage_path VARCHAR(500) NOT NULL,
    recognition_result JSONB,
    generated_trip_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    version INTEGER DEFAULT 0
);
CREATE INDEX idx_multimodal_uploads_user ON multimodal_uploads(user_id);
CREATE INDEX idx_multimodal_uploads_status ON multimodal_uploads(status);

CREATE TABLE community_posts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    destination VARCHAR(100),
    tags JSONB,
    media_urls JSONB,
    view_count INTEGER DEFAULT 0,
    like_count INTEGER DEFAULT 0,
    comment_count INTEGER DEFAULT 0,
    favorite_count INTEGER DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    version INTEGER DEFAULT 0
);
CREATE INDEX idx_community_posts_user ON community_posts(user_id);
CREATE INDEX idx_community_posts_destination ON community_posts(destination);
CREATE INDEX idx_community_posts_created ON community_posts(created_at DESC);

CREATE TABLE community_comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT,
    content TEXT NOT NULL,
    like_count INTEGER DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    version INTEGER DEFAULT 0
);
CREATE INDEX idx_community_comments_post ON community_comments(post_id);
CREATE INDEX idx_community_comments_parent ON community_comments(parent_id);

CREATE TABLE community_likes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(user_id, target_id, target_type)
);
CREATE INDEX idx_community_likes_target ON community_likes(target_id, target_type);
CREATE INDEX idx_community_likes_user ON community_likes(user_id);

CREATE TABLE community_favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(user_id, post_id)
);
CREATE INDEX idx_community_favorites_user ON community_favorites(user_id);
CREATE INDEX idx_community_favorites_post ON community_favorites(post_id);

CREATE TABLE user_behaviors (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    behavior_type VARCHAR(30) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT,
    context JSONB,
    weight INTEGER DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_user_behaviors_user ON user_behaviors(user_id);
CREATE INDEX idx_user_behaviors_type ON user_behaviors(behavior_type);
CREATE INDEX idx_user_behaviors_target ON user_behaviors(target_type, target_id);
CREATE INDEX idx_user_behaviors_created ON user_behaviors(created_at DESC);

CREATE TABLE travel_teams (
    id BIGSERIAL PRIMARY KEY,
    creator_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    destination VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    team_type VARCHAR(20) NOT NULL DEFAULT 'BUDDY',
    interests JSONB,
    max_members INTEGER NOT NULL DEFAULT 4,
    current_members INTEGER NOT NULL DEFAULT 1,
    gender_requirement VARCHAR(10) DEFAULT 'ANY',
    min_age INTEGER,
    max_age INTEGER,
    contact VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    version INTEGER DEFAULT 0
);
CREATE INDEX idx_travel_teams_creator ON travel_teams(creator_id);
CREATE INDEX idx_travel_teams_destination ON travel_teams(destination);
CREATE INDEX idx_travel_teams_status ON travel_teams(status);
CREATE INDEX idx_travel_teams_dates ON travel_teams(start_date, end_date);

CREATE TABLE team_applications (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processed_at TIMESTAMP WITH TIME ZONE,
    processed_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    version INTEGER DEFAULT 0,
    UNIQUE(team_id, applicant_id)
);
CREATE INDEX idx_team_applications_team ON team_applications(team_id);
CREATE INDEX idx_team_applications_applicant ON team_applications(applicant_id);
CREATE INDEX idx_team_applications_status ON team_applications(status);

CREATE TABLE team_members (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(team_id, user_id)
);
CREATE INDEX idx_team_members_team ON team_members(team_id);
CREATE INDEX idx_team_members_user ON team_members(user_id);

CREATE TABLE trip_checkins (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    trip_id BIGINT NOT NULL,
    trip_day_id BIGINT,
    activity_id BIGINT,
    place_name VARCHAR(255),
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    notes TEXT,
    photo_urls JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'CHECKED_IN',
    checked_in_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    version INTEGER DEFAULT 0
);
CREATE INDEX idx_trip_checkins_user ON trip_checkins(user_id);
CREATE INDEX idx_trip_checkins_trip ON trip_checkins(trip_id);
CREATE INDEX idx_trip_checkins_activity ON trip_checkins(activity_id);
CREATE INDEX idx_trip_checkins_user_date ON trip_checkins(user_id, checked_in_at DESC);

CREATE TABLE train_ticket_info (
    id BIGSERIAL PRIMARY KEY,
    train_number VARCHAR(50) NOT NULL,
    departure_station VARCHAR(100) NOT NULL,
    arrival_station VARCHAR(100) NOT NULL,
    departure_time TIME NOT NULL,
    arrival_time TIME NOT NULL,
    ticket_class VARCHAR(32) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    version INTEGER DEFAULT 0
);
CREATE INDEX idx_train_ticket_info_train_number ON train_ticket_info(train_number);
CREATE INDEX idx_train_ticket_info_route ON train_ticket_info(departure_station, arrival_station);

CREATE TABLE trip_generation_tasks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    prompt TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    progress INT NOT NULL DEFAULT 0,
    progress_message VARCHAR(255),
    conversation_id BIGINT,
    trip_id BIGINT,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_tgt_user_status ON trip_generation_tasks(user_id, status);
CREATE INDEX idx_tgt_status ON trip_generation_tasks(status);
