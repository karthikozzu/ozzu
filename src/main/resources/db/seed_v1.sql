CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO domains (id, name, description, internal_properties, created_at, updated_at)
VALUES (
  gen_random_uuid(),
  'Cricket',
  'Cricket domain (matches, players, wagers)',
  jsonb_build_object('seedVersion','v1','sport','cricket'),
  now(),
  now()
)
ON CONFLICT (name) DO NOTHING;

INSERT INTO users (
  id,
  display_name,
  provider,
  provider_user_id,
  created_at,
  updated_at
)
VALUES (
  gen_random_uuid(),
  'Ozzu System',
  'EMAIL',
  'ozzu_system',
  now(),
  now()
);

WITH d AS (
  SELECT id AS domain_id FROM domains WHERE name = 'Cricket' LIMIT 1
),
u AS (
  SELECT id AS user_id FROM users WHERE provider = 'EMAIL' AND provider_user_id = 'ozzu_system' LIMIT 1
)
INSERT INTO lounges (
  id, domain_id, name, description, owner_user_id, internal_properties, created_at, updated_at
)
SELECT
  gen_random_uuid(),
  d.domain_id,
  'Ozzu Lounge',
  'Official Ozzu community lounge (default)',
  u.user_id,
  jsonb_build_object(
    'isDefault', true,
    'autoJoin', true,
    'systemLounge', true,
    'showLeaderboards', true,
    'showOtherUsersScores', false,
    'seedVersion','v1'
  ),
  now(),
  now()
FROM d, u
ON CONFLICT (domain_id, name) DO NOTHING;


WITH l AS (
  SELECT id AS lounge_id
  FROM lounges
  WHERE name = 'Ozzu Lounge'
  ORDER BY created_at ASC
  LIMIT 1
),
u AS (
  SELECT id AS user_id
  FROM users
  WHERE provider = 'EMAIL' AND provider_user_id = 'ozzu_system'
  LIMIT 1
)
INSERT INTO lounge_memberships (
  id, lounge_id, user_id, role, status, invited_by_user_id, joined_at, internal_properties, created_at, updated_at
)
SELECT
  gen_random_uuid(),
  l.lounge_id,
  u.user_id,
  'OWNER',
  'ACTIVE',
  NULL,
  now(),
  jsonb_build_object('seedVersion','v1'),
  now(),
  now()
FROM l, u
ON CONFLICT (lounge_id, user_id) DO NOTHING;


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
    AND COALESCE((l.internal_properties->>'isDefault')::boolean, false) = true;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS tr_auto_create_event_lounges_for_defaults ON events;

CREATE TRIGGER tr_auto_create_event_lounges_for_defaults
AFTER INSERT
ON events
FOR EACH ROW
EXECUTE FUNCTION trg_auto_create_event_lounges_for_defaults();


CREATE OR REPLACE FUNCTION trg_auto_join_user_to_default_lounges()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  -- Skip system user (identified by provider + provider_user_id)
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