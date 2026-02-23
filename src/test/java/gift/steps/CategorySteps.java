package gift.steps;

import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만약;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CategorySteps {

    @Autowired
    private SharedContext context;

    @만약("전체 카테고리를 조회한다")
    public void 전체_카테고리_조회() {
        ExtractableResponse<Response> response = RestAssured.given()
            .when()
            .get("/api/categories")
            .then()
            .extract();

        context.setLastResponse(response);
    }

    @그러면("카테고리 조회가 성공한다")
    public void 카테고리_조회_성공_확인() {
        assertThat(context.getLastResponse().statusCode())
            .isEqualTo(HttpStatus.OK.value());
    }

    @그리고("조회된 카테고리 목록에는 {string}가 포함되어 있다")
    public void 카테고리_목록_확인(String 카테고리명) {
        List<String> categoryNames = context.getLastResponse()
            .jsonPath()
            .getList("name", String.class);

        assertThat(categoryNames).contains(카테고리명);
    }

    @그리고("조회된 카테고리 목록에는 {string}, {string}가 포함되어 있다")
    public void 카테고리_목록_복수_확인(String 카테고리1, String 카테고리2) {
        List<String> categoryNames = context.getLastResponse()
            .jsonPath()
            .getList("name", String.class);

        assertThat(categoryNames).contains(카테고리1, 카테고리2);
    }
}
