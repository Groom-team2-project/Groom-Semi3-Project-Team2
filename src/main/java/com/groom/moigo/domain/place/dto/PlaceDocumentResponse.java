package com.groom.moigo.domain.place.dto;

import com.groom.moigo.domain.place.kakao.dto.KakaoDocument;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class PlaceDocumentResponse {

    private final String kakaoPlaceId; //저장되지 않은 장소라서 카카오id를 사용
    private final String name;
    private final String category;
    private final String categoryGroupCode;
    private final String categoryGroupName;
    private final String address;
    private final String roadAddress;
    private final BigDecimal longitude;
    private final BigDecimal latitude;
    private final String phone;
    private final String placeUrl;
    private final String selectionToken;

    public static PlaceDocumentResponse from(KakaoDocument document, String selectionToken) {
        return new PlaceDocumentResponse(
                document.getId(),
                document.getPlaceName(),
                document.getCategoryName(),
                document.getCategoryGroupCode(),
                document.getCategoryGroupName(),
                document.getAddressName(),
                document.getRoadAddressName(),
                document.getX(),
                document.getY(),
                document.getPhone(),
                document.getPlaceUrl(),
                selectionToken
        );
    }
}
