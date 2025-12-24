-- =========================================================
-- OZ ZU - FULL SCHEMA (ALL TABLES + TRIGGERS + SEED)
-- Includes partitioned wagers (HASH on event_id, 64 partitions)
-- Fixes all wager foreign keys to composite (event_id, id)
-- =========================================================

-- -------------------------
-- Extensions
-- -------------------------
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- -------------------------
-- Enums
-- -------------------------
DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'auth_provider') THEN
            CREATE TYPE auth_provider AS ENUM ('APPLE','GOOGLE','FACEBOOK','EMAIL');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'event_status') THEN
            CREATE TYPE event_status AS ENUM ('SCHEDULED','LIVE','COMPLETED','CANCELED');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'wager_status') THEN
            CREATE TYPE wager_status AS ENUM ('CREATED','PLACED','LOCKED','SETTLED','CANCELED');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'wager_outcome') THEN
            CREATE TYPE wager_outcome AS ENUM ('PENDING','WON','LOST','VOID');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'token_txn_type') THEN
            CREATE TYPE token_txn_type AS ENUM (
                'AIRDROP','REFERRAL_BONUS','WATCH_SHORT','SPOTLIGHT',
                'LIFELINE','WAGER_CREATE','LOUNGE_WATCH',
                'WAGER_PAYOUT','WAGER_ENTRY_FEE','ADJUSTMENT'
                );
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'artifact_type') THEN
            CREATE TYPE artifact_type AS ENUM ('SHORT','IMAGE','VIDEO','ARTICLE','LINK');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'reward_trigger_type') THEN
            CREATE TYPE reward_trigger_type AS ENUM (
                'WATCH_SHORTS',
                'WATCH_ARTIFACTS',
                'CREATE_WAGER',
                'LIFELINE_RESPONSE',
                'EVENT_ENGAGEMENT'
                );
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'lounge_member_role') THEN
            CREATE TYPE lounge_member_role AS ENUM ('OWNER','ADMIN','MEMBER');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'lounge_member_status') THEN
            CREATE TYPE lounge_member_status AS ENUM ('INVITED','ACTIVE','INACTIVE','REMOVED','BANNED');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'lounge_notification_type') THEN
            CREATE TYPE lounge_notification_type AS ENUM ('INVITE','REMINDER','ANNOUNCEMENT','SYSTEM');
        END IF;
    END$$;

-- =========================================================
-- Core tables
-- =========================================================

