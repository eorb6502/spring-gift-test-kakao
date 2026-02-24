# PostgreSQL 테스트 환경 통일

## 왜 H2를 제거하고 PostgreSQL로 통일했는가?

### 핵심 원칙
**"테스트 환경과 운영 환경의 DB를 일치시켜 Production Parity를 확보한다"**

H2 인메모리 DB는 빠르고 편리하지만, 운영 환경과 SQL 방언·타입 시스템·제약조건 동작이 달라 **"H2에서는 통과하지만 프로덕션에서 실패하는"** 테스트가 발생할 수 있다.
Docker로 PostgreSQL을 띄우면 로컬에서도 충분히 빠르기 때문에, H2의 속도 이점이 DB 불일치 리스크를 상쇄할 만큼 크지 않다고 판단했다.

---

## 1. 테스트 실행 방식

```bash
./gradlew test
```

- Docker Compose로 PostgreSQL 자동 시작 → 테스트 실행 → 자동 종료
- 별도 프로파일 분기 없이 단일 경로로 실행

---

## 2. H2를 사용하지 않는 이유

| 영역 | H2 동작 | PostgreSQL 동작 |
|------|---------|----------------|
| **예약어** | 관대함 | `option`, `user`, `order` 등 엄격 |
| **타입 시스템** | 암묵적 변환 많음 | 엄격한 타입 체크 |
| **시퀀스 초기화** | `ALTER COLUMN ID RESTART` | `RESTART IDENTITY` |
| **CASCADE** | 개별 TRUNCATE | `CASCADE`로 의존 테이블 함께 처리 |
| **대소문자** | 구분 없음 | 따옴표 없으면 소문자로 변환 |

이러한 차이 때문에 H2에서 녹색이던 테스트가 PostgreSQL에서 실패하는 상황이 발생할 수 있다.
DB를 통일하면 이런 불일치를 원천적으로 제거할 수 있다.

---

## 3. 아키텍처

```
./gradlew test
  ├── dockerComposeUp   (docker compose up -d --wait)
  ├── 실행: Cucumber 테스트 (PostgreSQL)
  └── dockerComposeDown (docker compose down)
```

### 설정 파일

| 파일 | 역할 |
|------|------|
| `docker-compose.yml` | PostgreSQL 컨테이너 정의 |
| `src/test/resources/application.properties` | 테스트 DB 접속 정보 |
| `DatabaseCleaner` | PostgreSQL 기반 테이블 초기화 |

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

---

## 5. DatabaseCleaner

H2/PostgreSQL 분기를 위한 인터페이스(`DatabaseCleanerStrategy`)와 Profile 분기를 제거하고, PostgreSQL 전용 `DatabaseCleaner` 하나로 통일했다.

```java
@Component
public class DatabaseCleaner {
    // SET session_replication_role = 'replica' → FK 비활성화
    // TRUNCATE TABLE "x" RESTART IDENTITY CASCADE → 테이블 초기화
    // SET session_replication_role = 'origin' → FK 활성화
}
```

---

## 6. 이전 구조와의 비교

| 항목 | 이전 (H2 + PostgreSQL 병행) | 현재 (PostgreSQL 통일) |
|------|---------------------------|----------------------|
| 실행 명령 | `./gradlew test` (H2), `./gradlew cucumberTest` (PG) | `./gradlew test` |
| DatabaseCleaner | 2개 (Profile 분기) | 1개 |
| 설정 파일 | `application.properties` + `application-cucumber.properties` | `application.properties` (테스트용) |
| Docker 의존 | cucumberTest만 | 항상 |
| Production Parity | cucumberTest만 보장 | 항상 보장 |
