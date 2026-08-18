package com.groom.moigo.domain.place.client;

import com.groom.moigo.domain.place.dto.KakaoSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KakaoClient {

    private final RestClient restClient;

    public KakaoClient(
            RestClient.Builder builder,
            @Value("${kakao.local.rest-api-key}") String apiKey
    ) {
        this.restClient = builder
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader("Authorization", "KakaoAK " + apiKey)
                .build();
    }

    public KakaoSearchResponse searchByKeyword(String keyword) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/keyword.json")
                        .queryParam("query", keyword)
                        .build())
                .retrieve()
                .body(KakaoSearchResponse.class);
    }
}
