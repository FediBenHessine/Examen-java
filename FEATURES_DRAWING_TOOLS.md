# 🎨 Whiteboard Application - Drawing Tools & Undo/Redo Features

## 📋 New Features Overview

### 1. **Per-User Undo/Redo** ✅
- Each user can only undo/redo their own commands within a session
- Commands are tracked with username for accountability
- Undo/Redo stacks are maintained separately from network broadcasts
- When you undo, only your drawing history is affected

**How it works:**
- Click **↶ Undo** button to undo your last action
- Click **↷ Redo** button to redo the undone action
- Other users' drawings remain unaffected
- All operations are synchronized across clients

### 2. **Drawing Tools** 🎯

#### 📏 **Line Tool** (Default)
- Classic straight-line drawing
- Click and drag to create lines
- Adjustable stroke width and color
- Perfect for precise geometric shapes

#### ✏️ **Pen Tool** (NEW)
- Freehand drawing for natural handwriting/sketching
- Draws smooth curves as you move the mouse
- Captures a series of points to create detailed paths
- Great for annotations and artistic drawings
- Usage:
  1. Click the "✏️ Pen" button to activate
  2. Click and drag to draw freehand
  3. Each pen stroke is saved as a single command

#### 🧹 **Erase Tool** (NEW)
- Removes drawn content by painting white over it
- Acts like a real eraser - removes whatever is beneath
- Automatically uses 2x the stroke width for effective erasing
- Usage:
  1. Click the "🧹 Erase" button to activate
  2. Click and drag over the area you want to erase
  3. Erased content can be undone with Undo button

#### 🗑️ **Delete Tool** (NEW)
- Intelligently removes entire strokes at the click position
- Click once to remove the topmost stroke where your cursor is
- More precise than eraser - removes only complete strokes
- Usage:
  1. Click the "🗑️ Delete" button to activate
  2. Click on a stroke to remove it completely
  3. Deleted strokes can be undone with Undo button

### 3. **User Tracking** 👤
- Every drawing command records who created it
- Database tracks username for each action
- Enables audit trail and future features:
  - See who drew what
  - Per-user statistics
  - User-specific undo/redo

### 4. **Database Schema Updates** 💾

#### Modified Table: `draw_commands`
New column added:
```sql
-- Track who created each command
username VARCHAR(255) DEFAULT 'UNKNOWN' NOT NULL

-- New index for faster queries
INDEX idx_session_username (session_id, username)
```

#### New Command Types
```java
enum Type { 
    LINE,        // Original - straight lines
    PEN,         // NEW - freehand drawing
    ERASER,      // NEW - eraser tool
    DELETE,      // NEW - stroke deletion
    RECT,        // Original - rectangles
    CIRCLE,      // Original - circles
    TOOL,        // Original - generic tool
    CLEAR,       // Original - clear canvas
    PING,        // Original - heartbeat
    PONG,        // Original - heartbeat response
    SYNC,        // Original - synchronization
    UNDO,        // Original - undo command
    REDO         // Original - redo command
}
```

## 🔄 How Undo/Redo Works

### Local Undo/Redo (Per-User)
1. **Undo Stack**: Stores complete canvas states before each action
2. **Redo Stack**: Stores states after undo
3. **User Isolation**: Each user maintains separate stacks
4. **Network Sync**: Undo/Redo commands are sent to server with username

### Remote Undo/Redo Handling
- When you receive an UNDO/REDO command from another user:
  - The command is logged but **not applied** to your canvas
  - Their undo/redo happens on their canvas locally
  - Their final result (after undo/redo) syncs back to you
  - Your undo/redo stacks remain unaffected

### Example Scenario
```
Timeline:
T1: User A draws line → Canvas shows A's line
T2: User B draws circle → Canvas shows A's line + B's circle
T3: User A clicks UNDO → Canvas shows B's circle only (A's line gone)
    - A's undo command sent to server
    - B still sees both drawings
    - A can REDO to get line back
T4: User B clicks UNDO → Canvas shows A's line only (B's circle gone)
    - B's undo command sent to server
    - A still has option to undo their undo
```

## 🛠️ Implementation Details

### CanvasPanel (Drawing & Rendering)
```java
// Tool management
public void setCurrentTool(DrawCommand.Type tool)
public DrawCommand.Type getCurrentTool()

// User tracking
public void setCurrentUsername(String username)

// Drawing state management
private List<DrawCommand> history          // All drawn commands
private Stack<> undoStack, redoStack       // User's undo/redo stacks
private DrawCommand.Type currentTool       // Active drawing tool
private List<Point> penPath                // Current pen stroke points

// Undo/Redo operations
public void undo()      // Only affects current user's commands
public void redo()      // Only affects current user's commands
public boolean canUndo()
public boolean canRedo()
```

### WhiteboardFrame (UI Controls)
```java
// Tool buttons in toolbar
📏 Line    - Switch to line drawing
✏️ Pen     - Switch to freehand drawing
🧹 Erase   - Switch to eraser
🗑️ Delete  - Switch to delete tool

// Button states
Active tool is highlighted in blue
Tool status shown in status bar
```