CREATE TABLE IF NOT EXISTS users (
                                     id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                     display_name        text,
                                     email               text,
                                     phone               text,
                                     provider            auth_provider NOT NULL DEFAULT 'EMAIL',
                                     provider_user_id    text,
                                     referral_code       text UNIQUE,
                                     referred_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
                                     created_at          timestamptz NOT NULL DEFAULT now(),
                                     updated_at          timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_provider
    ON users(provider, provider_user_id)
    WHERE provider_user_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email
    ON users(email)
    WHERE email IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_users_referred_by
    ON users(referred_by_user_id);

CREATE TABLE IF NOT EXISTS domains (
                                       id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                       name                text NOT NULL UNIQUE,
                                       description         text,
                                       internal_properties jsonb NOT NULL DEFAULT '{}'::jsonb,
                                       created_at          timestamptz NOT NULL DEFAULT now(),
                                       updated_at          timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS series (
                                      id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                      domain_id           uuid NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
                                      name                text NOT NULL,
                                      description         text,
                                      internal_properties jsonb NOT NULL DEFAULT '{}'::jsonb,
                                      created_at          timestamptz NOT NULL DEFAULT now(),
                                      updated_at          timestamptz NOT NULL DEFAULT now(),
                                      UNIQUE(domain_id, name)
);

CREATE INDEX IF NOT EXISTS ix_series_domain ON series(domain_id);

CREATE TABLE IF NOT EXISTS events (
                                      id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                      domain_id           uuid NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
                                      series_id           uuid REFERENCES series(id) ON DELETE SET NULL,
                                      name                text NOT NULL,
                                      description         text,
                                      status              event_status NOT NULL DEFAULT 'SCHEDULED',
                                      time_event_start    timestamptz,
                                      time_event_end      timestamptz,
                                      is_canceled         boolean NOT NULL DEFAULT false,
                                      is_completed        boolean NOT NULL DEFAULT false,
                                      internal_properties jsonb NOT NULL DEFAULT '{}'::jsonb,
                                      created_at          timestamptz NOT NULL DEFAULT now(),
                                      updated_at          timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_events_domain ON events(domain_id);
CREATE INDEX IF NOT EXISTS ix_events_series ON events(series_id);
CREATE INDEX IF NOT EXISTS ix_events_time ON events(time_event_start DESC);
CREATE INDEX IF NOT EXISTS ix_events_status ON events(status);

CREATE TABLE IF NOT EXISTS players (
                                       id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                       domain_id           uuid NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
                                       name                text NOT NULL,
                                       object_profile      text,
                                       object_status       text,
                                       internal_properties jsonb NOT NULL DEFAULT '{}'::jsonb,
                                       created_at          timestamptz NOT NULL DEFAULT now(),
                                       updated_at          timestamptz NOT NULL DEFAULT now(),
                                       UNIQUE(domain_id, name)
);

CREATE INDEX IF NOT EXISTS ix_players_domain ON players(domain_id);

CREATE TABLE IF NOT EXISTS teams (
                                     id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                     domain_id           uuid NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
                                     series_id           uuid REFERENCES series(id) ON DELETE SET NULL,
                                     name                text NOT NULL,
                                     internal_properties jsonb NOT NULL DEFAULT '{}'::jsonb,
                                     created_at          timestamptz NOT NULL DEFAULT now(),
                                     updated_at          timestamptz NOT NULL DEFAULT now(),
                                     UNIQUE(domain_id, name)
);

CREATE INDEX IF NOT EXISTS ix_teams_domain ON teams(domain_id);
CREATE INDEX IF NOT EXISTS ix_teams_series ON teams(series_id);

CREATE TABLE IF NOT EXISTS team_members (
                                            id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                            team_id     uuid NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
                                            player_id   uuid NOT NULL REFERENCES players(id) ON DELETE CASCADE,
                                            role        text,
                                            created_at  timestamptz NOT NULL DEFAULT now(),
                                            UNIQUE(team_id, player_id)
);

CREATE INDEX IF NOT EXISTS ix_team_members_team ON team_members(team_id);
CREATE INDEX IF NOT EXISTS ix_team_members_player ON team_members(player_id);

CREATE TABLE IF NOT EXISTS event_participants (
                                                  id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                                  domain_id            uuid NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
                                                  event_id             uuid NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                                                  player_id            uuid REFERENCES players(id) ON DELETE SET NULL,
                                                  team_id              uuid REFERENCES teams(id) ON DELETE SET NULL,
                                                  role                text,
                                                  internal_properties jsonb NOT NULL DEFAULT '{}'::jsonb,
                                                  created_at          timestamptz NOT NULL DEFAULT now(),
                                                  CHECK (player_id IS NOT NULL OR team_id IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS ix_event_participants_event ON event_participants(event_id);
CREATE INDEX IF NOT EXISTS ix_event_participants_player ON event_participants(player_id) WHERE player_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_event_participants_team ON event_participants(team_id) WHERE team_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_event_participants_event_player
    ON event_participants(event_id, player_id) WHERE player_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_event_participants_event_team
    ON event_participants(event_id, team_id) WHERE team_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS concept_terms (
                                             id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                             domain_id           uuid NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
                                             name                text NOT NULL,
                                             parent_id           uuid REFERENCES concept_terms(id) ON DELETE SET NULL,
                                             internal_properties jsonb NOT NULL DEFAULT '{}'::jsonb,
                                             created_at          timestamptz NOT NULL DEFAULT now(),
                                             updated_at          timestamptz NOT NULL DEFAULT now(),
                                             UNIQUE(domain_id, name)
);

CREATE INDEX IF NOT EXISTS ix_concept_terms_domain ON concept_terms(domain_id);
CREATE INDEX IF NOT EXISTS ix_concept_terms_parent ON concept_terms(parent_id);

CREATE TABLE IF NOT EXISTS relationships (
                                             id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                             domain_id             uuid NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
                                             name                 text NOT NULL,
                                             is_defining          boolean NOT NULL DEFAULT false,
                                             from_concept_term_id uuid NOT NULL REFERENCES concept_terms(id) ON DELETE RESTRICT,
                                             to_concept_term_id   uuid NOT NULL REFERENCES concept_terms(id) ON DELETE RESTRICT,
                                             internal_properties  jsonb NOT NULL DEFAULT '{}'::jsonb,
                                             created_at           timestamptz NOT NULL DEFAULT now(),
                                             UNIQUE(domain_id, name)
);

CREATE INDEX IF NOT EXISTS ix_relationships_domain ON relationships(domain_id);
CREATE INDEX IF NOT EXISTS ix_relationships_from_concept ON relationships(from_concept_term_id);
CREATE INDEX IF NOT EXISTS ix_relationships_to_concept ON relationships(to_concept_term_id);

CREATE TABLE IF NOT EXISTS scoped_referents (
                                                id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                                domain_id             uuid NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
                                                event_id              uuid NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                                                name                 text,
                                                group_affiliation    text,
                                                is_generated         boolean NOT NULL DEFAULT false,
                                                concept_term_id      uuid NOT NULL REFERENCES concept_terms(id) ON DELETE RESTRICT,

                                                entity_type          text NOT NULL,
                                                player_id            uuid REFERENCES players(id) ON DELETE SET NULL,
                                                team_id              uuid REFERENCES teams(id) ON DELETE SET NULL,
                                                entity_label         text,

                                                points_value         integer NOT NULL DEFAULT 0,
                                                is_optional          boolean NOT NULL DEFAULT true,
                                                is_event_constrained boolean NOT NULL DEFAULT true,
                                                description          text,

                                                internal_properties  jsonb NOT NULL DEFAULT '{}'::jsonb,
                                                created_at           timestamptz NOT NULL DEFAULT now(),
                                                updated_at           timestamptz NOT NULL DEFAULT now(),

                                                CHECK (
                                                    (player_id IS NOT NULL)::int +
                                                    (team_id IS NOT NULL)::int +
                                                    (entity_label IS NOT NULL)::int >= 1
                                                    )
);

CREATE INDEX IF NOT EXISTS ix_scoped_referents_event ON scoped_referents(event_id);
CREATE INDEX IF NOT EXISTS ix_scoped_referents_domain ON scoped_referents(domain_id);
CREATE INDEX IF NOT EXISTS ix_scoped_referents_concept ON scoped_referents(concept_term_id);
CREATE INDEX IF NOT EXISTS ix_scoped_referents_group ON scoped_referents(event_id, group_affiliation);

CREATE UNIQUE INDEX IF NOT EXISTS ux_scoped_ref_event_concept_player
    ON scoped_referents(event_id, concept_term_id, player_id) WHERE player_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_scoped_ref_event_concept_team
    ON scoped_referents(event_id, concept_term_id, team_id) WHERE team_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS wager_card_types (
                                                id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                                domain_id            uuid NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
                                                name                text NOT NULL,
                                                description         text,
                                                internal_properties jsonb NOT NULL DEFAULT '{}'::jsonb,
                                                created_at          timestamptz NOT NULL DEFAULT now(),
                                                updated_at          timestamptz NOT NULL DEFAULT now(),
                                                UNIQUE(domain_id, name)
);

CREATE INDEX IF NOT EXISTS ix_wager_card_types_domain ON wager_card_types(domain_id);

CREATE TABLE IF NOT EXISTS wager_card_type_bindings (
                                                        id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                                        domain_id            uuid NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
                                                        wager_card_type_id   uuid NOT NULL REFERENCES wager_card_types(id) ON DELETE CASCADE,
                                                        concept_term_id      uuid NOT NULL REFERENCES concept_terms(id) ON DELETE RESTRICT,
                                                        description         text,
                                                        internal_properties jsonb NOT NULL DEFAULT '{}'::jsonb,
                                                        created_at          timestamptz NOT NULL DEFAULT now(),
                                                        UNIQUE(wager_card_type_id, concept_term_id)
);

CREATE INDEX IF NOT EXISTS ix_wctb_domain ON wager_card_type_bindings(domain_id);
CREATE INDEX IF NOT EXISTS ix_wctb_card_type ON wager_card_type_bindings(wager_card_type_id);
CREATE INDEX IF NOT EXISTS ix_wctb_concept ON wager_card_type_bindings(concept_term_id);

-- =========================================================
-- WAGERS (Partitioned) - FIXED
-- =========================================================
CREATE TABLE IF NOT EXISTS wagers (
                                      id                  uuid NOT NULL DEFAULT gen_random_uuid(),
                                      domain_id            uuid NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
                                      event_id             uuid NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                                      user_id              uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                      name                text,
                                      status              wager_status NOT NULL DEFAULT 'CREATED',
                                      outcome             wager_outcome NOT NULL DEFAULT 'PENDING',
                                      stake_tokens        integer NOT NULL DEFAULT 0,
                                      payout_tokens       integer NOT NULL DEFAULT 0,
                                      narrative           jsonb NOT NULL DEFAULT '{}'::jsonb,
                                      internal_properties jsonb NOT NULL DEFAULT '{}'::jsonb,
                                      is_celebrity        boolean NOT NULL DEFAULT false,
                                      celebrity_label     text,
                                      created_at          timestamptz NOT NULL DEFAULT now(),
                                      updated_at          timestamptz NOT NULL DEFAULT now(),
                                      CONSTRAINT pk_wagers PRIMARY KEY (event_id, id)
) PARTITION BY HASH (event_id);

DO $$
    DECLARE i int;
    BEGIN
        FOR i IN 0..63 LOOP
                EXECUTE format(
                        'CREATE TABLE IF NOT EXISTS wagers_%s PARTITION OF wagers
                         FOR VALUES WITH (MODULUS 64, REMAINDER %s);',
                        i, i
                        );
            END LOOP;
    END $$;

-- Parent indexes propagate to partitions
CREATE INDEX IF NOT EXISTS ix_wagers_event_time ON wagers (event_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_wagers_event ON wagers(event_id);
CREATE INDEX IF NOT EXISTS ix_wagers_user_time ON wagers(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_wagers_domain_user ON wagers(domain_id, user_id);
CREATE INDEX IF NOT EXISTS ix_wagers_status ON wagers(status);
CREATE INDEX IF NOT EXISTS ix_wagers_outcome ON wagers(outcome);
CREATE UNIQUE INDEX ux_wagers_event_id_id ON wagers(event_id, id);

CREATE INDEX IF NOT EXISTS ix_wagers_celebrity_event
    ON wagers(event_id, is_celebrity)
    WHERE is_celebrity = true;

-- Optional but powerful: append-only status transitions with idempotency
CREATE TABLE IF NOT EXISTS wager_state_events (
                                                  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                                  wager_event_id uuid NOT NULL,
                                                  wager_id      uuid NOT NULL,
                                                  user_id       uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                                  request_id    text NOT NULL, -- idempotency key
                                                  old_status    wager_status,
                                                  new_status    wager_status NOT NULL,
                                                  reason        text,
                                                  metadata      jsonb NOT NULL DEFAULT '{}'::jsonb,
                                                  created_at    timestamptz NOT NULL DEFAULT now(),
                                                  CONSTRAINT fk_wse_wager
                                                      FOREIGN KEY (wager_event_id, wager_id)
                                                          REFERENCES wagers(event_id, id)
                                                          ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_wse_idempotency
    ON wager_state_events(wager_event_id, wager_id, request_id);

CREATE INDEX IF NOT EXISTS ix_wse_wager_time
    ON wager_state_events(wager_event_id, wager_id, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_wse_event_time
    ON wager_state_events(wager_event_id, created_at DESC);

-- Wager cards now reference wagers via composite FK
CREATE TABLE IF NOT EXISTS wager_cards (
                                           id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                           wager_event_id     uuid NOT NULL,
                                           wager_id           uuid NOT NULL,
                                           wager_card_type_id uuid NOT NULL REFERENCES wager_card_types(id) ON DELETE RESTRICT,
                                           created_at         timestamptz NOT NULL DEFAULT now(),
                                           CONSTRAINT fk_wager_cards_wager
                                               FOREIGN KEY (wager_event_id, wager_id)
                                                   REFERENCES wagers(event_id, id)
                                                   ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_wager_cards_wager ON wager_cards(wager_event_id, wager_id);
CREATE INDEX IF NOT EXISTS ix_wager_cards_type ON wager_cards(wager_card_type_id);

CREATE TABLE IF NOT EXISTS wager_card_bindings (
                                                   id                         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                                   wager_card_id               uuid NOT NULL REFERENCES wager_cards(id) ON DELETE CASCADE,
                                                   wager_card_type_binding_id  uuid NOT NULL REFERENCES wager_card_type_bindings(id) ON DELETE RESTRICT,

                                                   scoped_referent_id          uuid REFERENCES scoped_referents(id) ON DELETE SET NULL,

                                                   entity_type                 text,
                                                   player_id                   uuid REFERENCES players(id) ON DELETE RESTRICT,
                                                   team_id                     uuid REFERENCES teams(id) ON DELETE RESTRICT,
                                                   entity_label                text,

                                                   pick_payload                jsonb NOT NULL DEFAULT '{}'::jsonb,
                                                   created_at                  timestamptz NOT NULL DEFAULT now(),

                                                   CHECK (
                                                       scoped_referent_id IS NOT NULL
                                                           OR (
                                                           entity_type IS NOT NULL AND
                                                           (
                                                               (player_id IS NOT NULL)::int +
                                                               (team_id IS NOT NULL)::int +
                                                               (entity_label IS NOT NULL)::int >= 1
                                                               )
                                                           )
                                                       )
);

CREATE INDEX IF NOT EXISTS ix_wcb_wager_card ON wager_card_bindings(wager_card_id);
CREATE INDEX IF NOT EXISTS ix_wcb_type_binding ON wager_card_bindings(wager_card_type_binding_id);
CREATE INDEX IF NOT EXISTS ix_wcb_scoped_ref ON wager_card_bindings(scoped_referent_id) WHERE scoped_referent_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_wcb_player ON wager_card_bindings(player_id) WHERE player_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_wcb_team ON wager_card_bindings(team_id) WHERE team_id IS NOT NULL;

-- =========================================================
-- Lounges + event_lounges + lounge entries
-- =========================================================
CREATE TABLE IF NOT EXISTS lounges (
                                       id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                       domain_id            uuid NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
                                       name                text NOT NULL,
                                       description         text,
                                       owner_user_id       uuid REFERENCES users(id) ON DELETE SET NULL,
                                       internal_properties jsonb NOT NULL DEFAULT '{}'::jsonb,
                                       created_at          timestamptz NOT NULL DEFAULT now(),
                                       updated_at          timestamptz NOT NULL DEFAULT now(),
                                       UNIQUE(domain_id, name)
);

CREATE INDEX IF NOT EXISTS ix_lounges_domain ON lounges(domain_id);
CREATE INDEX IF NOT EXISTS ix_lounges_owner ON lounges(owner_user_id);

CREATE TABLE IF NOT EXISTS event_lounges (
                                             id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                             domain_id            uuid NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
                                             lounge_id            uuid NOT NULL REFERENCES lounges(id) ON DELETE CASCADE,
                                             event_id             uuid NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                                             entry_fee_tokens     integer NOT NULL DEFAULT 0,
                                             is_active            boolean NOT NULL DEFAULT true,
                                             internal_properties  jsonb NOT NULL DEFAULT '{}'::jsonb,
                                             created_at           timestamptz NOT NULL DEFAULT now(),
                                             updated_at           timestamptz NOT NULL DEFAULT now(),
                                             UNIQUE(lounge_id, event_id)
);

CREATE INDEX IF NOT EXISTS ix_event_lounges_event ON event_lounges(event_id);
CREATE INDEX IF NOT EXISTS ix_event_lounges_lounge ON event_lounges(lounge_id);
CREATE INDEX IF NOT EXISTS ix_event_lounges_domain ON event_lounges(domain_id);

CREATE TABLE IF NOT EXISTS lounge_entries (
                                              id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                              event_lounge_id uuid NOT NULL REFERENCES event_lounges(id) ON DELETE CASCADE,
                                              user_id         uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                              joined_at       timestamptz NOT NULL DEFAULT now(),
                                              left_at         timestamptz,
                                              UNIQUE(event_lounge_id, user_id)
);

CREATE INDEX IF NOT EXISTS ix_lounge_entries_lounge ON lounge_entries(event_lounge_id);
CREATE INDEX IF NOT EXISTS ix_lounge_entries_user ON lounge_entries(user_id);

-- wager_in_lounge must reference wagers by composite
CREATE TABLE IF NOT EXISTS wager_in_lounge (
                                               id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                               event_lounge_id uuid NOT NULL REFERENCES event_lounges(id) ON DELETE CASCADE,
                                               wager_event_id  uuid NOT NULL,
                                               wager_id        uuid NOT NULL,
                                               created_at      timestamptz NOT NULL DEFAULT now(),
                                               UNIQUE(event_lounge_id, wager_event_id, wager_id),
                                               CONSTRAINT fk_wager_in_lounge_wager
                                                   FOREIGN KEY (wager_event_id, wager_id)
                                                       REFERENCES wagers(event_id, id)
                                                       ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_wager_in_lounge_lounge ON wager_in_lounge(event_lounge_id);
CREATE INDEX IF NOT EXISTS ix_wager_in_lounge_wager ON wager_in_lounge(wager_event_id, wager_id);

-- =========================================================
-- Event scores
-- =========================================================
CREATE TABLE IF NOT EXISTS event_scores (
                                            id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                            event_id            uuid NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                                            schema_uri         text,
                                            score_json         jsonb NOT NULL,
                                            created_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
                                            created_at         timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_event_scores_event_time ON event_scores(event_id, created_at DESC);

-- =========================================================
-- Token ledger + balance view (composite wager ref)
-- =========================================================
CREATE TABLE IF NOT EXISTS token_ledger (
                                            id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                            user_id         uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                            domain_id       uuid REFERENCES domains(id) ON DELETE SET NULL,
                                            event_id        uuid REFERENCES events(id) ON DELETE SET NULL,

                                            wager_event_id  uuid,
                                            wager_id        uuid,

                                            lounge_id       uuid REFERENCES lounges(id) ON DELETE SET NULL,
                                            event_lounge_id uuid REFERENCES event_lounges(id) ON DELETE SET NULL,
                                            txn_type        token_txn_type NOT NULL,
                                            amount          integer NOT NULL,
                                            reason          text,
                                            metadata        jsonb NOT NULL DEFAULT '{}'::jsonb,
                                            created_at      timestamptz NOT NULL DEFAULT now(),

                                            CONSTRAINT fk_token_ledger_wager
                                                FOREIGN KEY (wager_event_id, wager_id)
                                                    REFERENCES wagers(event_id, id)
                                                    ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS ix_token_ledger_user_time ON token_ledger(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_token_ledger_event ON token_ledger(event_id);
CREATE INDEX IF NOT EXISTS ix_token_ledger_wager ON token_ledger(wager_event_id, wager_id);
CREATE INDEX IF NOT EXISTS ix_token_ledger_lounge ON token_ledger(lounge_id);
CREATE INDEX IF NOT EXISTS ix_token_ledger_event_lounge ON token_ledger(event_lounge_id);

CREATE OR REPLACE VIEW v_user_token_balance AS
SELECT user_id, COALESCE(SUM(amount), 0) AS balance
FROM token_ledger
GROUP BY user_id;

-- =========================================================
-- Sessions
-- =========================================================
CREATE TABLE IF NOT EXISTS user_sessions (
                                             id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                             user_id          uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,

                                             session_token    text NOT NULL UNIQUE,
                                             created_at       timestamptz NOT NULL DEFAULT now(),
                                             last_seen_at     timestamptz NOT NULL DEFAULT now(),
                                             expires_at       timestamptz NOT NULL,
                                             revoked_at       timestamptz,

                                             keep_logged_in   boolean NOT NULL DEFAULT false,

                                             device_info      jsonb NOT NULL DEFAULT '{}'::jsonb,
                                             ip_address       text,
                                             user_agent       text
);

CREATE INDEX IF NOT EXISTS ix_user_sessions_user_active
    ON user_sessions(user_id, expires_at DESC)
    WHERE revoked_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_user_sessions_token
    ON user_sessions(session_token);

-- =========================================================
-- Artifacts + Views
-- =========================================================
CREATE TABLE IF NOT EXISTS artifacts (
                                         id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                         domain_id            uuid NOT NULL REFERENCES domains(id) ON DELETE CASCADE,

                                         event_id             uuid REFERENCES events(id) ON DELETE CASCADE,
                                         lounge_id            uuid REFERENCES lounges(id) ON DELETE CASCADE,
                                         event_lounge_id      uuid REFERENCES event_lounges(id) ON DELETE CASCADE,

                                         type                artifact_type NOT NULL DEFAULT 'SHORT',
                                         title               text,
                                         description         text,
                                         content_uri         text NOT NULL,
                                         thumbnail_uri       text,

                                         content_schema_uri  text,
                                         internal_properties jsonb NOT NULL DEFAULT '{}'::jsonb,

                                         is_active           boolean NOT NULL DEFAULT true,
                                         sort_rank           integer NOT NULL DEFAULT 0,

                                         created_at          timestamptz NOT NULL DEFAULT now(),
                                         updated_at          timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_artifacts_domain ON artifacts(domain_id);
CREATE INDEX IF NOT EXISTS ix_artifacts_event ON artifacts(event_id) WHERE event_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_artifacts_lounge ON artifacts(lounge_id) WHERE lounge_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_artifacts_event_lounge ON artifacts(event_lounge_id) WHERE event_lounge_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_artifacts_active_rank ON artifacts(is_active, sort_rank, created_at DESC);

CREATE TABLE IF NOT EXISTS artifact_views (
                                              id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                              user_id         uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                              artifact_id     uuid NOT NULL REFERENCES artifacts(id) ON DELETE CASCADE,

                                              domain_id       uuid NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
                                              event_id        uuid REFERENCES events(id) ON DELETE SET NULL,
                                              lounge_id       uuid REFERENCES lounges(id) ON DELETE SET NULL,
                                              event_lounge_id uuid REFERENCES event_lounges(id) ON DELETE SET NULL,

                                              viewed_at       timestamptz NOT NULL DEFAULT now(),
                                              watch_seconds   integer NOT NULL DEFAULT 0,
                                              completed       boolean NOT NULL DEFAULT false,

                                              internal_properties jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS ix_artifact_views_user_time ON artifact_views(user_id, viewed_at DESC);
CREATE INDEX IF NOT EXISTS ix_artifact_views_artifact ON artifact_views(artifact_id, viewed_at DESC);
CREATE INDEX IF NOT EXISTS ix_artifact_views_domain ON artifact_views(domain_id, viewed_at DESC);

-- =========================================================
-- Rewards
-- =========================================================
CREATE TABLE IF NOT EXISTS reward_campaigns (
                                                id                    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                                domain_id              uuid NOT NULL REFERENCES domains(id) ON DELETE CASCADE,

                                                event_id               uuid REFERENCES events(id) ON DELETE CASCADE,
                                                lounge_id              uuid REFERENCES lounges(id) ON DELETE CASCADE,
                                                event_lounge_id        uuid REFERENCES event_lounges(id) ON DELETE CASCADE,

                                                name                  text NOT NULL,
                                                trigger_type          reward_trigger_type NOT NULL,

                                                watch_count_required  integer NOT NULL DEFAULT 0,
                                                watch_seconds_required integer NOT NULL DEFAULT 0,
                                                reward_tokens         integer NOT NULL DEFAULT 0,

                                                max_total_claims      integer,
                                                max_claims_per_user   integer NOT NULL DEFAULT 1,

                                                starts_at             timestamptz,
                                                ends_at               timestamptz,
                                                is_active             boolean NOT NULL DEFAULT true,

                                                internal_properties   jsonb NOT NULL DEFAULT '{}'::jsonb,
                                                created_at            timestamptz NOT NULL DEFAULT now(),
                                                updated_at            timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_reward_campaigns_domain_active
    ON reward_campaigns(domain_id, is_active, starts_at, ends_at);

CREATE INDEX IF NOT EXISTS ix_reward_campaigns_event
    ON reward_campaigns(event_id) WHERE event_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_reward_campaigns_event_lounge
    ON reward_campaigns(event_lounge_id) WHERE event_lounge_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS reward_claims (
                                             id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                             reward_campaign_id  uuid NOT NULL REFERENCES reward_campaigns(id) ON DELETE CASCADE,
                                             user_id             uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,

                                             claim_key           text,
                                             claimed_at          timestamptz NOT NULL DEFAULT now(),

                                             tokens_credited     integer NOT NULL DEFAULT 0,
                                             token_ledger_id     uuid REFERENCES token_ledger(id) ON DELETE SET NULL,

                                             context             jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS ix_reward_claims_campaign
    ON reward_claims(reward_campaign_id, claimed_at DESC);

CREATE INDEX IF NOT EXISTS ix_reward_claims_user
    ON reward_claims(user_id, claimed_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS ux_reward_claims_idempotency
    ON reward_claims(reward_campaign_id, user_id, claim_key)
    WHERE claim_key IS NOT NULL;

-- =========================================================
-- Lounge memberships + notifications
-- =========================================================
CREATE TABLE IF NOT EXISTS lounge_memberships (
                                                  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                                  lounge_id    uuid NOT NULL REFERENCES lounges(id) ON DELETE CASCADE,
                                                  user_id      uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,

                                                  role         lounge_member_role NOT NULL DEFAULT 'MEMBER',
                                                  status       lounge_member_status NOT NULL DEFAULT 'INVITED',

                                                  invited_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
                                                  joined_at    timestamptz,
                                                  left_at      timestamptz,

                                                  internal_properties jsonb NOT NULL DEFAULT '{}'::jsonb,
                                                  created_at   timestamptz NOT NULL DEFAULT now(),
                                                  updated_at   timestamptz NOT NULL DEFAULT now(),

                                                  UNIQUE(lounge_id, user_id)
);

CREATE INDEX IF NOT EXISTS ix_lounge_memberships_lounge_status
    ON lounge_memberships(lounge_id, status, role);

CREATE INDEX IF NOT EXISTS ix_lounge_memberships_user
    ON lounge_memberships(user_id);

CREATE INDEX IF NOT EXISTS ix_lounge_memberships_lounge
    ON lounge_memberships(lounge_id);

CREATE INDEX IF NOT EXISTS ix_lounge_memberships_active
    ON lounge_memberships(lounge_id, created_at)
    WHERE status = 'ACTIVE';

CREATE TABLE IF NOT EXISTS lounge_notifications (
                                                    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                                    lounge_id       uuid NOT NULL REFERENCES lounges(id) ON DELETE CASCADE,
                                                    sender_user_id  uuid REFERENCES users(id) ON DELETE SET NULL,

                                                    notification_type lounge_notification_type NOT NULL DEFAULT 'SYSTEM',
                                                    title           text,
                                                    message         text NOT NULL,

                                                    audience        jsonb NOT NULL DEFAULT '{}'::jsonb,
                                                    metadata        jsonb NOT NULL DEFAULT '{}'::jsonb,

                                                    created_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_lounge_notifications_lounge_time
    ON lounge_notifications(lounge_id, created_at DESC);

ALTER TABLE wager_card_bindings
    ADD COLUMN IF NOT EXISTS concept_term_id uuid;

ALTER TABLE wager_card_bindings
    ADD CONSTRAINT fk_wcb_concept_term
        FOREIGN KEY (concept_term_id) REFERENCES concept_terms(id) ON DELETE RESTRICT;

-- =========================================================
-- TRIGGERS / FUNCTIONS
-- =========================================================

-- A) scoped_referents: if player/team present -> must be in event_participants
CREATE OR REPLACE FUNCTION trg_scoped_referents_validate_participants()
    RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.player_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM event_participants ep
            WHERE ep.event_id = NEW.event_id AND ep.player_id = NEW.player_id
        ) THEN
            RAISE EXCEPTION
                'Scoped referent player_id % is not an event participant for event_id %',
                NEW.player_id, NEW.event_id
                USING ERRCODE = '23514';
        END IF;
    END IF;

    IF NEW.team_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM event_participants ep
            WHERE ep.event_id = NEW.event_id AND ep.team_id = NEW.team_id
        ) THEN
            RAISE EXCEPTION
                'Scoped referent team_id % is not an event participant for event_id %',
                NEW.team_id, NEW.event_id
                USING ERRCODE = '23514';
        END IF;
    END IF;

    RETURN NEW;
END$$;

DROP TRIGGER IF EXISTS tr_scoped_referents_validate_participants ON scoped_referents;
CREATE TRIGGER tr_scoped_referents_validate_participants
    BEFORE INSERT OR UPDATE OF event_id, player_id, team_id
    ON scoped_referents
    FOR EACH ROW EXECUTE FUNCTION trg_scoped_referents_validate_participants();

-- B) wager_card_bindings:
--    - resolve wager event from wager_cards.wager_event_id
--    - if scoped_referent_id: same event + concept match
--    - if direct pick: must be in event_participants for that event
CREATE OR REPLACE FUNCTION trg_wager_card_bindings_validate()
    RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_wager_event_id uuid;
    v_wctb_concept_term_id uuid;
    v_sr_event_id uuid;
    v_sr_concept_term_id uuid;
BEGIN
    SELECT wc.wager_event_id
    INTO v_wager_event_id
    FROM wager_cards wc
    WHERE wc.id = NEW.wager_card_id;

    IF v_wager_event_id IS NULL THEN
        RAISE EXCEPTION
            'Invalid wager_card_id %, cannot resolve wager/event',
            NEW.wager_card_id
            USING ERRCODE = '23503';
    END IF;

    SELECT wctb.concept_term_id
    INTO v_wctb_concept_term_id
    FROM wager_card_type_bindings wctb
    WHERE wctb.id = NEW.wager_card_type_binding_id;
    IF v_wctb_concept_term_id IS NULL THEN
        RAISE EXCEPTION
            'Invalid wager_card_type_binding_id %, cannot resolve concept_term_id',
            NEW.wager_card_type_binding_id
            USING ERRCODE = '23503';
    END IF;
    NEW.concept_term_id := v_wctb_concept_term_id;
    IF NEW.scoped_referent_id IS NOT NULL THEN
        SELECT sr.event_id, sr.concept_term_id
        INTO v_sr_event_id, v_sr_concept_term_id
        FROM scoped_referents sr
        WHERE sr.id = NEW.scoped_referent_id;

        IF v_sr_event_id IS NULL THEN
            RAISE EXCEPTION 'Invalid scoped_referent_id %', NEW.scoped_referent_id
                USING ERRCODE = '23503';
        END IF;

        IF v_sr_event_id <> v_wager_event_id THEN
            RAISE EXCEPTION
                'Scoped referent % belongs to event %, but wager is for event %',
                NEW.scoped_referent_id, v_sr_event_id, v_wager_event_id
                USING ERRCODE = '23514';
        END IF;

        IF v_sr_concept_term_id <> v_wctb_concept_term_id THEN
            RAISE EXCEPTION
                'Scoped referent concept_term_id % does not match expected concept_term_id % for wager_card_type_binding_id %',
                v_sr_concept_term_id, v_wctb_concept_term_id, NEW.wager_card_type_binding_id
                USING ERRCODE = '23514';
        END IF;

        RETURN NEW;
    END IF;

    IF NEW.player_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM event_participants ep
            WHERE ep.event_id = v_wager_event_id AND ep.player_id = NEW.player_id
        ) THEN
            RAISE EXCEPTION
                'Picked player_id % is not an event participant for wager event_id %',
                NEW.player_id, v_wager_event_id
                USING ERRCODE = '23514';
        END IF;
    END IF;

    IF NEW.team_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM event_participants ep
            WHERE ep.event_id = v_wager_event_id AND ep.team_id = NEW.team_id
        ) THEN
            RAISE EXCEPTION
                'Picked team_id % is not an event participant for wager event_id %',
                NEW.team_id, v_wager_event_id
                USING ERRCODE = '23514';
        END IF;
    END IF;

    RETURN NEW;
END$$;

CREATE OR REPLACE FUNCTION trg_event_participants_validate_entity_domain()
    RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_event_domain uuid;
    v_team_domain uuid;
    v_player_domain uuid;
BEGIN
    SELECT domain_id INTO v_event_domain FROM events WHERE id = NEW.event_id;
    IF v_event_domain IS NULL THEN
        RAISE EXCEPTION 'Invalid event_id %', NEW.event_id USING ERRCODE='23503';
    END IF;

    IF NEW.team_id IS NOT NULL THEN
        SELECT domain_id INTO v_team_domain FROM teams WHERE id = NEW.team_id;
        IF v_team_domain IS NULL THEN
            RAISE EXCEPTION 'Invalid team_id %', NEW.team_id USING ERRCODE='23503';
        END IF;
        IF v_team_domain <> v_event_domain THEN
            RAISE EXCEPTION 'Team domain_id % != event domain_id %', v_team_domain, v_event_domain USING ERRCODE='23514';
        END IF;
    END IF;

    IF NEW.player_id IS NOT NULL THEN
        SELECT domain_id INTO v_player_domain FROM players WHERE id = NEW.player_id;
        IF v_player_domain IS NULL THEN
            RAISE EXCEPTION 'Invalid player_id %', NEW.player_id USING ERRCODE='23503';
        END IF;
        IF v_player_domain <> v_event_domain THEN
            RAISE EXCEPTION 'Player domain_id % != event domain_id %', v_player_domain, v_event_domain USING ERRCODE='23514';
        END IF;
    END IF;

    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS tr_event_participants_validate_entity_domain ON event_participants;
CREATE TRIGGER tr_event_participants_validate_entity_domain
    BEFORE INSERT OR UPDATE OF event_id, team_id, player_id
    ON event_participants
    FOR EACH ROW EXECUTE FUNCTION trg_event_participants_validate_entity_domain();

DROP TRIGGER IF EXISTS tr_wager_card_bindings_validate ON wager_card_bindings;
CREATE TRIGGER tr_wager_card_bindings_validate
    BEFORE INSERT OR UPDATE OF wager_card_id, wager_card_type_binding_id, scoped_referent_id, player_id, team_id, entity_label
    ON wager_card_bindings
    FOR EACH ROW EXECUTE FUNCTION trg_wager_card_bindings_validate();

-- C) HARDEN: wager_card_type_binding must belong to same wager_card_type as the parent wager_card
CREATE OR REPLACE FUNCTION trg_wager_card_bindings_validate_card_type()
    RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_wc_type_id uuid;
    v_binding_type_id uuid;
BEGIN
    SELECT wc.wager_card_type_id INTO v_wc_type_id
    FROM wager_cards wc
    WHERE wc.id = NEW.wager_card_id;

    IF v_wc_type_id IS NULL THEN
        RAISE EXCEPTION 'Invalid wager_card_id %, cannot resolve wager_card_type_id',
            NEW.wager_card_id
            USING ERRCODE = '23503';
    END IF;

    SELECT wctb.wager_card_type_id INTO v_binding_type_id
    FROM wager_card_type_bindings wctb
    WHERE wctb.id = NEW.wager_card_type_binding_id;

    IF v_binding_type_id IS NULL THEN
        RAISE EXCEPTION 'Invalid wager_card_type_binding_id %, cannot resolve wager_card_type_id',
            NEW.wager_card_type_binding_id
            USING ERRCODE = '23503';
    END IF;

    IF v_wc_type_id <> v_binding_type_id THEN
        RAISE EXCEPTION
            'wager_card_type_binding_id % belongs to wager_card_type_id %, but wager_card_id % is of wager_card_type_id %',
            NEW.wager_card_type_binding_id, v_binding_type_id, NEW.wager_card_id, v_wc_type_id
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END$$;

DROP TRIGGER IF EXISTS tr_wager_card_bindings_validate_card_type ON wager_card_bindings;
CREATE TRIGGER tr_wager_card_bindings_validate_card_type
    BEFORE INSERT OR UPDATE OF wager_card_id, wager_card_type_binding_id
    ON wager_card_bindings
    FOR EACH ROW EXECUTE FUNCTION trg_wager_card_bindings_validate_card_type();

-- D) Domain consistency triggers (fixed for partitioned wagers)
CREATE OR REPLACE FUNCTION trg_wagers_validate_domain_event()
    RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_event_domain uuid;
BEGIN
    SELECT e.domain_id INTO v_event_domain FROM events e WHERE e.id = NEW.event_id;
    IF v_event_domain IS NULL THEN
        RAISE EXCEPTION 'Invalid event_id %', NEW.event_id USING ERRCODE = '23503';
    END IF;

    IF NEW.domain_id <> v_event_domain THEN
        RAISE EXCEPTION
            'wagers.domain_id % does not match events.domain_id % for event_id %',
            NEW.domain_id, v_event_domain, NEW.event_id
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END$$;

DROP TRIGGER IF EXISTS tr_wagers_validate_domain_event ON wagers;
CREATE TRIGGER tr_wagers_validate_domain_event
    BEFORE INSERT OR UPDATE OF domain_id, event_id
    ON wagers
    FOR EACH ROW EXECUTE FUNCTION trg_wagers_validate_domain_event();

CREATE OR REPLACE FUNCTION trg_scoped_referents_validate_domain()
    RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_event_domain uuid;
BEGIN
    SELECT e.domain_id INTO v_event_domain FROM events e WHERE e.id = NEW.event_id;
    IF v_event_domain IS NULL THEN
        RAISE EXCEPTION 'Invalid event_id %', NEW.event_id USING ERRCODE = '23503';
    END IF;

    IF NEW.domain_id <> v_event_domain THEN
        RAISE EXCEPTION
            'scoped_referents.domain_id % does not match events.domain_id % for event_id %',
            NEW.domain_id, v_event_domain, NEW.event_id
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END$$;

DROP TRIGGER IF EXISTS tr_scoped_referents_validate_domain ON scoped_referents;
CREATE TRIGGER tr_scoped_referents_validate_domain
    BEFORE INSERT OR UPDATE OF domain_id, event_id
    ON scoped_referents
    FOR EACH ROW EXECUTE FUNCTION trg_scoped_referents_validate_domain();

CREATE OR REPLACE FUNCTION trg_event_participants_validate_domain()
    RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_event_domain uuid;
BEGIN
    SELECT e.domain_id INTO v_event_domain FROM events e WHERE e.id = NEW.event_id;
    IF v_event_domain IS NULL THEN
        RAISE EXCEPTION 'Invalid event_id %', NEW.event_id USING ERRCODE = '23503';
    END IF;

    IF NEW.domain_id <> v_event_domain THEN
        RAISE EXCEPTION
            'event_participants.domain_id % does not match events.domain_id % for event_id %',
            NEW.domain_id, v_event_domain, NEW.event_id
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END$$;

DROP TRIGGER IF EXISTS tr_event_participants_validate_domain ON event_participants;
CREATE TRIGGER tr_event_participants_validate_domain
    BEFORE INSERT OR UPDATE OF domain_id, event_id
    ON event_participants
    FOR EACH ROW EXECUTE FUNCTION trg_event_participants_validate_domain();

CREATE OR REPLACE FUNCTION trg_event_lounges_validate_domain()
    RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_event_domain uuid;
    DECLARE v_lounge_domain uuid;
BEGIN
    SELECT e.domain_id INTO v_event_domain FROM events e WHERE e.id = NEW.event_id;
    IF v_event_domain IS NULL THEN
        RAISE EXCEPTION 'Invalid event_id %', NEW.event_id USING ERRCODE = '23503';
    END IF;

    SELECT l.domain_id INTO v_lounge_domain FROM lounges l WHERE l.id = NEW.lounge_id;
    IF v_lounge_domain IS NULL THEN
        RAISE EXCEPTION 'Invalid lounge_id %', NEW.lounge_id USING ERRCODE = '23503';
    END IF;

    IF NEW.domain_id <> v_event_domain THEN
        RAISE EXCEPTION
            'event_lounges.domain_id % does not match events.domain_id % for event_id %',
            NEW.domain_id, v_event_domain, NEW.event_id
            USING ERRCODE = '23514';
    END IF;

    IF NEW.domain_id <> v_lounge_domain THEN
        RAISE EXCEPTION
            'event_lounges.domain_id % does not match lounges.domain_id % for lounge_id %',
            NEW.domain_id, v_lounge_domain, NEW.lounge_id
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END$$;

DROP TRIGGER IF EXISTS tr_event_lounges_validate_domain ON event_lounges;
CREATE TRIGGER tr_event_lounges_validate_domain
    BEFORE INSERT OR UPDATE OF domain_id, lounge_id, event_id
    ON event_lounges
    FOR EACH ROW EXECUTE FUNCTION trg_event_lounges_validate_domain();

-- Domain consistency for wager_card_bindings picks (scoped/direct) in same domain as wager/event
CREATE OR REPLACE FUNCTION trg_wager_card_bindings_validate_domain()
    RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_wager_event_id uuid;
    v_wager_id uuid;
    v_wager_domain_id uuid;
    v_event_domain_id uuid;
    v_sr_domain_id uuid;
    v_player_domain uuid;
    v_team_domain uuid;
BEGIN
    -- Resolve wager_event_id + wager_id from wager_cards
    SELECT wc.wager_event_id, wc.wager_id
    INTO v_wager_event_id, v_wager_id
    FROM wager_cards wc
    WHERE wc.id = NEW.wager_card_id;

    IF v_wager_event_id IS NULL OR v_wager_id IS NULL THEN
        RAISE EXCEPTION 'Invalid wager_card_id %, cannot resolve wager/event keys',
            NEW.wager_card_id USING ERRCODE = '23503';
    END IF;

    -- Resolve wager domain via composite join to wagers
    SELECT w.domain_id
    INTO v_wager_domain_id
    FROM wagers w
    WHERE w.event_id = v_wager_event_id AND w.id = v_wager_id;

    IF v_wager_domain_id IS NULL THEN
        RAISE EXCEPTION 'Cannot resolve wager (event_id %, id %) from wager_card_id %',
            v_wager_event_id, v_wager_id, NEW.wager_card_id USING ERRCODE = '23503';
    END IF;

    SELECT e.domain_id INTO v_event_domain_id
    FROM events e
    WHERE e.id = v_wager_event_id;

    IF v_event_domain_id IS NULL THEN
        RAISE EXCEPTION 'Invalid event_id % (from wager_card)', v_wager_event_id USING ERRCODE = '23503';
    END IF;

    IF v_event_domain_id <> v_wager_domain_id THEN
        RAISE EXCEPTION
            'Wager domain_id % does not match event domain_id % (event_id %)',
            v_wager_domain_id, v_event_domain_id, v_wager_event_id
            USING ERRCODE = '23514';
    END IF;

    IF NEW.scoped_referent_id IS NOT NULL THEN
        SELECT sr.domain_id INTO v_sr_domain_id
        FROM scoped_referents sr
        WHERE sr.id = NEW.scoped_referent_id;

        IF v_sr_domain_id IS NULL THEN
            RAISE EXCEPTION 'Invalid scoped_referent_id %', NEW.scoped_referent_id
                USING ERRCODE = '23503';
        END IF;

        IF v_sr_domain_id <> v_wager_domain_id THEN
            RAISE EXCEPTION
                'Scoped referent domain_id % does not match wager/event domain_id %',
                v_sr_domain_id, v_wager_domain_id
                USING ERRCODE = '23514';
        END IF;

        RETURN NEW;
    END IF;

    IF NEW.player_id IS NOT NULL THEN
        SELECT p.domain_id INTO v_player_domain FROM players p WHERE p.id = NEW.player_id;
        IF v_player_domain IS NULL THEN
            RAISE EXCEPTION 'Invalid player_id %', NEW.player_id USING ERRCODE = '23503';
        END IF;
        IF v_player_domain <> v_wager_domain_id THEN
            RAISE EXCEPTION
                'Picked player domain_id % does not match wager/event domain_id %',
                v_player_domain, v_wager_domain_id
                USING ERRCODE = '23514';
        END IF;
    END IF;

    IF NEW.team_id IS NOT NULL THEN
        SELECT t.domain_id INTO v_team_domain FROM teams t WHERE t.id = NEW.team_id;
        IF v_team_domain IS NULL THEN
            RAISE EXCEPTION 'Invalid team_id %', NEW.team_id USING ERRCODE = '23503';
        END IF;
        IF v_team_domain <> v_wager_domain_id THEN
            RAISE EXCEPTION
                'Picked team domain_id % does not match wager/event domain_id %',
                v_team_domain, v_wager_domain_id
                USING ERRCODE = '23514';
        END IF;
    END IF;

    RETURN NEW;
END$$;

DROP TRIGGER IF EXISTS tr_wager_card_bindings_validate_domain ON wager_card_bindings;
CREATE TRIGGER tr_wager_card_bindings_validate_domain
    BEFORE INSERT OR UPDATE OF wager_card_id, scoped_referent_id, player_id, team_id
    ON wager_card_bindings
    FOR EACH ROW EXECUTE FUNCTION trg_wager_card_bindings_validate_domain();

-- Reward caps trigger (kept)
CREATE OR REPLACE FUNCTION trg_reward_claims_enforce_caps()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
DECLARE
    v_max_total integer;
    v_max_user integer;
    v_total_claims integer;
    v_user_claims integer;
BEGIN
    SELECT max_total_claims, max_claims_per_user
    INTO v_max_total, v_max_user
    FROM reward_campaigns
    WHERE id = NEW.reward_campaign_id;

    IF v_max_user IS NOT NULL THEN
        SELECT count(*) INTO v_user_claims
        FROM reward_claims
        WHERE reward_campaign_id = NEW.reward_campaign_id
          AND user_id = NEW.user_id;

        IF v_user_claims >= v_max_user THEN
            RAISE EXCEPTION
                'User % has reached max_claims_per_user % for reward_campaign %',
                NEW.user_id, v_max_user, NEW.reward_campaign_id
                USING ERRCODE = '23514';
        END IF;
    END IF;

    IF v_max_total IS NOT NULL THEN
        SELECT count(*) INTO v_total_claims
        FROM reward_claims
        WHERE reward_campaign_id = NEW.reward_campaign_id;

        IF v_total_claims >= v_max_total THEN
            RAISE EXCEPTION
                'Reward campaign % has reached max_total_claims %',
                NEW.reward_campaign_id, v_max_total
                USING ERRCODE = '23514';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS tr_reward_claims_enforce_caps ON reward_claims;
CREATE TRIGGER tr_reward_claims_enforce_caps
    BEFORE INSERT
    ON reward_claims
    FOR EACH ROW
EXECUTE FUNCTION trg_reward_claims_enforce_caps();

-- Lounge membership set times trigger
CREATE OR REPLACE FUNCTION trg_lounge_memberships_set_times()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := now();

    IF (TG_OP = 'INSERT' AND NEW.status = 'ACTIVE' AND NEW.joined_at IS NULL) THEN
        NEW.joined_at := now();
    ELSIF (TG_OP = 'UPDATE') THEN
        IF (OLD.status IS DISTINCT FROM NEW.status) THEN
            IF NEW.status = 'ACTIVE' AND NEW.joined_at IS NULL THEN
                NEW.joined_at := now();
            END IF;

            IF NEW.status IN ('REMOVED','BANNED') AND NEW.left_at IS NULL THEN
                NEW.left_at := now();
            END IF;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS tr_lounge_memberships_set_times ON lounge_memberships;
CREATE TRIGGER tr_lounge_memberships_set_times
    BEFORE INSERT OR UPDATE OF status, role, internal_properties
    ON lounge_memberships
    FOR EACH ROW
EXECUTE FUNCTION trg_lounge_memberships_set_times();

-- Only one OWNER per lounge
CREATE OR REPLACE FUNCTION trg_lounge_memberships_one_owner()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.role = 'OWNER' THEN
        IF EXISTS (
            SELECT 1
            FROM lounge_memberships lm
            WHERE lm.lounge_id = NEW.lounge_id
              AND lm.role = 'OWNER'
              AND lm.id <> COALESCE(NEW.id, gen_random_uuid())
              AND lm.status <> 'REMOVED'
        ) THEN
            RAISE EXCEPTION 'Only one OWNER allowed per lounge (lounge_id=%)', NEW.lounge_id
                USING ERRCODE = '23514';
        END IF;

        IF NEW.status IN ('REMOVED','BANNED') THEN
            RAISE EXCEPTION 'OWNER cannot be REMOVED/BANNED (lounge_id=%)', NEW.lounge_id
                USING ERRCODE = '23514';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS tr_lounge_memberships_one_owner ON lounge_memberships;
CREATE TRIGGER tr_lounge_memberships_one_owner
    BEFORE INSERT OR UPDATE OF role, status
    ON lounge_memberships
    FOR EACH ROW
EXECUTE FUNCTION trg_lounge_memberships_one_owner();

-- Auto-create event lounges for default lounges
CREATE OR REPLACE FUNCTION trg_auto_create_event_lounges_for_defaults()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO event_lounges (
        id, domain_id, lounge_id, event_id, entry_fee_tokens, is_active, internal_properties, created_at, updated_at
    )
    SELECT
        gen_random_uuid(),
        NEW.domain_id,
        l.id,
        NEW.id,
        0,
        true,
        jsonb_build_object('autoCreated', true, 'seedVersion','v1'),
        now(),
        now()
    FROM lounges l
    WHERE l.domain_id = NEW.domain_id
      AND COALESCE((l.internal_properties->>'isDefault')::boolean, false) = true
    ON CONFLICT (lounge_id, event_id) DO NOTHING;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS tr_auto_create_event_lounges_for_defaults ON events;
CREATE TRIGGER tr_auto_create_event_lounges_for_defaults
    AFTER INSERT
    ON events
    FOR EACH ROW
EXECUTE FUNCTION trg_auto_create_event_lounges_for_defaults();

-- Auto-join new users to default lounges (autoJoin=true)
CREATE OR REPLACE FUNCTION trg_auto_join_user_to_default_lounges()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    -- Skip system user
    IF NEW.provider = 'EMAIL' AND NEW.provider_user_id = 'ozzu_system' THEN
        RETURN NEW;
    END IF;

    INSERT INTO lounge_memberships (
        id, lounge_id, user_id, role, status, invited_by_user_id, joined_at, internal_properties, created_at, updated_at
    )
    SELECT
        gen_random_uuid(),
        l.id,
        NEW.id,
        'MEMBER',
        'ACTIVE',
        NULL,
        now(),
        jsonb_build_object('autoJoined', true, 'seedVersion','v1'),
        now(),
        now()
    FROM lounges l
    WHERE COALESCE((l.internal_properties->>'autoJoin')::boolean, false) = true
    ON CONFLICT (lounge_id, user_id) DO NOTHING;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS tr_auto_join_user_to_default_lounges ON users;
CREATE TRIGGER tr_auto_join_user_to_default_lounges
    AFTER INSERT
    ON users
    FOR EACH ROW
EXECUTE FUNCTION trg_auto_join_user_to_default_lounges();