# YAPLAB-APP

*Transforming Connections Into Seamless Conversations*

![JavaScript](https://img.shields.io/badge/JavaScript-ES6+-yellow.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring](https://img.shields.io/badge/Spring-6DB33F?style=flat&logo=spring&logoColor=white)
![React](https://img.shields.io/badge/React-20232A?style=flat&logo=react&logoColor=61DAFB)

---

## Table of Contents

- [Overview](#overview)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Usage](#usage)
  - [File Storage](#file-storage)
  - [Configuration](#configuration)
  - [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

Yaplab-app is a real-time messaging platform with a Spring Boot backend and React frontend. It supports chat, media sharing, user/group management, and JWT-secured WebSocket connections. This version is for **local development only**.

---

## Getting Started

### Prerequisites

This project requires the following dependencies:

- **Programming Language:** Java 21
- **Package Manager:** Maven, npm
- **Database:** MySQL (local instance)

### Installation

1. **Install MySQL locally** and create a database named `yaplabdb`.
2. **Clone the repository:**
   ```bash
   git clone https://github.com/AnkitArsh/yaplab-app
   cd yaplab-app
   ```
3. **Install backend dependencies:**
   ```bash
   cd yaplab-app-backend
   mvn install
   ```
4. **Install frontend dependencies:**
   ```bash
   cd ../yaplab-app-frontend
   npm install
   ```

---

## Usage

### Backend

To run the backend server, use the following command:

```bash
cd yaplab-app-backend
mvn spring-boot:run
```

### Frontend

To run the frontend application, use the following command:

```bash
cd yaplab-app-frontend
npm run dev
```

---

## File Storage

- All uploaded files are stored in the local `uploads/` directory (created automatically).
- Subdirectories: `uploads/images/`, `uploads/videos/`, `uploads/audio/`, `uploads/documents/`, `uploads/gifs/`
- No cloud or Azure storage is used in local development.

---

## Configuration

### application.properties

- Only one `application.properties` file is needed for local development.
- Example configuration:

```properties
spring.application.name=yaplab-app
spring.datasource.url=jdbc:mysql://localhost:3306/yaplabdb?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_mysql_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.show-sql=false
spring.jpa.hibernate.ddl-auto=update
file.upload-dir=uploads
jwt.secret=your_jwt_secret_key_here
spring.security.user.name=admin
spring.security.user.password=change_this_password
tenor.api.key=your_tenor_api_key_here
tenor.client.key=yaplab
```

- **No Azure or cloud keys are required.**
- Do **not** ignore `application.properties` in `.gitignore` for local-only development.

### Environment Variables

- You may set secrets (like `jwt.secret` or `tenor.api.key`) as environment variables if you prefer, but it's not required for local use.
- Generate a secure JWT secret: `openssl rand -base64 32`
- Get your own Tenor API key from [Google Tenor API](https://developers.google.com/tenor/guides/quickstart)

---

## Testing

### Backend

To run tests for the backend, use the following command:

```bash
cd yaplab-app-backend
mvn test
```

### Frontend

To run tests for the frontend, use the following command:

```bash
cd yaplab-app-frontend
npm test
```

---

## Troubleshooting

### Common Issues

- **Database connection issues:**
  - Ensure MySQL is running and `yaplabdb` exists.
  - Check your username/password in `application.properties`.
- **File upload issues:**
  - Make sure the `uploads/` directory is writable.
- **Missing API keys:**
  - Set `tenor.api.key` and `jwt.secret` in `application.properties`.
- **Frontend cannot connect to backend:**
  - Check that backend is running on `localhost:8080` and frontend is using the correct API base URL.

---

## Contributing

We welcome contributions! Please feel free to submit issues and pull requests.

## License

This project is open source and available under the [MIT License](LICENSE).

---

**Happy coding! 🚀**
