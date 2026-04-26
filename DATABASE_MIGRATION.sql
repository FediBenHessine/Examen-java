-- ============================================
-- DATABASE MIGRATION FOR UNDO/REDO WITH USER TRACKING
-- ============================================
-- Run this SQL to update your database with the new features:
-- - Add username column to draw_commands table
-- - Enable per-user undo/redo tracking
-- - Support for new drawing tools (PEN, ERASER, DELETE)

-- ✅ Step 1: Add username column to draw_commands table (if it doesn't exist)
ALTER TABLE draw_commands
ADD COLUMN username VARCHAR(255) DEFAULT 'UNKNOWN' NOT NULL AFTER tool_name;

-- ✅ Step 2: Add index for faster queries by username + session
ALTER TABLE draw_commands
ADD INDEX idx_session_username (session_id, username);

-- ✅ Step 3: Set existing commands to 'SYSTEM' to distinguish them
UPDATE draw_commands
SET username = 'SYSTEM'
WHERE username = 'UNKNOWN' AND cmd_type NOT IN ('UNDO', 'REDO', 'DELETE');

-- ============================================
-- VERIFICATION QUERIES
-- ============================================
-- Use these to verify the migration worked:

-- Check the structure of draw_commands table:
-- DESCRIBE draw_commands;

-- Check draw commands by user and session:
-- SELECT username, cmd_type, COUNT(*) as count
-- FROM draw_commands
-- WHERE session_id = 1
-- GROUP BY username, cmd_type;

-- ============================================
-- SAMPLE QUERIES FOR NEW FEATURES
-- ============================================

-- Get all draw commands for a specific user in a session:
-- SELECT * FROM draw_commands
-- WHERE session_id = 1 AND username = 'player1'
-- ORDER BY executed_at ASC;

-- Get recent undo/redo operations:
-- SELECT username, cmd_type, executed_at
-- FROM draw_commands
-- WHERE session_id = 1 AND cmd_type IN ('UNDO', 'REDO')
-- ORDER BY executed_at DESC LIMIT 10;

-- Count active drawing tools used per user:
-- SELECT username, cmd_type, COUNT(*) as count
-- FROM draw_commands
-- WHERE session_id = 1 AND cmd_type IN ('LINE', 'PEN', 'ERASER', 'DELETE')
-- GROUP BY username, cmd_type;

