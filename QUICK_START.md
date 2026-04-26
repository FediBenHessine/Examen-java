# 🚀 Quick Start Guide - New Features

## ⚡ TL;DR (5 minute setup)

### Step 1: Update Database (2 min)
```sql
-- Copy and paste into MySQL:
ALTER TABLE draw_commands 
ADD COLUMN username VARCHAR(255) DEFAULT 'UNKNOWN' NOT NULL AFTER tool_name;

ALTER TABLE draw_commands 
ADD INDEX idx_session_username (session_id, username);
```

### Step 2: Rebuild & Run (2 min)
```bash
cd C:\Firas\1er Ingenieur\JOO\Examen-java
javac -encoding UTF-8 -d bin src/Model/*.java src/Network/*.java src/IHM/*.java src/auth/*.java src/Database/*.java src/RMI/*.java src/Main.java
java -cp bin Main
```

### Step 3: Test (1 min)
1. Login and create/join room
2. Click tool buttons: 📏 Line | ✏️ Pen | 🧹 Erase | 🗑️ Delete
3. Draw something
4. Click ↶ Undo to see it disappear
5. Click ↷ Redo to see it return

## 🎨 How to Use New Features

### Tool Buttons (Toolbar)
```
📏 Line   - Draw straight lines (click & drag)
✏️ Pen    - Draw freehand (click & drag freely)
🧹 Erase  - Erase with white overlay (click & drag)
🗑️ Delete - Remove strokes by clicking on them
```

### Undo/Redo
```
↶ Undo - Revert your last action
↷ Redo - Restore the undone action
```

### Important Notes ⚠️
- **Undo/Redo only affects YOUR drawings** - Other users' drawings are not affected
- **All drawing tools can be undone** - Including eraser and delete
- **Username is automatically tracked** - Based on who's logged in
- **Database stores everything** - For audit trail and sync

## 🔄 What's Changed

### You Get:
✅ Per-user undo/redo (only affects your drawings)  
✅ Pen tool (freehand drawing)  
✅ Eraser tool (cover with white)  
✅ Delete tool (click to remove strokes)  
✅ User tracking (who did what)  
✅ Database persistence (for auditing)  

### Database Changes:
- New `username` column in `draw_commands` table
- New index for fast queries
- No breaking changes to existing data

### Code Changes:
- `Model/DrawCommand.java` - Added username field
- `Network/CommandProtocol.java` - Serialize username
- `Database/DatabaseManager.java` - Store/retrieve username
- `IHM/CanvasPanel.java` - Complete drawing engine
- `IHM/WhiteboardFrame.java` - Tool buttons
- `DATABASE_MIGRATION.sql` - Migration script

## 📊 Multi-User Example

```
Timeline:
T1: Alice draws line
    → Alice sees: line
    → Bob sees: line
    → ↶ Undo enabled for Alice

T2: Bob draws circle
    → Alice sees: line + circle
    → Bob sees: line + circle
    → ↶ Undo enabled for Bob (independently)

T3: Alice clicks Undo
    → Alice sees: circle only
    → Bob still sees: line + circle
    → Bob's undo/redo unchanged

T4: Bob clicks Undo
    → Bob sees: line only
    → Alice still sees: circle only
    → Both users see different states!
```

## 🎯 Key Points

### Undo/Redo Behavior
- Each user has their own undo/redo stack
- When you undo, your drawing disappears but others' remains
- This allows independent editing of shared canvas
- Everyone sees the sum of all final drawing states

### Tool Behavior
| Tool | Action | Result |
|------|--------|--------|
| Line | Click & drag | Straight line |
| Pen | Click & drag | Smooth freehand curve |
| Erase | Click & drag | White cover (looks like erasing) |
| Delete | Click once | Remove entire stroke |

### Network Sync
- All drawing commands sent to other users
- Each user's undo/redo stays local
- When you undo, only you see the change
- When you draw again, everyone sees it

## 🔍 Troubleshooting

| Problem | Solution |
|---------|----------|
| Undo button disabled | Draw something first |
| Can't find tools | Check toolbar for new buttons |
| Pen not drawing | Use click & drag motion (not just click) |
| Delete removes wrong stroke | Click more precisely on stroke |
| Username not showing in DB | Run migration script first |
| "UNDO incompatible" error | Recompile all Java files |

## 📁 Documentation Files

- **SETUP_INSTRUCTIONS.md** - Complete setup guide
- **FEATURES_DRAWING_TOOLS.md** - Detailed feature documentation
- **IMPLEMENTATION_SUMMARY.md** - Technical implementation details
- **DATABASE_MIGRATION.sql** - SQL migration script

## ✅ Verification Checklist

After setup, verify:
- [ ] Database migration completed (no errors)
- [ ] Code recompiled (no compilation errors)
- [ ] Application starts (can login)
- [ ] Tool buttons visible in toolbar
- [ ] Can draw with each tool
- [ ] Undo button enables after drawing
- [ ] Undo works (drawing disappears)
- [ ] Redo button enables after undo
- [ ] Redo works (drawing returns)
- [ ] Multi-user drawing works
- [ ] Each user's undo independent

## 🎓 Code Examples

### Switch to Pen Tool
```java
canvas.setCurrentTool(DrawCommand.Type.PEN);
```

### Set Username
```java
canvas.setCurrentUsername("john_doe");
```

### Perform Undo
```java
canvas.undo();
updateUndoRedoButtons();
```

### Check if Can Undo
```java
if (canvas.canUndo()) {
    btnUndo.setEnabled(true);
}
```

## 🆘 Getting Help

If something doesn't work:

1. **Check Console** - Look for error messages
2. **Check Database** - Run: `DESCRIBE draw_commands; SHOW INDEXES FROM draw_commands;`
3. **Recompile** - Sometimes caching causes issues
4. **Restart** - Try closing and reopening the app
5. **Reset** - Use fresh database if needed
6. **Read Docs** - Check SETUP_INSTRUCTIONS.md for details

## 📞 Common Questions

**Q: Will undo/redo break existing rooms?**  
A: No! Existing data is preserved. Migration is backward compatible.

**Q: Can I undo other users' drawings?**  
A: No. Each user can only undo their own drawings.

**Q: Does pen tool work over other drawings?**  
A: Yes! Pen draws on top of existing drawings just like other tools.

**Q: Can deleted strokes be restored?**  
A: Yes! Use undo to restore any deleted stroke.

**Q: Is username really tracked in database?**  
A: Yes! Check with: `SELECT DISTINCT username FROM draw_commands;`

**Q: What if I want to erase vs delete?**  
A: Erase covers with white (softer). Delete removes completely (more precise).

---

## 🎉 You're Ready!

Your whiteboard now has:
- Professional undo/redo
- Multiple drawing tools
- User accountability
- Full audit trail

Happy drawing! 🎨

