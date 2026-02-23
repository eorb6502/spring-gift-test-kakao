package gift.steps;

import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만약;
import io.cucumber.java.ko.먼저;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductSteps {

    @Autowired
    private SharedContext context;

    private final List<ExtractableResponse<Response>> productRegistrationResponses = new ArrayList<>();

    @만약("상품 {string}를 가격 {int}원, 이미지 {string}로 카테고리 {string}에 등록한다")
    public void 상품_등록(String 상품명, int 가격, String 이미지, String 카테고리명) {
        Long categoryId = context.getCategoryId(카테고리명);

        ExtractableResponse<Response> response = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", 상품명,
                "price", 가격,
                "imageUrl", 이미지,
                "categoryId", categoryId
            ))
            .when()
            .post("/api/products")
            .then()
            .extract();

        productRegistrationResponses.add(response);

        if (response.statusCode() == HttpStatus.OK.value()) {
            Long productId = response.jsonPath().getLong("id");
            context.setProductId(상품명, productId);
        }
    }

    @먼저("존재하지 않는 카테고리 ID {int}가 있다")
    public void 존재하지_않는_카테고리(int 카테고리ID) {
        // 아무것도 하지 않음 - 단순히 시나리오 가독성을 위한 단계
    }

    @만약("상품 {string}를 가격 {int}원, 이미지 {string}로 카테고리 ID {int}에 등록한다")
    public void 잘못된_카테고리로_상품_등록(String 상품명, int 가격, String 이미지, int 카테고리ID) {
        ExtractableResponse<Response> response = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", 상품명,
                "price", 가격,
                "imageUrl", 이미지,
                "categoryId", 카테고리ID
            ))
            .when()
            .post("/api/products")
            .then()
            .extract();

        context.setLastResponse(response);
    }

    @만약("전체 상품을 조회한다")
    public void 전체_상품_조회() {
        ExtractableResponse<Response> response = RestAssured.given()
            .when()
            .get("/api/products")
            .then()
            .extract();

        context.setLastResponse(response);
    }

    @그러면("모든 상품 등록이 성공한다")
    public void 모든_상품_등록_성공_확인() {
        for (ExtractableResponse<Response> response : productRegistrationResponses) {
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        }
    }

    @그러면("상품 조회가 성공한다")
    public void 상품_조회_성공_확인() {
        assertThat(context.getLastResponse().statusCode())
            .isEqualTo(HttpStatus.OK.value());
    }

    @그리고("조회된 상품 목록에는 {string}, {string}, {string}가 포함되어 있다")
    public void 상품_목록_확인(String 상품1, String 상품2, String 상품3) {
        List<String> productNames = context.getLastResponse()
            .jsonPath()
            .getList("name", String.class);

        assertThat(productNames).containsExactlyInAnyOrder(상품1, 상품2, 상품3);
    }

    @그러면("상품 등록이 실패한다")
    public void 상품_등록_실패_확인() {
        assertThat(context.getLastResponse().statusCode())
            .isEqualTo(HttpStatus.NOT_FOUND.value());
    }
}
