package gift.steps;

import gift.model.Member;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만약;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GiftSteps {

    @Autowired
    private SharedContext context;

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
            Long giftId = response.jsonPath().getLong("id");
            context.setLastGiftId(giftId);
        }
    }

    @만약("사용자 {string}이 옵션 {string}를 {int}개 선택하여 사용자 {string}에게 {string} 메시지로 선물을 보낸다")
    public void 다른_사용자에게_선물_보내기(String 발신자명, String 옵션명, int 수량, String 수신자명, String 메시지) {
        Member sender = context.getMember(발신자명);
        Member receiver = context.getMember(수신자명);
        Long optionId = context.getOptionId(옵션명);

        ExtractableResponse<Response> response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Member-Id", sender.getId())
            .body(Map.of(
                "optionId", optionId,
                "quantity", 수량,
                "receiverId", receiver.getId(),
                "message", 메시지
            ))
            .when()
            .post("/api/gifts")
            .then()
            .extract();

        context.setLastResponse(response);
        if (response.statusCode() == HttpStatus.OK.value()) {
            Long giftId = response.jsonPath().getLong("id");
            context.setLastGiftId(giftId);
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

    @그러면("선물 요청이 실패한다")
    public void 요청_실패_확인() {
        assertThat(context.getLastResponse().statusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @그러면("선물 요청이 성공한다")
    public void 요청_성공_확인() {
        assertThat(context.getLastResponse().statusCode())
            .isEqualTo(HttpStatus.OK.value());
    }

    @그리고("발송된 선물의 수신자는 {string}이다")
    public void 선물_수신자_확인(String 예상수신자명) {
        Long giftId = context.getLastGiftId();
        Member expectedReceiver = context.getMember(예상수신자명);

        Long actualReceiverId = RestAssured.given()
            .when()
            .get("/api/gifts/{id}", giftId)
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .jsonPath().getLong("receiver.id");

        assertThat(actualReceiverId).isEqualTo(expectedReceiver.getId());
    }

    @그리고("발송된 선물의 메시지는 {string}이다")
    public void 선물_메시지_확인(String 예상메시지) {
        Long giftId = context.getLastGiftId();

        String actualMessage = RestAssured.given()
            .when()
            .get("/api/gifts/{id}", giftId)
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .jsonPath().getString("message");

        assertThat(actualMessage).isEqualTo(예상메시지);
    }
}
