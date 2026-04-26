# Whiteboard Collaboration Application

A real-time collaborative whiteboard application built with Java, featuring user authentication, room discovery, and live drawing synchronization.

## Features

- **User Authentication**: Login and sign up functionality
- **Real-time Collaboration**: Live drawing synchronization between multiple users
- **Room Discovery**: UDP-based room discovery for easy joining
- **Multiple Room Types**: Public, password-protected, and private rooms
- **Session Management**: RMI-based session handling
- **Database Persistence**: MySQL database for user accounts, rooms, and drawing history

## Getting Started

### Prerequisites

- Java 17 or higher
- MySQL database
- Required dependencies (see below)

### Database Setup

1. Create a MySQL database named `whiteboard`
2. Run the SQL scripts in `database/setup.sql` to create tables
3. Update database connection settings in `Database/Singleton.java`

### Running the Application

1. Compile the project:
   ```bash
   javac -encoding UTF-8 -cp "src" -d "out" src/Main.java src/IHM/*.java src/Database/*.java src/Model/*.java src/Network/*.java src/RMI/*.java src/auth/*.java
   ```

2. Run the application:
   ```bash
   java -cp "out" Main
   ```

## User Guide

### Signing Up

1. Launch the application
2. Click "Sign Up" on the login dialog
3. Enter a username (minimum 3 characters)
4. Enter a password (minimum 6 characters)
5. Confirm your password
6. Click "Register"

### Logging In

1. Enter your username and password
2. Click "Login"
3. You'll be taken to the dashboard

### Hosting a Room

1. From the dashboard, click "Host a Room"
2. Enter room details:
   - Room name
   - Visibility (Public/Private/Password-protected)
   - Password (if required)
   - Port number
3. Click "Start Hosting"
4. Share your IP address and port with others

### Joining a Room

1. From the dashboard, click "Join a Room"
2. The app will automatically discover available rooms
3. Select a room from the list
4. Enter password if required
5. Click "Join Selected"

### Drawing

- Use the color buttons to change drawing color
- Use the clear button to clear the canvas
- All drawings are synchronized in real-time

## Architecture

### Packages

- `IHM/`: User interface components (Login, Dashboard, Whiteboard)
- `Database/`: Database operations and connection management
- `Model/`: Data models (User, Room, DrawCommand)
- `Network/`: Socket communication and UDP discovery
- `RMI/`: Remote session management
- `auth/`: Authentication services

### Key Components

- **LoginDialog**: Handles user authentication and registration
- **DashboardFrame**: Main application dashboard
- **WhiteboardFrame**: Drawing interface with real-time sync
- **UDPRoomDiscovery**: Network discovery service
- **SocketServer/Client**: Real-time communication
- **SessionImpl**: RMI session management

## Security Notes

⚠️ **This is a demo application with the following security limitations:**

- Passwords are stored in plaintext (use BCrypt in production)
- No HTTPS/TLS encryption
- Basic input validation only
- No rate limiting or account lockout

For production use, implement proper security measures.

## Troubleshooting

### Common Issues

1. **"Session already active" error**: Close all application instances and try again
2. **Rooms not appearing in discovery**: Check firewall settings for UDP port 8888
3. **Connection lost**: Check network connectivity and port availability
4. **Database connection errors**: Verify MySQL is running and credentials are correct

### Debug Mode

Enable debug logging by checking console output for messages prefixed with:
- 🔧 Setup information
- ✅ Success messages
- ❌ Error messages
- 📡 Network discovery
- 🟢 Service status

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is for educational purposes.
