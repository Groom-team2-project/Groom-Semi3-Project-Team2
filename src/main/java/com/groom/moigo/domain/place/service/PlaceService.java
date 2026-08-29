package com.groom.moigo.domain.place.service;

import com.groom.moigo.domain.place.dto.*;
import com.groom.moigo.domain.place.entity.PlaceCategory;
import com.groom.moigo.domain.place.entity.PlaceEntity;
import com.groom.moigo.domain.place.kakao.client.KakaoClient;
import com.groom.moigo.domain.place.kakao.dto.KakaoSearchResponse;
import com.groom.moigo.domain.place.repository.PlaceRepository;
import com.groom.moigo.domain.place.token.PlaceSelectionClaims;
import com.groom.moigo.domain.place.token.PlaceSelectionTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {
    private final KakaoClient kakaoClient;
    private final PlaceRepository placeRepository;
    private final PlaceSelectionTokenProvider placeSelectionTokenProvider;
    private final PlacePersistenceService placePersistenceService;

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
                .map(document -> PlaceDocumentResponse.from(
                        document,
                        placeSelectionTokenProvider.create(document)
                ))
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
                .map(document -> PlaceDocumentResponse.from(
                        document,
                        placeSelectionTokenProvider.create(document)
                ))
                .toList();

        return PlaceDocumentListResponse.of(places, kakaoResponse.getMeta());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PlaceRegisterResponse registerPlace(PlaceRegisterRequest request) {
        PlaceSelectionClaims claims = placeSelectionTokenProvider.verify(request.getSelectionToken());
        Optional<PlaceEntity> existingPlace = placeRepository.findByKakaoPlaceId(claims.kakaoPlaceId());

        if (existingPlace.isPresent()) {
            PlaceEntity updated = placePersistenceService.update(
                    existingPlace.get().getPlaceId(),
                    claims
            );
            return PlaceRegisterResponse.from(updated);
        }

        return createOrGetPlace(claims);
    }

    private PlaceRegisterResponse createOrGetPlace(PlaceSelectionClaims claims) {
        try {
            return PlaceRegisterResponse.from(placePersistenceService.create(claims));
        } catch (DataIntegrityViolationException exception) {
            PlaceEntity existing = placePersistenceService.findByKakaoPlaceId(claims.kakaoPlaceId());
            return PlaceRegisterResponse.from(existing);
        }
    }
}
