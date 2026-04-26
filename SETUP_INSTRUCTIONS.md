# ✅ Setup Instructions - Undo/Redo & Drawing Tools

## 📋 Prerequisites
- Java 11+
- MySQL Server running
- Whiteboard application built and running
- Access to MySQL database

## 🔧 Step 1: Database Migration

### Option A: Run SQL Migration Script
Execute the `DATABASE_MIGRATION.sql` file in your MySQL client:

```bash
# Using MySQL command line
mysql -u your_user -p your_database < DATABASE_MIGRATION.sql

# Or run individual commands in MySQL Workbench
```

### Option B: Manual SQL Commands
Connect to your MySQL database and run:

```sql
-- Add username column to track who did each command
ALTER TABLE draw_commands 
ADD COLUMN username VARCHAR(255) DEFAULT 'UNKNOWN' NOT NULL 
AFTER tool_name;

-- Create index for faster queries
ALTER TABLE draw_commands 
ADD INDEX idx_session_username (session_id, username);

-- Update existing records
UPDATE draw_commands 
SET username = 'SYSTEM' 
WHERE username = 'UNKNOWN' AND cmd_type NOT IN ('UNDO', 'REDO', 'DELETE');
```

### Verify Migration Success
```sql
-- Check table structure
DESCRIBE draw_commands;
-- Should show: username | varchar(255) | NO

-- Check indexes
SHOW INDEXES FROM draw_commands;
-- Should show: idx_session_username on (session_id, username)
```

## 🔨 Step 2: Rebuild Application

### Recompile Java Files
```bash
# Navigate to project directory
cd C:\Firas\1er Ingenieur\JOO\Examen-java

# Compile with UTF-8 support (for emoji characters)
javac -encoding UTF-8 -d bin ^
  src/Model/DrawCommand.java ^
  src/Network/CommandProtocol.java ^
  src/IHM/CanvasPanel.java ^
  src/IHM/WhiteboardFrame.java ^
  src/Database/DatabaseManager.java ^
  src/IHM/DashboardFrame.java ^
  src/IHM/LoginDialog.java ^
  src/IHM/*.java ^
  src/auth/*.java ^
  src/Database/*.java ^
  src/Network/*.java ^
  src/RMI/*.java ^
  src/Main.java
```

### Or Using IDE
1. Right-click project → Build Project (IntelliJ IDEA)
2. Or Maven: `mvn clean compile`
3. Check for compilation errors in console

## 🎯 Step 3: Verify All Changes

### Check Modified Files
- ✅ `src/Model/DrawCommand.java` - Added `username` field
- ✅ `src/Network/CommandProtocol.java` - Updated serialization
- ✅ `src/Database/DatabaseManager.java` - Updated DB operations
- ✅ `src/IHM/CanvasPanel.java` - Complete rewrite with tools
- ✅ `src/IHM/WhiteboardFrame.java` - Added tool buttons

### Verify in Database
```sql
-- Check the migration worked
SELECT COUNT(*) as total_commands, 
       COUNT(DISTINCT username) as unique_users 
FROM draw_commands;

-- Should show username column
SELECT * FROM draw_commands LIMIT 1 \G
```

## 🚀 Step 4: Run Application

### Start Application
```bash
# From project directory
java -cp bin Main

# Or if using Maven
mvn exec:java -Dexec.mainClass="Main"
```

### Test Features

#### 1. Test Login
```
Username: admin
Password: admin123
```

#### 2. Create/Join Room
- Host a test room OR
- Join an existing room

#### 3. Test Drawing Tools
- **Line Tool (📏)**: Click and drag to draw straight lines
- **Pen Tool (✏️)**: Click and drag for freehand drawing
- **Erase Tool (🧹)**: Click and drag to erase with white cover
- **Delete Tool (🗑️)**: Click once to remove strokes

#### 4. Test Undo/Redo
```
1. Draw something (line, circle, etc.)
   → ↶ Undo button becomes enabled
2. Click ↶ Undo
   → Drawing disappears
   → ↷ Redo button becomes enabled
3. Click ↷ Redo
   → Drawing reappears
```

#### 5. Test Multi-User
1. Open two instances of the application
2. User A creates a room
3. User B joins that room
4. User A draws something
5. Both see the same drawing
6. User A clicks Undo → Only A's drawing affected
7. User B can still draw independently
8. User A's undo doesn't affect B's drawings

## 🔍 Troubleshooting

