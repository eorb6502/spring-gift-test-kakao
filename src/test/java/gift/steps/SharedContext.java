package gift.steps;

import gift.model.Member;
import io.cucumber.spring.ScenarioScope;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ScenarioScope
public class SharedContext {
    private final Map<String, Member> members = new HashMap<>();
    private final Map<String, Long> categoryIds = new HashMap<>();
    private final Map<String, Long> productIds = new HashMap<>();
    private final Map<String, Long> optionIds = new HashMap<>();
    private ExtractableResponse<Response> lastResponse;
    private Long lastGiftId;

    public void setMember(String name, Member member) {
        members.put(name, member);
    }

    public Member getMember(String name) {
        return members.get(name);
    }

    public void setCategoryId(String name, Long id) {
        categoryIds.put(name, id);
    }

    public Long getCategoryId(String name) {
        return categoryIds.get(name);
    }

    public void setProductId(String name, Long id) {
        productIds.put(name, id);
    }

    public Long getProductId(String name) {
        return productIds.get(name);
    }

    public void setOptionId(String name, Long id) {
        optionIds.put(name, id);
    }

    public Long getOptionId(String name) {
        return optionIds.get(name);
    }

    public void setLastResponse(ExtractableResponse<Response> response) {
        this.lastResponse = response;
    }

    public ExtractableResponse<Response> getLastResponse() {
        return lastResponse;
    }

    public void setLastGiftId(Long giftId) {
        this.lastGiftId = giftId;
    }

    public Long getLastGiftId() {
        return lastGiftId;
    }
}