### DrawCommand Model (Enhanced)
```java
public class DrawCommand {
    public enum Type { LINE, PEN, ERASER, DELETE, ... }
    
    // Original fields
    public Type type
    public double x1, y1, x2, y2
    public String colorHex
    public float strokeWidth
    
    // NEW fields
    public String username           // Who created this command
    public String payload            // For PEN tool: serialized point path
}
```

## 🔐 Security & Data Integrity

### Username Verification
- Username is set by the application (not sent by client)
- Set from authenticated login
- Can't be spoofed in network messages
- Stored in database for audit trail

### Command Validation
- Draw commands include stroke data for verification
- Erase/Delete commands include click coordinates
- Undo/Redo commands tracked with timestamps
- All operations logged to database

## 📊 Serialization Format

### Command Protocol (Network Format)
```
LINE Command:
"LINE|x1|y1|x2|y2|#FF0000|2.0|username"

PEN Command:
"PEN|x1|y1|x2|y2|#FF0000|2.0|username"
(payload contains point path serialized as "x,y;x,y;...")

ERASER Command:
"ERASER|x1|y1|x2|y2|#FFFFFF|4.0|username"

DELETE Command:
"DELETE|x1|y1|0|0|#000000|0|username"
(only x1,y1 used - click position)

UNDO Command:
"UNDO|username"

REDO Command:
"REDO|username"
```

## 💾 Database Schema

### draw_commands Table
```sql
Field              | Type           | Purpose
---|---|---
id                 | INT PRIMARY KEY | Unique command ID
session_id         | INT            | Session this command belongs to
cmd_type           | VARCHAR(50)    | Command type (LINE, PEN, ERASER, etc.)
x1, y1, x2, y2     | DOUBLE         | Coordinates
color_hex          | VARCHAR(7)     | Color in hex format (#RRGGBB)
stroke_width       | FLOAT          | Pen width in pixels
tool_name          | VARCHAR(50)    | Tool identifier
username           | VARCHAR(255)   | ✅ NEW: Who created this command
executed_at        | TIMESTAMP      | When the command was executed
```

### Example Queries
```sql
-- Get all commands by user in a session
SELECT * FROM draw_commands 
WHERE session_id = 1 AND username = 'player1'
ORDER BY executed_at ASC;

-- Count strokes per user
SELECT username, COUNT(*) as stroke_count 
FROM draw_commands 
WHERE session_id = 1 AND cmd_type IN ('LINE', 'PEN', 'ERASER')
GROUP BY username;

-- Get undo/redo activity
SELECT username, cmd_type, COUNT(*) as count 
FROM draw_commands 
WHERE cmd_type IN ('UNDO', 'REDO') AND session_id = 1
GROUP BY username, cmd_type;
```

## 🚀 Usage Guide

### For Drawing
1. **Select Tool**: Click one of the tool buttons (📏 Line, ✏️ Pen, 🧹 Erase, 🗑️ Delete)
2. **Choose Color & Size**: Use color picker and size selector
3. **Draw**: Click and drag on canvas (or click to delete)
4. **Undo/Redo**: Click ↶ Undo or ↷ Redo as needed

### For Multi-User Sessions
1. Each user's drawing tool is only affected by their own undo/redo
2. Drawings from other users remain visible and synchronized
3. Each user sees the result of everyone's final drawing state
4. Network sync ensures all clients stay in sync

### Configuration
```java
// In WhiteboardFrame constructor
canvas.setCurrentUsername(username);  // Set the user

// In CanvasPanel
canvas.setCurrentTool(DrawCommand.Type.PEN);     // Switch tools
canvas.setToolSettings("#FF0000", 5.0f);         // Color & size
```

## 🐛 Troubleshooting

### Undo/Redo not working
- Ensure you've drawn at least one stroke (undo button should be enabled)
- Check that username is set in canvas
- Look for errors in console logs

### Pen tool not drawing smoothly
- Increase stroke width for better visibility
- Check that you're dragging (not just clicking)
- Verify point buffer is being populated

### Delete tool not removing strokes
- Ensure you're clicking directly on a stroke
- Try clicking closer to the stroke center
- Strokes with large width are easier to click

### Username not tracking correctly
- Check that `bindLocalDraw` is called before drawing
- Verify database has username column (run migration)
- Check database logs for INSERT errors

## 📌 Future Enhancements

- [ ] Selective undo (undo only specific user's strokes)
- [ ] Shape tools (rectangle, circle, polygon)
- [ ] Layer support (separate layers per user)
- [ ] Stroke thickness preview
- [ ] Drawing animation replay
- [ ] Export per-user drawing history
- [ ] Collaborative annotations

## 📝 Code Examples

### Switch to Pen Tool
```java
canvas.setCurrentTool(DrawCommand.Type.PEN);
```

### Set User Context
```java
canvas.setCurrentUsername("player1");
```

### Handle Undo
```java
canvas.undo();  // Undoes current user's last action
updateUndoRedoButtons();  // Update button states
```

### Custom Drawing with Undo Support
```java
// Save state before major operation
canvas.saveUndoState();  // Internal method

// Draw something
canvas.addRemoteCommand(serializedCommand);

// Later: undo it
canvas.undo();
```

---

**Last Updated**: April 26, 2026  
**Version**: 2.0 (Undo/Redo with User Tracking)

