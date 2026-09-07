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
            if (scenario.startsWith("keyword")) client.searchByKeyword("카페", 1, 15, "accuracy", null, null, null);
            else client.searchByCategory("CE7", "126,33,127,34", 1, 15);
        }).isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.KAKAO_LOCAL_API_ERROR);
        server.verify();
    }
}
