# H2 vs PostgreSQL 테스트 환경 비교

## 왜 PostgreSQL로 테스트하는가?

### 핵심 목표
**"프로덕션 환경과 동일한 DB로 테스트하여 Production Parity를 확보한다"**

H2 인메모리 DB는 빠르고 편리하지만, 프로덕션에서 사용하는 PostgreSQL과 SQL 방언, 타입 시스템, 제약조건 동작이 다릅니다.
이 차이로 인해 "H2에서는 통과하지만 프로덕션에서 실패하는" 테스트가 발생할 수 있습니다.

---

## 1. 테스트 실행 방식 비교

### H2 (기존 방식)
```bash
./gradlew test
```
- Docker 불필요
- 인메모리 DB → 빠른 실행
- 개발 중 빠른 피드백에 적합

### PostgreSQL (새로운 방식)
```bash
./gradlew cucumberTest
```
- Docker Compose로 PostgreSQL 자동 시작/종료
- 프로덕션과 동일한 DB 엔진
- CI/CD 환경에서의 검증에 적합

---

## 2. SQL 방언 차이

### DatabaseCleaner에서의 차이

| 작업 | H2 | PostgreSQL |
|------|-----|------------|
| FK 비활성화 | `SET REFERENTIAL_INTEGRITY FALSE` | `SET session_replication_role = 'replica'` |
| 테이블 초기화 | `TRUNCATE TABLE x` + `ALTER TABLE x ALTER COLUMN ID RESTART WITH 1` | `TRUNCATE TABLE "x" RESTART IDENTITY CASCADE` |
| FK 활성화 | `SET REFERENTIAL_INTEGRITY TRUE` | `SET session_replication_role = 'DEFAULT'` |

### 예약어 차이

PostgreSQL에서는 `option`이 예약어이므로, 엔티티 이름과 충돌할 수 있습니다.
이를 해결하기 위해 `globally_quoted_identifiers=true` 설정을 사용합니다.

```properties
# application-cucumber.properties
spring.jpa.properties.hibernate.globally_quoted_identifiers=true
```

이 설정은 모든 테이블/컬럼 이름을 큰따옴표로 감싸서 예약어 충돌을 방지합니다.

---

## 3. 아키텍처

```
./gradlew test          → H2 인메모리 (Docker 불필요, 빠른 피드백)
./gradlew cucumberTest  → Docker PostgreSQL (Production Parity)
```

### Profile 기반 분리

| 컴포넌트 | 기본 프로파일 | cucumber 프로파일 |
|----------|-------------|------------------|
| DB | H2 인메모리 | PostgreSQL (Docker) |
| DatabaseCleaner | `DatabaseCleaner` (`@Profile("!cucumber")`) | `PostgresDatabaseCleaner` (`@Profile("cucumber")`) |
| 설정 파일 | `application.properties` | `application-cucumber.properties` |

### DatabaseCleanerStrategy 인터페이스

```java
public interface DatabaseCleanerStrategy {
    void clear();
}
```

- `DatabaseCleaner`: H2용 구현 (`@Profile("!cucumber")`)
- `PostgresDatabaseCleaner`: PostgreSQL용 구현 (`@Profile("cucumber")`)
- `CucumberSpringConfig`에서 `DatabaseCleanerStrategy` 인터페이스로 주입

---

## 4. Docker Compose 설정

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: gift_test
      POSTGRES_USER: gift
      POSTGRES_PASSWORD: gift
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U gift -d gift_test"]
      interval: 3s
      timeout: 3s
      retries: 10
```

### Gradle 태스크 흐름

```
cucumberTest
  ├── dependsOn: dockerComposeUp  (docker compose up -d --wait)
  ├── 실행: Cucumber 테스트 (spring.profiles.active=cucumber)
  └── finalizedBy: dockerComposeDown  (docker compose down)
```

---

## 5. H2 vs PostgreSQL에서 발견할 수 있는 차이점들

| 영역 | H2 | PostgreSQL |
|------|-----|------------|
| **예약어** | 관대함 | `option`, `user`, `order` 등 엄격 |
| **타입 시스템** | 암묵적 변환 많음 | 엄격한 타입 체크 |
| **시퀀스** | `ALTER COLUMN ID RESTART` | `RESTART IDENTITY` |
| **CASCADE** | 개별 TRUNCATE | `CASCADE`로 의존 테이블 함께 처리 |
| **대소문자** | 대소문자 구분 없음 | 따옴표 없으면 소문자로 변환 |
| **문자열 비교** | 관대함 | 타입 일치 필요 |

---

## 6. 언제 어떤 테스트를 실행하나?

| 상황 | 권장 명령어 | 이유 |
|------|-----------|------|
| 로컬 개발 중 빠른 확인 | `./gradlew test` | H2로 빠른 피드백 |
| PR 머지 전 검증 | `./gradlew cucumberTest` | Production Parity 확보 |
| CI/CD 파이프라인 | 둘 다 실행 | 빠른 피드백 + 안정성 보장 |

---

## 결론

**H2와 PostgreSQL 테스트를 함께 유지 = 개발 속도 + 프로덕션 안정성**

- H2: 개발 중 빠른 피드백 루프
- PostgreSQL: 배포 전 프로덕션 환경과의 일치 검증
- 두 환경 모두 동일한 Cucumber 시나리오를 실행하므로, 비즈니스 로직 검증의 일관성이 보장됩니다
