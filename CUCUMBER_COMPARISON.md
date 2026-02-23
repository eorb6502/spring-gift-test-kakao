# RestAssured vs Cucumber BDD 비교

## 왜 Cucumber로 전환하는가?

### 핵심 목표
**"비개발자도 이해할 수 있는 테스트 시나리오"**

기존 RestAssured 테스트는 개발자만 읽을 수 있지만, Cucumber는 기획자, QA, 도메인 전문가도 읽고 이해할 수 있습니다.

---

## 1. 테스트 시나리오 표현 방식 비교

### 기존 방식 (RestAssured + JUnit)

```java
@DisplayName("사용자가 상품을 선택해 자신에게 선물하면, 해당 옵션의 재고 수량이 감소한다")
@Test
void 나에게_선물하기_재고_차감() {
    // given
    Member sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
    Long categoryId = 카테고리를_생성한다("음료");
    Long productId = 상품을_등록한다("아메리카노", 500, "/img/ame", categoryId);
    Long optionId = 옵션을_등록한다("ICE", 10, productId);

    // when
    선물을_보낸다(sender.getId(), optionId, 1, sender.getId(), "나에게 주는 선물");

    // then
    int remainingQuantity = 옵션을_조회한다(optionId).jsonPath().getInt("quantity");
    assertThat(remainingQuantity).isEqualTo(9);
}
```

**특징:**
- ✅ 개발자에게 익숙한 구조
- ✅ 한글 메서드명으로 가독성 확보
- ❌ Java 문법 지식 필요 (변수, 타입, 메서드 호출)
- ❌ 비즈니스 로직과 기술적 세부사항이 섞임
- ❌ 비개발자가 읽고 이해하기 어려움

---

### Cucumber 방식 (Gherkin Feature + Step Definitions)

**Feature 파일 (비즈니스 시나리오) - gift.feature**

```gherkin
# language: ko
기능: 선물하기
  사용자는 상품을 선택하여 다른 사용자에게 선물을 보낼 수 있다.
  선물이 발송되면 해당 옵션의 재고가 차감된다.

  시나리오: 나에게 선물하기 - 재고 차감
    먼저 사용자 "보내는사람"이 이메일 "sender@test.com"로 등록되어 있다
    그리고 카테고리 "음료"가 생성되어 있다
    그리고 상품 "아메리카노"가 가격 500원, 이미지 "/img/ame"로 카테고리 "음료"에 등록되어 있다
    그리고 옵션 "ICE"가 재고 10개로 상품 "아메리카노"에 등록되어 있다
    만약 사용자 "보내는사람"이 옵션 "ICE"를 1개 선택하여 자신에게 "나에게 주는 선물" 메시지로 선물을 보낸다
    그러면 옵션 "ICE"의 재고는 9개이다
```

**Step Definitions (기술 구현) - CommonSteps.java, GiftSteps.java**

```java
// CommonSteps.java - 공통 Given 단계
@먼저("사용자 {string}이 이메일 {string}로 등록되어 있다")
public void 사용자_등록(String 이름, String 이메일) {
    Member member = memberRepository.save(new Member(이름, 이메일));
    context.setMember(이름, member);
}

@그리고("옵션 {string}가 재고 {int}개로 상품 {string}에 등록되어 있다")
public void 옵션_등록_완료(String 옵션명, int 재고, String 상품명) {
    Long productId = context.getProductId(상품명);
    Long optionId = RestAssured.given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", 옵션명, "quantity", 재고, "productId", productId))
        .when()
        .post("/api/options")
        .then()
        .statusCode(HttpStatus.OK.value())
        .extract()
        .jsonPath().getLong("id");

    context.setOptionId(옵션명, optionId);
}

// GiftSteps.java - 선물 관련 When/Then 단계
@만약("사용자 {string}이 옵션 {string}를 {int}개 선택하여 자신에게 {string} 메시지로 선물을 보낸다")
public void 자신에게_선물_보내기(String 사용자명, String 옵션명, int 수량, String 메시지) {
    Member sender = context.getMember(사용자명);
    Long optionId = context.getOptionId(옵션명);

    ExtractableResponse<Response> response = RestAssured.given()
        .contentType(ContentType.JSON)
        .header("Member-Id", sender.getId())
        .body(Map.of(
            "optionId", optionId,
            "quantity", 수량,
            "receiverId", sender.getId(),
            "message", 메시지
        ))
        .when()
        .post("/api/gifts")
        .then()
        .extract();

    context.setLastResponse(response);
    if (response.statusCode() == HttpStatus.OK.value()) {
        context.setLastGiftId(response.jsonPath().getLong("id"));
    }
}

@그러면("옵션 {string}의 재고는 {int}개이다")
public void 재고_확인(String 옵션명, int 예상재고) {
    Long optionId = context.getOptionId(옵션명);

    int actualQuantity = RestAssured.given()
        .when()
        .get("/api/options/{id}", optionId)
        .then()
        .statusCode(HttpStatus.OK.value())
        .extract()
        .jsonPath().getInt("quantity");

    assertThat(actualQuantity).isEqualTo(예상재고);
}
```

