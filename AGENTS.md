# Project Guidelines

- Make only changes required for the requested task.
- Read relevant existing code before modifying it.
- Reuse existing code and framework features before adding abstractions or dependencies.
- Keep implementations simple and scoped. Do not over-engineer.
- Do not modify unrelated files.

## Backend
- Java 21, Spring Boot, Gradle.
- Backend code is under `backend/`.
- Use Spring JDBC and explicit SQL. Do not introduce JPA/Hibernate.
- PostgreSQL is the database.
- Follow the existing database schema; do not change it unless explicitly requested.

## Frontend
- React, JavaScript, Vite.
- Frontend code is under `frontend/`.