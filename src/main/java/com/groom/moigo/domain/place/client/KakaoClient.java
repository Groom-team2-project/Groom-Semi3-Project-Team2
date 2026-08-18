package com.groom.moigo.domain.place.client;

import com.groom.moigo.domain.place.dto.KakaoSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

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

    public KakaoSearchResponse searchByKeyword(
            String keyword,
            int page,
            int size,
            String sort,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer radius
    ) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/keyword.json")
                        .queryParam("query", keyword)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .queryParam("sort", sort)
                        .queryParamIfPresent("x", java.util.Optional.ofNullable(longitude))
                        .queryParamIfPresent("y", java.util.Optional.ofNullable(latitude))
                        .queryParamIfPresent("radius", java.util.Optional.ofNullable(radius))
                        .build())
                .retrieve()
                .body(KakaoSearchResponse.class);
    }
}
