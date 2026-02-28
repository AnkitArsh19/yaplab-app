# YAPLAB-APP

*Transforming Connections Into Seamless Conversations*

![JavaScript](https://img.shields.io/badge/JavaScript-ES6+-yellow.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring](https://img.shields.io/badge/Spring-6DB33F?style=flat&logo=spring&logoColor=white)
![React](https://img.shields.io/badge/React-20232A?style=flat&logo=react&logoColor=61DAFB)

---

## What Is This?

Yaplab is a full-stack real-time messaging application — built from scratch, not a tutorial clone. It handles personal and group chats, file sharing, message encryption, user presence, and WebSocket-driven live updates, all wired together with a Spring Boot backend and a React frontend.

---

## Why It Was Built

This project exists to demonstrate end-to-end ownership of a non-trivial application: authentication flows, real-time bidirectional communication, file handling, database design, and a responsive UI — the kind of work that shows up in production systems, not just code samples.

---

## Features

### Messaging
- Personal (1-on-1) and group chat
- Text, image, video, audio, GIF, and document messages
- Message editing, deletion (soft delete), and forwarding
- Reply-to-message threading
- Multi-select operations (bulk delete, forward, mark as read)
- Emoji picker and GIF search (Tenor API)

### Real-Time UX
- WebSocket (STOMP over SockJS) for instant message delivery
- Typing indicators with auto-stop
- Message status tracking: SENT → DELIVERED → READ with live tick updates
- User presence: online/offline status with "last seen" timestamps
- Connection health monitoring, auto-reconnect with exponential backoff

### Groups
- Create, rename, and manage groups
- Add/remove members, admin controls
- Group avatars and settings
- Real-time group event broadcasting (member joins/leaves, settings updates)

### Auth & Security
- JWT access + refresh token authentication
- BCrypt password hashing (strength 12)
- Stateless sessions — no server-side session storage
- WebSocket authentication via STOMP CONNECT headers
- AES-256 encryption at rest for message content in the database

### Profile & Settings
- Profile picture upload and management
- User settings and about section
- Email change with confirmation flow

### File Handling
- Upload and preview: images, videos, audio, documents, GIFs
- Audio recording with waveform visualization (WaveSurfer.js)
- Video playback in modal
- Contact card sharing
- All files stored locally in `uploads/` with organized subdirectories

### Connection Resilience
- Automatic reconnection on network loss or tab switch
- Message queuing while disconnected (sent automatically on reconnect)
- Token auto-refresh before WebSocket reconnection
- Page visibility and focus-aware reconnection

---

## Tech Stack

| Layer     | Technology                                                               |
| --------- | ------------------------------------------------------------------------ |
| Frontend  | React 19, Vite 7, React Router 7, STOMP.js, SockJS, Framer Motion       |
| Backend   | Java 21, Spring Boot 3.4, Spring Security, Spring WebSocket, JPA/Hibernate |
| Database  | MySQL                                                                    |
| Auth      | JWT (JJWT 0.12), BCrypt, refresh tokens                                 |
| Encryption| AES-256/CBC/PKCS5Padding (encryption at rest)                            |
| Media     | WaveSurfer.js, emoji-mart, date-fns                                      |

---

## Project Structure

```
yaplab-app/
├── yaplab-app-backend/
│   └── src/main/java/com/yaplab/
│       ├── config/           # WebSocket, Security, AOP, Encryption config
│       ├── user/             # User entity, service, controller
│       ├── message/          # Message entity, service, controller, encryption converter
│       ├── chatroom/         # ChatRoom entity, service, controller
│       ├── group/            # Group entity, service, controller
│       ├── files/            # File upload entity, service, controller
│       ├── security/         # JWT, auth, token management
│       └── enums/            # MessageType, MessageStatus, UserStatus
├── yaplab-app-frontend/
│   └── src/
│       ├── components/
│       │   ├── auth/         # Login / Register
│       │   ├── chat/         # Sidebar, ChatArea, MessageInput, MessageArea
│       │   ├── groups/       # Group modals and settings
│       │   ├── media/        # Attachments, audio recorder/player, video, GIFs
│       │   ├── modals/       # Contact selection, settings, user info
│       │   └── ui/           # Reusable UI components
│       ├── styles/           # Component-level CSS
│       └── utils/            # API client, WebSocket service, helpers
└── uploads/                  # Local file storage (auto-created)
```

---

## Getting Started

### Prerequisites

- **Java 21**
- **Maven**
- **Node.js** (v18+) and **npm**
- **MySQL** (local instance)

### Installation

1. **Create the database:**
   ```sql
   CREATE DATABASE yaplabdb;
   ```

2. **Clone the repository:**
   ```bash
   git clone https://github.com/AnkitArsh/yaplab-app
   cd yaplab-app
   ```

3. **Configure the backend** — edit `yaplab-app-backend/src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/yaplabdb?useSSL=false&serverTimezone=UTC
   spring.datasource.username=root
   spring.datasource.password=your_mysql_password
   jwt.secret=your_jwt_secret_key_here
   encryption.message.secret=your_encryption_secret_key_here
   tenor.api.key=your_tenor_api_key_here
   ```
   Generate secure keys:
   ```bash
   openssl rand -base64 32   # for jwt.secret
   openssl rand -base64 32   # for encryption.message.secret
   ```
   Get a Tenor API key from [Google Tenor API](https://developers.google.com/tenor/guides/quickstart).

4. **Install and run the backend:**
   ```bash
   cd yaplab-app-backend
   mvn install
   mvn spring-boot:run
   ```

5. **Install and run the frontend:**
   ```bash
   cd ../yaplab-app-frontend
   npm install
   npm run dev
   ```

The backend runs on `http://localhost:8080`, the frontend on `http://localhost:5173`.

---

<details>
<summary><strong>API Overview</strong> (click to expand)</summary>

### Authentication
| Method | Endpoint               | Description             |
| ------ | ---------------------- | ----------------------- |
| POST   | `/auth/register`       | Register a new user     |
| POST   | `/auth/login`          | Login, returns JWT pair |
| POST   | `/auth/refresh`        | Refresh access token    |

### Users
| Method | Endpoint                        | Description                    |
| ------ | ------------------------------- | ------------------------------ |
| GET    | `/users/{id}`                   | Get user by ID                 |
| GET    | `/users/list/ONLINE`            | List online users              |
| GET    | `/users/status/comprehensive`   | Get all user statuses          |
| PUT    | `/users/{id}`                   | Update user profile            |
| POST   | `/users/{id}/profile-picture`   | Upload profile picture         |

### Messages
| Method | Endpoint                          | Description               |
| ------ | --------------------------------- | ------------------------- |
| GET    | `/messages/chatroom/{chatroomId}` | Get messages for chatroom |
| DELETE | `/messages/{id}`                  | Soft-delete a message     |
| PUT    | `/messages/{id}`                  | Edit a message            |

### Chat Rooms
| Method | Endpoint                       | Description                |
| ------ | ------------------------------ | -------------------------- |
| GET    | `/chatrooms/user/{userId}`     | Get user's chat rooms      |
| POST   | `/chatrooms`                   | Create a chat room         |

### Groups
| Method | Endpoint                          | Description             |
| ------ | --------------------------------- | ----------------------- |
| POST   | `/groups`                         | Create a group          |
| PUT    | `/groups/{id}`                    | Update group settings   |
| POST   | `/groups/{id}/members`            | Add members             |
| DELETE | `/groups/{id}/members/{userId}`   | Remove a member         |

### Files
| Method | Endpoint             | Description        |
| ------ | -------------------- | ------------------ |
| POST   | `/files/upload`      | Upload a file      |
| GET    | `/files/{id}`        | Download a file    |

### WebSocket Destinations
| Destination                          | Direction | Description                |
| ------------------------------------ | --------- | -------------------------- |
| `/app/messages/personal`             | Send      | Send personal message      |
| `/app/messages/group`                | Send      | Send group message         |
| `/app/messages/typing/{roomId}`      | Send      | Start typing indicator     |
| `/app/messages/stop-typing/{roomId}` | Send      | Stop typing indicator      |
| `/app/messages.read`                 | Send      | Mark messages as read      |
| `/app/messages.delivered`            | Send      | Mark messages as delivered |
| `/user/{userId}/messages`            | Receive   | Personal messages          |
| `/user/{userId}/status`              | Receive   | Message status updates     |
| `/room/{roomId}/messages`            | Receive   | Room messages              |
| `/room/{roomId}/events`              | Receive   | Room events (typing, etc.) |
| `/topic/user-status`                 | Receive   | User online/offline status |
| `/topic/group/{groupId}`             | Receive   | Group events               |

</details>

---

## Known Limitations

- **Local development only** — no cloud deployment configuration included.
- **No end-to-end encryption** — messages are encrypted at rest (AES-256 in the database) but not end-to-end between clients.
- **Single-server architecture** — WebSocket connections are not distributed across multiple instances.
- **No push notifications** — real-time updates require the app to be open.
- **No message pagination** — all messages for a chat are loaded at once.

---

## Troubleshooting

| Problem                              | Fix                                                                                          |
| ------------------------------------ | -------------------------------------------------------------------------------------------- |
| Database connection fails            | Ensure MySQL is running and `yaplabdb` exists. Check credentials in `application.properties`. |
| `messageEncryptionUtil` bean error   | Set `encryption.message.secret` in `application.properties` to any non-empty string.          |
| File uploads fail                    | Make sure the `uploads/` directory is writable.                                               |
| Frontend can't reach backend         | Confirm backend is running on `localhost:8080`. Check CORS and proxy settings.                |
| WebSocket won't connect              | Verify auth token is present in localStorage. Check browser console for STOMP errors.         |
| Missing GIFs                         | Set a valid `tenor.api.key` in `application.properties`.                                      |

---

## License

This project is open source and available under the [MIT License](LICENSE).
