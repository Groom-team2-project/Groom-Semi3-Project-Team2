package com.groom.moigo.domain.place2.kakao.client;

import com.groom.moigo.domain.place2.kakao.dto.KakaoSearchResponse;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

//카카오api 요청응답 변환
@Component
public class KakaoClient {
    private  final RestClient restClient;

    KakaoClient(RestClient restClient) {
        this.restClient = restClient;
    }
    @Autowired
    public KakaoClient(
            RestClient.Builder builder,
            @Value("${kakao.local.rest-api-key}") String apiKey
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3_000);
        requestFactory.setReadTimeout(5_000);
        this.restClient = builder
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader("Authorization", "KakaoAK " + apiKey)
                .requestFactory(requestFactory)
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
        try {
            KakaoSearchResponse response = restClient.get()
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
            if (response == null) {
                throw new BusinessException(ErrorCode.KAKAO_LOCAL_API_ERROR);
            }
            return response;
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.KAKAO_LOCAL_API_ERROR);
        }
    }

}