### Issue: "username column not found" Error
**Solution**: Run the DATABASE_MIGRATION.sql script
```sql
ALTER TABLE draw_commands 
ADD COLUMN username VARCHAR(255) DEFAULT 'UNKNOWN' NOT NULL 
AFTER tool_name;
```

### Issue: "Cannot find symbol: PEN" during compilation
**Solution**: Ensure you're using the updated DrawCommand.java with:
```java
enum Type { LINE, PEN, ERASER, RECT, CIRCLE, ... }
```

### Issue: Undo button always disabled
**Solution**: 
1. Check that you've drawn at least one stroke
2. Verify canvas.setCurrentUsername() is being called
3. Check console for errors

### Issue: Pen tool not drawing anything
**Solution**:
1. Use freehand motion (not just a single click)
2. Check that tool is selected (button should be highlighted)
3. Try increasing stroke width for visibility

### Issue: Delete tool removes wrong stroke
**Solution**:
1. Click more precisely on the stroke center
2. Try strokes with thicker width (easier to click)
3. Use Undo if you delete the wrong stroke

### Issue: Drawings not syncing between users
**Solution**:
1. Verify both users are in the same room
2. Check network connectivity
3. Look for "PING/PONG" messages in console
4. Restart the application

## 📊 Monitoring & Debugging

### Check Console Output
Look for these messages indicating successful operation:

```
✅ Canvas initialized successfully
🔧 Current Tool: LINE (or PEN, ERASER, DELETE)
📍 Drawing command: LINE|100|100|200|200|#000000|2.0|username
↶ Undo: Canvas reverted to previous state
↷ Redo: Undone action restored
```

### Query Database for Activity
```sql
-- See what each user draws
SELECT username, cmd_type, COUNT(*) as count 
FROM draw_commands 
WHERE session_id = 1
GROUP BY username, cmd_type 
ORDER BY username;

-- Check for undo/redo activity
SELECT username, cmd_type, executed_at 
FROM draw_commands 
WHERE session_id = 1 AND cmd_type IN ('UNDO', 'REDO')
ORDER BY executed_at DESC;

-- Verify pen strokes are stored
SELECT username, cmd_type, LENGTH(payload) as path_length 
FROM draw_commands 
WHERE cmd_type = 'PEN' LIMIT 5;
```

### Monitor Performance
```sql
-- Check if queries are fast (need index)
SELECT COUNT(*) 
FROM draw_commands 
WHERE session_id = ? AND username = ?;
-- Should be INSTANT with index
```

## 🎓 Learning Resources

### Key Classes to Understand
1. **DrawCommand.java** - Command model with username field
2. **CanvasPanel.java** - Drawing engine with undo/redo logic
3. **CommandProtocol.java** - Serialization/deserialization
4. **WhiteboardFrame.java** - UI with tool buttons
5. **DatabaseManager.java** - Persistence layer

### Important Methods
```java
// In CanvasPanel
canvas.setCurrentTool(Type tool)      // Switch drawing tool
canvas.setCurrentUsername(String user) // Set active user
canvas.undo()                          // Undo user's action
canvas.redo()                          // Redo user's action

// In WhiteboardFrame
bindLocalDraw(callback)    // Connect draw events to network
handleNetworkCommand(cmd)  // Handle remote draw events
```

## ✅ Validation Checklist

- [ ] Database migration completed successfully
- [ ] All Java files recompiled without errors
- [ ] Application starts without exceptions
- [ ] Login works with correct credentials
- [ ] Can create/join rooms
- [ ] Line tool draws straight lines
- [ ] Pen tool draws freehand strokes
- [ ] Erase tool covers with white
- [ ] Delete tool removes clicked strokes
- [ ] Undo button works and reverts last action
- [ ] Redo button restores undone actions
- [ ] Multiple users can draw simultaneously
- [ ] Each user's undo only affects their drawings
- [ ] Drawing commands are stored in database with username
- [ ] Console shows no errors during operation

## 📞 Support

If you encounter issues:
1. Check the console output for error messages
2. Verify database connection is working
3. Review the FEATURES_DRAWING_TOOLS.md documentation
4. Check that all source files were updated
5. Recompile the entire project from scratch

---

**Setup Complete!** 🎉

Your whiteboard application now supports:
- ✅ Per-user undo/redo
- ✅ Multiple drawing tools
- ✅ User tracking and accountability
- ✅ Enhanced drawing capabilities

Happy drawing! 🎨