**특징:**
- ✅ **비개발자도 읽을 수 있는 시나리오** (Feature 파일)
- ✅ **비즈니스 로직과 기술 구현의 분리**
- ✅ 재사용 가능한 Step Definitions
- ✅ 자연어로 작성된 명세서 역할
- ❌ 초기 설정이 복잡함
- ❌ Step Definitions 작성 필요

---

## 2. 구조 차이

### 기존 방식 (삭제됨)
```
테스트 파일 1개 = 시나리오 + 구현 + 검증
모든 것이 Java 코드에 섞여 있음
```

**파일 구조 (현재는 삭제됨):**
```
src/test/java/gift/
├── GiftAcceptanceTest.java    (시나리오 3개 + 헬퍼 메서드) [DELETED]
└── ProductAcceptanceTest.java (시나리오 3개 + 헬퍼 메서드) [DELETED]
```

---

### Cucumber 방식 (현재 구조)
```
Feature 파일 (시나리오) + Step Definitions (구현) + Context (상태 공유)
역할이 명확히 분리됨
```

**파일 구조:**
```
src/test/
├── java/gift/
│   ├── CucumberTest.java              [JUnit Platform Suite Runner]
│   ├── DatabaseCleaner.java           [DB 초기화 - 기존 파일 재사용]
│   └── steps/
│       ├── CucumberSpringConfig.java  [Spring Boot 통합 설정]
│       ├── SharedContext.java         [시나리오 간 상태 공유 (@ScenarioScope)]
│       ├── CommonSteps.java           [공통 Step - 사용자/카테고리/상품/옵션]
│       ├── GiftSteps.java             [선물 관련 Step 구현]
│       └── ProductSteps.java          [상품 관련 Step 구현]
└── resources/features/
    ├── gift.feature                   [선물 시나리오 3개]
    └── product.feature                [상품 시나리오 3개]
```

---

## 3. 실제 변경 예시

### 테스트 시나리오: "재고 부족 시 선물 불가"

#### Before (RestAssured)
```java
@DisplayName("옵션 수량이 부족하면 선물 요청이 거부된다")
@Test
void 재고_부족_시_선물_불가() {
    // given
    Member sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
    Long categoryId = 카테고리를_생성한다("음료");
    Long productId = 상품을_등록한다("아메리카노", 500, "/img/ame", categoryId);
    Long optionId = 옵션을_등록한다("ICE", 1, productId);

    // when
    ExtractableResponse<Response> response = 선물을_보낸다(sender.getId(), optionId, 2, sender.getId(), "수량 초과");

    // then
    assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());

    int remainingQuantity = 옵션을_조회한다(optionId).jsonPath().getInt("quantity");
    assertThat(remainingQuantity).isEqualTo(1);
}
```

**누가 읽을 수 있나?** Java를 아는 개발자만

---

#### After (Cucumber)

**gift.feature (누구나 읽을 수 있음)**
```gherkin
시나리오: 재고 부족 시 선물 불가
  먼저 사용자 "보내는사람"이 이메일 "sender@test.com"로 등록되어 있다
  그리고 카테고리 "음료"가 생성되어 있다
  그리고 상품 "아메리카노"가 가격 500원, 이미지 "/img/ame"로 카테고리 "음료"에 등록되어 있다
  그리고 옵션 "ICE"가 재고 1개로 상품 "아메리카노"에 등록되어 있다
  만약 사용자 "보내는사람"이 옵션 "ICE"를 2개 선택하여 자신에게 "수량 초과" 메시지로 선물을 보낸다
  그러면 선물 요청이 실패한다
  그리고 옵션 "ICE"의 재고는 1개이다
```

