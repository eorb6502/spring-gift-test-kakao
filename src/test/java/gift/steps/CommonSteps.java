package gift.steps;

import gift.model.Member;
import gift.model.MemberRepository;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.먼저;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class CommonSteps {

    @Autowired
    private SharedContext context;

    @Autowired
    private MemberRepository memberRepository;

    @먼저("사용자 {string}이 이메일 {string}로 등록되어 있다")
    public void 사용자_등록(String 이름, String 이메일) {
        Member member = memberRepository.save(new Member(이름, 이메일));
        context.setMember(이름, member);
    }

    @먼저("카테고리 {string}가 생성되어 있다")
    public void 카테고리_생성(String 카테고리명) {
        Long categoryId = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", 카테고리명))
            .when()
            .post("/api/categories")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .jsonPath().getLong("id");

        context.setCategoryId(카테고리명, categoryId);
    }

    @그리고("상품 {string}가 가격 {int}원, 이미지 {string}로 카테고리 {string}에 등록되어 있다")
    public void 상품_등록_완료(String 상품명, int 가격, String 이미지, String 카테고리명) {
        Long categoryId = context.getCategoryId(카테고리명);
        Long productId = RestAssured.given()
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
            .statusCode(HttpStatus.OK.value())
            .extract()
            .jsonPath().getLong("id");

        context.setProductId(상품명, productId);
    }

    @그리고("옵션 {string}가 재고 {int}개로 상품 {string}에 등록되어 있다")
    public void 옵션_등록_완료(String 옵션명, int 재고, String 상품명) {
        Long productId = context.getProductId(상품명);
        Long optionId = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", 옵션명,
                "quantity", 재고,
                "productId", productId
            ))
            .when()
            .post("/api/options")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .jsonPath().getLong("id");

        context.setOptionId(옵션명, optionId);
    }
}
