<!-- codex-harness:start -->
## Codex Harness

하네스 자체의 구축, 점검, 동기화에는 `.agents/skills/harness/SKILL.md`를 읽는다.
메인 세션이 작업 분해, 공용 계약, 통합과 완료 판정을 맡는다. 프로젝트 역할은 필요한 경우에만 하나씩 호출하며, 서로 독립적인 작업이 분명할 때만 최대 2개 스레드를 사용한다.
<!-- codex-harness:end -->

<!-- codex-harness:domain:start -->
## Workout Log 개발 하네스

기능 구현, 프론트엔드-백엔드 인터페이스, 데이터베이스 변경, 코드 리뷰, 테스트 및 검증 작업에는 `.agents/skills/workout-development/SKILL.md`를 읽는다.
백엔드 PostgreSQL 통합 테스트는 기존 `PostgresIntegrationTest`의 Testcontainers + `@ServiceConnection` 구성을 재사용한다. 환경변수나 별도 `docker run`으로 로컬 테스트 DB를 연결하지 않으며, 개발 DB `workout_log`를 테스트에서 절대 수정하지 않는다. 전체 테스트는 환경변수 설정 없이 `backend/`에서 `./gradlew test`로 실행한다.
<!-- codex-harness:domain:end -->
