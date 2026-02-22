# Code Review by Leo

> "테스트 코드는 시스템의 살아있는 문서입니다."

## 총평

전반적으로 인수 테스트의 기본 골격이 잘 잡혀 있습니다. 한글 헬퍼 메서드를 활용한 가독성, Given-When-Then 구조, `DatabaseCleaner`를 통한 테스트 격리 등 좋은 패턴을 따르고 있습니다. 다만 Outside-In 원칙의 일부 위반, `DatabaseCleaner`의 잠재적 결함, 그리고 프로덕션 코드에서의 설계 피드백 포인트가 존재합니다.

---

## 1. 명세로서의 가치 (Specification)

### [PASS] `@DisplayName` - 비즈니스 용어 사용

```
"관리자는 카테고리를 생성하고 상품을 등록하여 판매를 시작한다"
"사용자가 상품을 선택해 자신에게 선물하면, 해당 옵션의 재고 수량이 감소한다"
"옵션 수량이 부족하면 선물 요청이 거부된다"
```

기술 용어(API, 200, POST) 없이 비즈니스 행위 중심으로 잘 작성되어 있습니다.

### [PASS] Given-When-Then 구조

주석으로 `// given`, `// when`, `// then`이 명확히 구분되어 있고, 각 단계의 역할이 일관됩니다.

### [PASS] 가독성 - 헬퍼 메서드

`카테고리를_생성한다()`, `상품을_등록한다()`, `선물을_보낸다()` 등 한글 메서드명을 활용해 테스트 본문이 명세서처럼 읽힙니다.

---

## 2. Outside-In 원칙 (Black-box Testing)

### 🟡 `GiftAcceptanceTest` - `MemberRepository` 직접 사용

**파일:** `GiftAcceptanceTest.java:30`

```java
@Autowired
MemberRepository memberRepository;

// ...
Member sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
```

인수 테스트는 **오직 HTTP 인터페이스**를 통해서만 시스템과 상호작용해야 합니다. `MemberRepository`를 직접 주입받아 데이터를 세팅하는 것은 내부 구현에 의존하는 것입니다.

**개선 방향:** Member 생성 API(`POST /api/members`)를 프로덕션 코드에 추가하고, 테스트에서는 HTTP 요청으로 회원을 생성해야 합니다.

```java
// Before (내부 구현 의존)
Member sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));

// After (Outside-In)
Long senderId = 회원을_생성한다("보내는사람", "sender@test.com");
```

이것은 REVIEWER.md의 핵심 원칙인 **"내부 구현(Repository, Service)을 직접 호출하지 않고 오직 HTTP 요청/응답으로만 검증"**에 해당합니다.

---

## 3. 테스트 격리 및 신뢰성 (Reliability)

### 🔴 `DatabaseCleaner` - AUTO_INCREMENT 초기화 누락

**파일:** `DatabaseCleaner.java:19-24`

```java
public void clear() {
    entityManager.flush();
    entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
    for (final String tableName : getTableNames()) {
        entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
    }
    entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
}
```

`TRUNCATE TABLE`은 데이터를 삭제하지만, H2에서 **ID 자동 증가 시퀀스를 초기화하지 않을 수 있습니다.** 테스트가 특정 ID 값에 의존하지 않으므로 현재는 문제가 되지 않지만, REVIEWER.md 체크리스트에서 요구하는 **"완벽히 초기화(Truncate & ID Restart)"**를 충족하려면 시퀀스 리셋이 필요합니다.

**개선 방향:**

```java
for (final String tableName : getTableNames()) {
    entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
    entityManager.createNativeQuery(
        "ALTER TABLE " + tableName + " ALTER COLUMN ID RESTART WITH 1"
    ).executeUpdate();
}
```

### 🟡 `DatabaseCleaner` - SQL Injection 가능성

**파일:** `DatabaseCleaner.java:22`

```java
entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
```

`getTableNames()`가 JPA 메타모델에서 엔티티명을 가져오므로 현재는 안전하지만, 엔티티명과 테이블명이 다를 경우(예: `@Table(name = "...")`) 의도치 않은 동작이 발생할 수 있습니다. 문자열 연결로 네이티브 쿼리를 구성하는 패턴 자체가 잠재적 위험을 내포합니다.

### [PASS] 순서 의존성 제거

각 테스트가 `@BeforeEach`에서 DB를 초기화하고, 자체적으로 필요한 데이터를 생성하므로 테스트 간 독립성이 잘 보장됩니다.

### [PASS] 데이터 일관성 - 실패 시나리오 검증

