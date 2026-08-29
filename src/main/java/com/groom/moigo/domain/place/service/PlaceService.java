package com.groom.moigo.domain.place.service;

import com.groom.moigo.domain.place.dto.*;
import com.groom.moigo.domain.place.entity.PlaceCategory;
import com.groom.moigo.domain.place.entity.PlaceEntity;
import com.groom.moigo.domain.place.kakao.client.KakaoClient;
import com.groom.moigo.domain.place.kakao.dto.KakaoSearchResponse;
import com.groom.moigo.domain.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {
    private final KakaoClient kakaoClient;
    private final PlaceRepository placeRepository;

    public PlaceDocumentListResponse searchPlaces(
            String keyword,
            int page,
            int size
    ){
        KakaoSearchResponse kakaoResponse = kakaoClient.searchByKeyword(
                keyword,
                page,
                size,
                "accuracy",
                null,
                null,
                null);
        List<PlaceDocumentResponse> places = kakaoResponse.getDocuments().stream()
                .map(PlaceDocumentResponse::from)
                .toList();
        return PlaceDocumentListResponse.of(places, kakaoResponse.getMeta());
    }

    public CategoryListResponse getCategories() {
          List<CategoryResponse> categories = Arrays.stream(PlaceCategory.values())
                  .map(CategoryResponse::from)
                  .toList();
        return CategoryListResponse.of(categories);
    }

    public PlaceDocumentListResponse getCategoryPlaces(
            String categoryGroupCode,
            BigDecimal southWestLongitude,
            BigDecimal southWestLatitude,
            BigDecimal northEastLongitude,
            BigDecimal northEastLatitude,
            int page,
            int size
    ) {
        String rect = String.join(
                ",",
                southWestLongitude.toPlainString(),
                southWestLatitude.toPlainString(),
                northEastLongitude.toPlainString(),
                northEastLatitude.toPlainString()
        );

        KakaoSearchResponse kakaoResponse = kakaoClient.searchByCategory(
                categoryGroupCode,
                rect,
                page,
                size);

        List<PlaceDocumentResponse> places = kakaoResponse.getDocuments().stream()
                .map(PlaceDocumentResponse::from)
                .toList();

        return PlaceDocumentListResponse.of(places, kakaoResponse.getMeta());
    }

    /*
    TODO:
        1.  Place를 Schedule에서 등록하기 위해서, createPlace 메서드를 호출할 경우,
            Place DB 내에 기존에 등록했던 이력이 있는지 검사 후,
            존재한다면 -> 해당 데이터와 비교 후 최신화(방법에 대해서는 2에서 서술)
            없다면 -> 해당 데이터 추가
        2.  1에서 등록했던 이력 혹은 데이터가 정확한지 알아내기 위하여 데이터에 대한 해싱을 고려하고 있는데,
            이게 효과적인지 알기 위해서 성능 비교가 필요할 것으로 보임.
            우선 v1의 구현으로 모든 데이터를 각각 비교하여 구현하고,
            성능 개선 작업에서 v2를 구현하여 해싱 적용을 고려해볼 것.
        3.  2에서 v1 구현을 위해서,
            첫번째로 KakaoPlaceId 가 일치하는 행이 있는지 검사(findKakaoPlaceId 로 행을 찾음),
            두번째로 좌표 일치 여부 검사,
            세번째로 주소 일치 여부 검사.
        ->  성능 오버헤드에는 큰 영향을 주지 않을 수도 있을 것으로 판단되어서, v2 계획은 잠정 폐기하겠습니다.
            (행으로 가져온 데이터를 Backend 내에서 확인하는게 해싱에 필요한 리소스 보다 적을 것으로 보입니다)
     */
    @Transactional
    public PlaceRegisterResponse registerPlace(PlaceRegisterRequest request) {
        Optional<PlaceEntity> optionalPlace = placeRepository.findByKakaoPlaceId(request.getKakaoPlaceId());

        PlaceEntity place;

        if (optionalPlace.isPresent()) {
            // DB 내 존재하는 Place 처리
            place = optionalPlace.get();

            // Place 정보 변경 여부 검사
            if (hasPlaceChanged(place, request)) {
                PlaceEntity updatedPlace = updatePlace(place, request);

                return PlaceRegisterResponse.from(updatedPlace);
            }
            // 변경점 없다고 판단시 그대로 반환
        } else {
            // DB 내 존재하지 않는 Place일 경우 생성
            place = createPlace(request);
        }

        return PlaceRegisterResponse.from(place);
    }

    private PlaceEntity updatePlace(
            PlaceEntity place,
            PlaceRegisterRequest request
    ) {
        place.update(
                request.getName(),
                request.getCategory(),
                request.getAddress(),
                request.getRoadAddress(),
                request.getPhone(),
                request.getPlaceUrl(),
                request.getLatitude(),
                request.getLongitude()
        );
        return place;
    }

    private PlaceEntity createPlace(PlaceRegisterRequest request) {
        PlaceEntity place = PlaceEntity.create(
                request.getKakaoPlaceId(),
                request.getName(),
                request.getCategory(),
                request.getAddress(),
                request.getRoadAddress(),
                request.getPhone(),
                request.getPlaceUrl(),
                request.getLatitude(),
                request.getLongitude()
        );

        return placeRepository.save(place);
    }

    private boolean hasPlaceChanged(PlaceEntity place, PlaceRegisterRequest request) {
        return hasCoordinateChanged(place.getLatitude(), request.getLatitude())
                || hasCoordinateChanged(place.getLongitude(), request.getLongitude())
                || !Objects.equals(place.getRoadAddress(), request.getRoadAddress())
                || !Objects.equals(place.getAddress(), request.getAddress())
                || !Objects.equals(place.getPhone(), request.getPhone())
                || !Objects.equals(place.getPlaceUrl(), request.getPlaceUrl())
                || !Objects.equals(place.getCategory(), request.getCategory())
                || !Objects.equals(place.getName(), request.getName());
    }

    private boolean hasCoordinateChanged(BigDecimal current, BigDecimal incoming) {
        if (current == null || incoming == null) {
            return current != incoming;
        }
        return current.compareTo(incoming) != 0;
    }

}
