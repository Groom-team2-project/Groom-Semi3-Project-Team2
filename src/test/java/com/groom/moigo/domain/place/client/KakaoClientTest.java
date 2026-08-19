package com.groom.moigo.domain.place.client;

import com.groom.moigo.domain.place.dto.KakaoSearchResponse;
import com.groom.moigo.domain.place.exception.PlaceErrorCode;
import com.groom.moigo.domain.place.exception.PlaceException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KakaoClientTest {
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("정상 카카오 검색 응답을 역직렬화한다")
    void readsSuccessfulResponse() throws Exception {
        startServer(exchange -> respond(exchange, 200, """
                {"meta":{"total_count":1,"pageable_count":1,"is_end":true},
                 "documents":[{"id":"1","place_name":"성수 카페","x":"127.1","y":"37.5"}]}
                """));

        KakaoSearchResponse response = client(1_000).searchByKeyword(
                "카페", 1, 15, "accuracy", null, null, null);

        assertThat(response.getDocuments()).hasSize(1);
        assertThat(response.getDocuments().getFirst().getName()).isEqualTo("성수 카페");
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 500, 503})
    @DisplayName("카카오 4xx와 5xx 응답을 KAKAO_LOCAL_API_ERROR로 변환한다")
    void translatesHttpErrors(int status) throws Exception {
        startServer(exchange -> respond(exchange, status, "{\"message\":\"error\"}"));

        assertThatThrownBy(() -> client(1_000).searchByKeyword(
                "카페", 1, 15, "accuracy", null, null, null))
                .isInstanceOfSatisfying(PlaceException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(PlaceErrorCode.KAKAO_LOCAL_API_ERROR));
    }

    @Test
    @DisplayName("카카오 응답 시간 초과를 KAKAO_LOCAL_API_ERROR로 변환한다")
    void translatesReadTimeout() throws Exception {
        startServer(exchange -> {
            try {
                Thread.sleep(250);
                respond(exchange, 200, "{\"meta\":{},\"documents\":[]}");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });

        assertThatThrownBy(() -> client(30).searchByKeyword(
                "카페", 1, 15, "accuracy", null, null, null))
                .isInstanceOfSatisfying(PlaceException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(PlaceErrorCode.KAKAO_LOCAL_API_ERROR));
    }

    private KakaoClient client(int readTimeoutMillis) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(1_000);
        requestFactory.setReadTimeout(readTimeoutMillis);
        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .requestFactory(requestFactory)
                .build();
        return new KakaoClient(restClient);
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v2/local/search/keyword.json", exchange -> handler.handle(exchange));
        server.start();
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