`재고_부족_시_선물_불가` 테스트에서 요청 실패 후 재고가 변하지 않았는지까지 검증하고 있어 사이드 이펙트 부재를 확인합니다.

```java
assertThat(remainingQuantity).isEqualTo(1); // 재고 불변 확인
```

---

## 4. 설계 피드백 (Design Feedback)

### 🟡 JPA 엔티티 직접 반환 - 순환 참조 위험

**파일:** `GiftRestController.java:21`, `ProductRestController.java:27` 등

```java
@GetMapping("/{id}")
public Gift retrieveById(@PathVariable Long id) {
    return giftService.retrieveById(id);
}
```

모든 Controller가 JPA 엔티티를 **직접 JSON으로 직렬화**합니다. `Gift` -> `Option` -> `Product` -> `Category` 처럼 양방향 관계가 추가될 경우 **Jackson 순환 참조 에러(`StackOverflowError`)**가 발생합니다. 또한 내부 엔티티 구조가 API 응답 스펙에 그대로 노출되어 API 변경에 취약합니다.

**개선 방향:** Response DTO를 도입하여 API 계약과 도메인 모델을 분리하세요.

### 🟡 `GiftService.give()` - 트랜잭션 내 외부 호출

**파일:** `GiftService.java:34-42`

```java
@Transactional
public Gift give(final GiveGiftRequest request, final Long memberId) {
    // ... DB 작업 ...
    giftRepository.save(gift);
    giftDelivery.deliver(gift);  // 외부 시스템 호출
    return gift;
}
```

`giftDelivery.deliver(gift)`가 트랜잭션 내부에서 호출됩니다. 현재는 `FakeGiftDelivery`이지만, 실제 카카오 API 연동 시:
- 외부 API 호출 실패 시 전체 트랜잭션이 롤백되어 선물 기록이 사라집니다.
- 외부 API 응답 지연 시 DB 커넥션을 불필요하게 점유합니다.

**개선 방향:** 이벤트 기반 분리(`@TransactionalEventListener`) 또는 트랜잭션 커밋 후 별도 처리를 고려하세요.

### 🟢 `Option.decrease()` - 동시성 미보장

**파일:** `Option.java:30-34`

```java
public void decrease(final int quantity) {
    if (this.quantity < quantity) {
        throw new IllegalStateException("재고가 부족합니다. 현재 수량: " + this.quantity);
    }
    this.quantity -= quantity;
}
```

동시에 여러 사용자가 같은 옵션으로 선물을 보내면 **재고 초과 차감(Lost Update)**이 발생할 수 있습니다. `@Version`을 통한 낙관적 락 또는 `@Lock(PESSIMISTIC_WRITE)` 쿼리를 고려하세요.

### 🟢 Request DTO - 입력 검증 부재

**파일:** `CreateProductRequest.java`, `GiveGiftRequest.java` 등

모든 Request DTO에 `@NotNull`, `@NotBlank`, `@Positive` 등의 Bean Validation이 없습니다. 음수 가격, 빈 이름, 0 이하 수량 등 비정상 입력이 그대로 시스템에 저장됩니다.

---

## 리뷰 체크리스트 요약

| 항목 | 상태 | 비고 |
|------|------|------|
| 비즈니스 용어 `@DisplayName` | ✅ | 잘 작성됨 |
| Given-When-Then 구조 | ✅ | 명확한 구분 |
| 헬퍼 메서드 가독성 | ✅ | 한글 메서드명 활용 |
| 인터페이스 중심 검증 | ⚠️ | `MemberRepository` 직접 사용 |
| 간접 상태 관찰 | ✅ | 조회 API로 상태 확인 |
| DB 완벽 초기화 | ❌ | ID 시퀀스 미초기화 |
| 순서 의존성 제거 | ✅ | 독립적 테스트 |
| 데이터 일관성 | ✅ | 실패 후 사이드이펙트 검증 |
| 테스트 용이성 설계 | ⚠️ | Member API 미존재 |
| 추상화 수준 | ✅ | 적절한 수준 |

---

## 레오의 한마디

> "테스트의 뼈대는 훌륭합니다. 특히 실패 시나리오에서 재고 불변을 검증하는 것은 많은 주니어가 놓치는 포인트인데 잘 챙겼습니다. 다만 **`MemberRepository` 직접 주입은 Outside-In 원칙의 명백한 위반**입니다. 인수 테스트에서 `@Autowired Repository`가 보이는 순간 '이 시스템에는 이 기능을 위한 API가 빠져있구나'라는 설계 피드백으로 받아들이세요. 회원 생성 API를 만들고, 테스트를 순수하게 HTTP로만 구성하면 한 단계 더 성장한 인수 테스트가 됩니다."
