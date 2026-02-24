# Application 컨테이너화 구현 문서

## 개요

기존에는 PostgreSQL만 Docker로 실행하고 Spring Boot 애플리케이션은 테스트 프로세스 내에서 embedded로 실행했습니다.
이번 변경으로 애플리케이션까지 Docker 컨테이너로 실행하여 **프로덕션과 동일한 환경**에서 E2E 테스트를 수행할 수 있습니다.

## 아키텍처 비교

### Before (요구사항 2)
```
[Test JVM]                    [Docker]
├── Spring Boot (embedded)    └── PostgreSQL
├── Cucumber Steps
└── REST Assured → localhost:randomPort
```

### After (요구사항 3)
```
[Test JVM]                    [Docker]
├── Cucumber Steps            ├── Spring Boot App (:28080)
└── REST Assured              └── PostgreSQL (:5432)
    → localhost:28080             └── app → postgres:5432
```

## 핵심 설계 결정

### 1. Docker Compose Profiles로 기존 테스트 보호

`app` 서비스에 `profiles: ["app"]`을 설정하여 기존 `test` 태스크(PostgreSQL만 사용)에 영향을 주지 않습니다.

- `docker compose up` → PostgreSQL만 시작 (기존 동작)
- `docker compose --profile app up` → PostgreSQL + App 시작 (Docker E2E)

### 2. CucumberSpringConfig 패키지 분리

`@CucumberContextConfiguration`은 glue 경로당 하나만 존재해야 합니다.

- `gift.config.CucumberSpringConfig` - Embedded 모드 (기존 `test` 태스크)
- `gift.docker.DockerCucumberSpringConfig` - Docker 모드 (`cucumberTest` 태스크)

각 테스트 러너가 서로 다른 glue 경로를 사용하므로 충돌 없이 공존합니다.

### 3. Step Definitions 재사용

기존 `gift.steps` 패키지의 Step Definitions는 두 모드에서 모두 재사용됩니다.
REST Assured의 baseURI/port 설정만 다르고, 테스트 로직은 동일합니다.

### 4. DB 클린업 전략

- **Embedded 모드**: `EntityManager` 기반 `DatabaseCleaner` (앱 컨텍스트 내에서 동작)
- **Docker 모드**: `JdbcTemplate` 기반 직접 SQL (앱은 별도 컨테이너이므로 테스트 JVM에서 DB 직접 접근)

### 5. Multi-stage Dockerfile

- **Builder stage**: `eclipse-temurin:21-jdk` + Gradle 빌드
- **Runtime stage**: `eclipse-temurin:21-jre-alpine` (경량 이미지, curl 포함 for healthcheck)

## Gradle 태스크 정리

| 태스크 | 설명 |
| :--- | :--- |
| `test` | Embedded 모드 Cucumber 테스트 (PostgreSQL Docker + 내장 서버) |
| `dockerBuild` | 애플리케이션 Docker 이미지 빌드 |
| `dockerUp` | 전체 스택 시작 (app + postgres) |
| `dockerDown` | 전체 스택 종료 |
| `cucumberTest` | Docker 환경 E2E Cucumber 테스트 |

## 파일 변경 목록

### 신규 파일
- `Dockerfile` - Multi-stage 빌드 설정
- `.dockerignore` - Docker 빌드 컨텍스트 최적화
- `src/test/java/gift/config/CucumberSpringConfig.java` - Embedded 모드 설정 (패키지 이동)
- `src/test/java/gift/docker/DockerCucumberSpringConfig.java` - Docker 모드 설정
- `src/test/java/gift/DockerCucumberTest.java` - Docker Cucumber 테스트 러너
- `src/test/resources/application-docker.properties` - Docker 프로파일 설정

### 수정 파일
- `docker-compose.yml` - app 서비스 추가
- `build.gradle` - Docker 관련 Gradle 태스크 추가
- `src/test/java/gift/CucumberTest.java` - glue 경로 수정
- `src/test/resources/cucumber.properties` - glue 경로 수정

### 삭제 파일
- `src/test/java/gift/steps/CucumberSpringConfig.java` - `gift.config` 패키지로 이동
