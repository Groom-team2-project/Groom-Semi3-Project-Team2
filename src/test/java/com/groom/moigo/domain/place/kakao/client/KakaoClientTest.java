package com.groom.moigo.domain.place.kakao.client;

import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import java.net.SocketTimeoutException;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class KakaoClientTest {
    @ParameterizedTest(name = "외부 API 오류 변환: {0}")
    @ValueSource(strings = {"keyword:500", "category:500", "keyword:timeout", "category:timeout",
            "keyword:empty", "category:empty"})
    void translatesUpstreamFailures(String scenario) {
        var builder = RestClient.builder().baseUrl("https://dapi.kakao.com");
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new KakaoClient(builder.build());
        var expectation = server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://dapi.kakao.com/v2/local/search/")));
        if (scenario.endsWith("timeout")) expectation.andRespond(withException(new SocketTimeoutException("test timeout")));
        else if (scenario.endsWith("empty")) expectation.andRespond(withSuccess());
        else expectation.andRespond(withServerError());
        assertThatThrownBy(() -> {
            if (scenario.startsWith("keyword")) client.searchByKeyword("카페", 1, 15, "accuracy", null, null, null, null, null);
            else client.searchByCategory("CE7", "126,33,127,34", 1, 15);
        }).isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.KAKAO_LOCAL_API_ERROR);
        server.verify();
    }

    @ParameterizedTest(name = "카카오 요청 선택 조건 포함·생략: {0}")
    @ValueSource(strings = {"none", "category", "bounds", "both"})
    void sendsOptionalFiltersExactlyOnce(String combination) {
        var builder = RestClient.builder().baseUrl("https://dapi.kakao.com");
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new KakaoClient(builder.build());
        String category = combination.equals("category") || combination.equals("both") ? "CE7" : null;
        String rect = combination.equals("bounds") || combination.equals("both") ? "126,33,127,34" : null;
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://dapi.kakao.com/v2/local/search/keyword.json")))
                .andExpect(request -> {
                    var query = org.springframework.web.util.UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
                    assertThat(query.get("query")).containsExactly("cafe");
                    assertThat(query.get("page")).containsExactly("2");
                    assertThat(query.get("size")).containsExactly("5");
                    assertThat(query.get("sort")).containsExactly("accuracy");
                    if (category == null) assertThat(query).doesNotContainKey("category_group_code");
                    else assertThat(query.get("category_group_code")).containsExactly(category);
                    if (rect == null) assertThat(query).doesNotContainKey("rect");
                    else assertThat(query.get("rect")).containsExactly(rect);
                    assertThat(query).doesNotContainKeys("x", "y", "radius");
                })
                .andRespond(withSuccess("""
                        {"documents":[],"meta":{"total_count":0,"pageable_count":0,"is_end":true}}
                        """, org.springframework.http.MediaType.APPLICATION_JSON));
        var result = client.searchByKeyword("cafe", 2, 5, "accuracy", null, null, null, category, rect);
        assertThat(result.getDocuments()).isEmpty();
        assertThat(result.getMeta().isEnd()).isTrue();
        server.verify();
    }
}