**누가 읽을 수 있나?** 기획자, QA, 도메인 전문가, 고객사 담당자 모두

**GiftSteps.java (개발자만 신경 쓰면 됨)**
```java
@그러면("선물 요청이 실패한다")
public void 요청_실패_확인() {
    assertThat(context.getLastResponse().statusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST.value());
}
```

---

## 4. 핵심 차이점 요약

| 항목 | RestAssured | Cucumber BDD |
|------|------------|--------------|
| **가독성** | 개발자만 읽을 수 있음 | 누구나 읽을 수 있음 |
| **문서화** | 코드가 곧 문서 | Feature가 살아있는 명세서 |
| **역할 분리** | 시나리오+구현 혼재 | Feature(시나리오) ↔ Steps(구현) |
| **재사용성** | 메서드 재사용 | Step 재사용 (다른 시나리오에서도) |
| **협업** | 개발자 중심 | 전 팀원 참여 가능 |
| **설정 복잡도** | 단순 (JUnit만) | 복잡 (Cucumber 설정 필요) |
| **학습 곡선** | 낮음 | 중간 (Gherkin 문법) |
| **유지보수** | Java 코드 수정 | Feature 수정 (비개발자도 가능) |

---

## 5. Cucumber의 핵심 개념

### Gherkin 키워드 (한글)
- **기능**: 테스트하려는 기능 설명
- **시나리오**: 구체적인 테스트 케이스
- **먼저/주어진** (Given): 초기 상태 설정
- **그리고** (And): 추가 조건
- **만약** (When): 실제 액션 수행
- **그러면** (Then): 결과 검증

### Step Definitions
- Feature 파일의 각 단계를 실제 코드로 구현
- 파라미터 추출 가능 (`{string}`, `{int}` 등)
- 여러 시나리오에서 재사용 가능

### SharedContext (@ScenarioScope)
- 시나리오 내에서 데이터 공유
- Given에서 생성한 데이터를 When/Then에서 사용
- 각 시나리오마다 독립적으로 생성/삭제

---

## 6. 실제로 얻을 수 있는 것

### Before
```
PM: "재고 부족 시 어떻게 동작하나요?"
개발자: "코드를 보시면... (Java 파일 열기)"
PM: "음... 잘 모르겠는데요"
```

### After
```
PM: "재고 부족 시 어떻게 동작하나요?"
개발자: "gift.feature 파일을 보세요"
PM: (읽고) "아, 요청이 실패하고 재고는 그대로 유지되는군요!"
```

---

## 7. 변경 후 실행 방법

### 기존
```bash
./gradlew test
```

### Cucumber
```bash
./gradlew test
# 또는
./gradlew cucumberTest  (별도 task 생성 시)
```

**실행 결과도 달라집니다:**

**기존 (현재는 삭제됨):**
```
GiftAcceptanceTest > 나에게_선물하기_재고_차감() PASSED
GiftAcceptanceTest > 재고_부족_시_선물_불가() PASSED
ProductAcceptanceTest > 카테고리_기반_상품_진열() PASSED
```

**Cucumber (현재 구조):**
```
CucumberTest > Cucumber > 선물하기 > 나에게 선물하기 - 재고 차감 PASSED
CucumberTest > Cucumber > 선물하기 > 재고 부족 시 선물 불가 PASSED
CucumberTest > Cucumber > 선물하기 > 선물 발송 데이터 일관성 PASSED
CucumberTest > Cucumber > 상품 관리 > 카테고리 기반 상품 진열 PASSED
CucumberTest > Cucumber > 상품 관리 > 전체 상품 목록 조회 PASSED
CucumberTest > Cucumber > 상품 관리 > 유효하지 않은 카테고리로 상품 등록 PASSED

BUILD SUCCESSFUL
```

Feature 파일을 열면 더 자세한 실행 과정을 볼 수 있습니다!

---

## 결론

**RestAssured → Cucumber 전환 = 테스트를 코드에서 '명세서'로 승격**

- 코드는 변하지 않지만, 표현 방식이 완전히 달라집니다
- 비개발자와의 협업이 가능해집니다
- 테스트가 곧 문서이자 요구사항이 됩니다

이것이 BDD (Behavior-Driven Development)의 핵심입니다.
