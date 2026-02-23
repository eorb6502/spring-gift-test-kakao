package gift.steps;

import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만약;
import io.cucumber.java.ko.먼저;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class OptionSteps {

    @Autowired
    private SharedContext context;

    @만약("옵션 {string}의 정보를 조회한다")
    public void 옵션_정보_조회(String 옵션명) {
        Long optionId = context.getOptionId(옵션명);

        ExtractableResponse<Response> response = RestAssured.given()
            .when()
            .get("/api/options/{id}", optionId)
            .then()
            .extract();

        context.setLastResponse(response);
    }

    @그러면("옵션 조회가 성공한다")
    public void 옵션_조회_성공_확인() {
        assertThat(context.getLastResponse().statusCode())
            .isEqualTo(HttpStatus.OK.value());
    }

    @그리고("조회된 옵션의 이름은 {string}이고 재고는 {int}개이다")
    public void 옵션_정보_확인(String 예상이름, int 예상재고) {
        ExtractableResponse<Response> response = context.getLastResponse();
        assertThat(response.jsonPath().getString("name")).isEqualTo(예상이름);
        assertThat(response.jsonPath().getInt("quantity")).isEqualTo(예상재고);
    }

    @먼저("존재하지 않는 상품 ID {int}가 있다")
    public void 존재하지_않는_상품(int 상품ID) {
        // 아무것도 하지 않음 - 단순히 시나리오 가독성을 위한 단계
    }

    @만약("옵션 {string}를 재고 {int}개로 상품 ID {int}에 등록한다")
    public void 잘못된_상품에_옵션_등록(String 옵션명, int 재고, int 상품ID) {
        ExtractableResponse<Response> response = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", 옵션명,
                "quantity", 재고,
                "productId", 상품ID
            ))
            .when()
            .post("/api/options")
            .then()
            .extract();

        context.setLastResponse(response);
    }

    @그러면("옵션 등록이 실패한다")
    public void 옵션_등록_실패_확인() {
        assertThat(context.getLastResponse().statusCode())
            .isEqualTo(HttpStatus.NOT_FOUND.value());
    }
}
