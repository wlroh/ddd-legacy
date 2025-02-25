package kitchenpos.acceptance;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import kitchenpos.domain.MenuGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static kitchenpos.acceptance.MenuGroupSteps.메뉴그룹_등록_요청;
import static kitchenpos.acceptance.MenuGroupSteps.메뉴그룹_목록_조회_요청;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("메뉴그룹 관련 기능")
public class MenuGroupAcceptanceTest extends AcceptanceTest {

    @DisplayName("메뉴그룹을 등록한다.")
    @Test
    void addMenuGroup() {
        // when
        메뉴그룹_등록_요청함("추천메뉴");

        // then
        var 메뉴그룹_목록 = 메뉴그룹_목록_조회_요청함();
        메뉴그룹이_조회됨(메뉴그룹_목록, "추천메뉴");
    }

    @DisplayName("메뉴그룹 목록을 조회한다.")
    @Test
    void showProducts() {
        // given
        메뉴그룹이_등록됨("추천메뉴");
        메뉴그룹이_등록됨("신메뉴");

        // when
        var 메뉴그룹_목록 = 메뉴그룹_목록_조회_요청함();

        // then
        메뉴그룹이_조회됨(메뉴그룹_목록, "추천메뉴", "신메뉴");
    }

    private ExtractableResponse<Response> 메뉴그룹_등록_요청함(final String name) {
        final MenuGroup menuGroup = new MenuGroup();
        menuGroup.setName(name);

        return 메뉴그룹_등록_요청(given(), menuGroup);
    }

    private ExtractableResponse<Response> 메뉴그룹_목록_조회_요청함() {
        return 메뉴그룹_목록_조회_요청(given());
    }

    private UUID 메뉴그룹이_등록됨(final String name) {
        return 메뉴그룹_등록_요청함(name).jsonPath().getUUID("id");
    }

    private void 메뉴그룹이_조회됨(final ExtractableResponse<Response> response, final String... name) {
        assertThat(response.jsonPath().getList("name", String.class)).contains(name);
    }
}
