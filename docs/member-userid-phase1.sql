ALTER TABLE fp_103 ADD COLUMN IF NOT EXISTS user_id INTEGER;
ALTER TABLE fp_105 ADD COLUMN IF NOT EXISTS user_id INTEGER;

ALTER TABLE fp_150 ADD COLUMN IF NOT EXISTS user_id INTEGER;
ALTER TABLE fp_150 ADD COLUMN IF NOT EXISTS friend_user_id INTEGER;
ALTER TABLE fp_150 ADD COLUMN IF NOT EXISTS initiator_user_id INTEGER;

ALTER TABLE fp_160 ADD COLUMN IF NOT EXISTS blocker_user_id INTEGER;
ALTER TABLE fp_160 ADD COLUMN IF NOT EXISTS blocked_user_id INTEGER;

ALTER TABLE fp_40 ADD COLUMN IF NOT EXISTS reporter_user_id INTEGER;
ALTER TABLE fp_40 ADD COLUMN IF NOT EXISTS target_user_id INTEGER;

ALTER TABLE fp_200 ADD COLUMN IF NOT EXISTS user_id INTEGER;
ALTER TABLE fp_200 ADD COLUMN IF NOT EXISTS friend_user_id INTEGER;

UPDATE fp_103 t
SET user_id = u.user_id
FROM fp_100 u
WHERE t.user_id IS NULL
  AND t.username = u.username;

UPDATE fp_105 t
SET user_id = u.user_id
FROM fp_100 u
WHERE t.user_id IS NULL
  AND t.username = u.username;

UPDATE fp_150 t
SET user_id = u.user_id
FROM fp_100 u
WHERE t.user_id IS NULL
  AND t.username = u.username;

UPDATE fp_150 t
SET friend_user_id = u.user_id
FROM fp_100 u
WHERE t.friend_user_id IS NULL
  AND t.friend_name = u.username;

UPDATE fp_150 t
SET initiator_user_id = u.user_id
FROM fp_100 u
WHERE t.initiator_user_id IS NULL
  AND t.initiator_username = u.username;

UPDATE fp_160 t
SET blocker_user_id = u.user_id
FROM fp_100 u
WHERE t.blocker_user_id IS NULL
  AND t.blocker_username = u.username;

UPDATE fp_160 t
SET blocked_user_id = u.user_id
FROM fp_100 u
WHERE t.blocked_user_id IS NULL
  AND t.blocked_username = u.username;

UPDATE fp_40 t
SET reporter_user_id = u.user_id
FROM fp_100 u
WHERE t.reporter_user_id IS NULL
  AND t.reporter_username = u.username;

UPDATE fp_40 t
SET target_user_id = u.user_id
FROM fp_100 u
WHERE t.target_user_id IS NULL
  AND t.target_username = u.username;

UPDATE fp_200 t
SET user_id = u.user_id
FROM fp_100 u
WHERE t.user_id IS NULL
  AND t.username = u.username;

UPDATE fp_200 t
SET friend_user_id = u.user_id
FROM fp_100 u
WHERE t.friend_user_id IS NULL
  AND t.friend_name = u.username;

CREATE INDEX IF NOT EXISTS idx_fp_103_user_id ON fp_103(user_id);
CREATE INDEX IF NOT EXISTS idx_fp_105_user_id ON fp_105(user_id);
CREATE INDEX IF NOT EXISTS idx_fp_150_user_id ON fp_150(user_id);
CREATE INDEX IF NOT EXISTS idx_fp_150_friend_user_id ON fp_150(friend_user_id);
CREATE INDEX IF NOT EXISTS idx_fp_150_initiator_user_id ON fp_150(initiator_user_id);
CREATE INDEX IF NOT EXISTS idx_fp_160_blocker_user_id ON fp_160(blocker_user_id);
CREATE INDEX IF NOT EXISTS idx_fp_160_blocked_user_id ON fp_160(blocked_user_id);
CREATE INDEX IF NOT EXISTS idx_fp_40_reporter_user_id ON fp_40(reporter_user_id);
CREATE INDEX IF NOT EXISTS idx_fp_40_target_user_id ON fp_40(target_user_id);
CREATE INDEX IF NOT EXISTS idx_fp_200_user_id ON fp_200(user_id);
CREATE INDEX IF NOT EXISTS idx_fp_200_friend_user_id ON fp_200(friend_user_id);
