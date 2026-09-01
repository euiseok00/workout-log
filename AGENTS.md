# Project Guidelines

## Core

- Make only changes required for the requested task.
- Read relevant existing code before modifying it.
- Prefer the smallest working solution.
- Reuse existing code and framework features before adding abstractions.
- Do not modify unrelated files or perform broad refactoring.
- Do not add dependencies unless clearly necessary.
- Preserve the existing project structure and coding style.

## Backend

- Java 21, Spring Boot, Gradle.
- Backend code is under `backend/`.
- Use Spring JDBC and explicit SQL.
- Do not introduce JPA/Hibernate or Lombok.
- PostgreSQL is the database.
- Follow the existing Controller / Service / Repository pattern.
- Do not change the database schema unless explicitly requested.
- Use transactions for related multi-table writes.

## Frontend

- React, JavaScript, Vite.
- Frontend code is under `frontend/`.
- Mobile-first; use approximately 390px as the main reference width.
- Reuse existing components and styles.
- Do not introduce TypeScript, UI libraries, or state-management libraries unless explicitly requested.
- Do not split components or create abstractions unless they provide a clear benefit.
- Preserve the existing Black / White / Gray visual direction.

## UI

- Keep layouts functional and information-dense.
- Avoid gradients, glassmorphism, neon effects, excessive shadows, large border-radius, and excessive pill UI.
- Use accent colors only when they communicate meaning or state.
- Keep borders and dividers visually clear.

## Validation

- After changes, run the relevant build/test command.
- Do not leave debug code, unused imports, or commented-out code.
- Report changed files, main changes, and verification performed.