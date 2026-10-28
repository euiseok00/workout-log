## Workout Log 개발 지침

- 프론트엔드(`frontend/`) 검증: `npm run lint`, `npm test`, `npm run build`
- 백엔드(`backend/`) 검증: 환경변수 설정 없이 `./gradlew test` (Windows PowerShell은 `.\gradlew.bat test`)
- 백엔드 PostgreSQL 통합 테스트는 기존 `PostgresIntegrationTest`의 Testcontainers + `@ServiceConnection` 구성을 재사용한다. 환경변수나 별도 `docker run`으로 로컬 테스트 DB를 연결하지 않으며, 개발 DB `workout_log`를 테스트에서 절대 수정하지 않는다.
