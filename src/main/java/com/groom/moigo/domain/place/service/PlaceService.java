package com.groom.moigo.domain.place.service;

import com.groom.moigo.domain.place.client.KakaoClient;
import com.groom.moigo.domain.place.dto.*;
import com.groom.moigo.domain.place.entity.PlaceEntity;
import com.groom.moigo.domain.place.repository.PlaceRepository;
import com.groom.moigo.domain.place.exception.PlaceErrorCode;
import com.groom.moigo.domain.place.exception.PlaceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class PlaceService {
    private final PlaceRepository placeRepository;
    private final KakaoClient kakaoClient;

    public PlaceResponse getPlace(Long placeId) {
        PlaceEntity place = placeRepository
                .findByPlaceIdAndDeletedAtIsNull(placeId)
                .orElseThrow(() -> new PlaceException(PlaceErrorCode.PLACE_NOT_FOUND));
        return PlaceResponse.from(place);
    }
    public PlaceListResponse getPlaces(int page, int size) {
        Pageable pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PlaceEntity> result = placeRepository.findAllByDeletedAtIsNull(pageable);
        return PlaceListResponse.from(result);
    }

    @Transactional
    public PlacePageResponse searchPlaces(
            String query,
            int page,
            int size,
            String sort,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer radius
    ){
        validateSearchRequest(query, page, size, sort, latitude, longitude, radius);
        KakaoSearchResponse result = kakaoClient.searchByKeyword(
                query.trim(), page, size, sort, latitude, longitude, radius
        );
        if (result.getMeta() == null || result.getDocuments() == null) {
            throw new PlaceException(PlaceErrorCode.INVALID_KAKAO_PLACE_DATA);
        }

        Map<String, KakaoDocument> documentsById = result.getDocuments().stream()
                .peek(PlaceService::validateDocumentIdentity)
                .collect(Collectors.toMap(
                        document -> document.getId().trim(),
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        Map<String, PlaceEntity> existingByKakaoId = documentsById.isEmpty()
                ? Map.of()
                : placeRepository.findAllByKakaoPlaceIdIn(documentsById.keySet()).stream()
                        .collect(Collectors.toMap(PlaceEntity::getKakaoPlaceId, Function.identity()));

        List<PlaceEntity> synchronizedPlaces = documentsById.values().stream()
                .map(document -> synchronize(document, existingByKakaoId))
                .toList();
        placeRepository.saveAll(synchronizedPlaces);

        List<PlaceSearchResponse> places = synchronizedPlaces.stream()
                .map(PlaceSearchResponse::from)
                .toList();
        return PlacePageResponse.of(places, page, size, result.getMeta());
    }

    private PlaceEntity synchronize(KakaoDocument document, Map<String, PlaceEntity> existingByKakaoId) {
        String kakaoPlaceId = document.getId().trim();
        PlaceEntity place = existingByKakaoId.get(kakaoPlaceId);
        if (place == null) {
            return PlaceEntity.create(document);
        }
        place.updateKakaoInfo(document);
        place.restore();
        return place;
    }

    private static void validateSearchRequest(String query, int page, int size, String sort,
                                              BigDecimal latitude, BigDecimal longitude, Integer radius) {
        if (query == null || query.isBlank()) {
            throw new PlaceException(PlaceErrorCode.PLACE_QUERY_REQUIRED);
        }
        if (page < 1 || page > 45 || size < 1 || size > 15) {
            throw new PlaceException(PlaceErrorCode.INVALID_PLACE_PAGE_SIZE);
        }
        boolean hasLatitude = latitude != null;
        boolean hasLongitude = longitude != null;
        boolean invalidCoordinates = hasLatitude != hasLongitude
                || (hasLatitude && (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0));
        boolean invalidSort = !"accuracy".equals(sort) && !"distance".equals(sort);
        boolean invalidRadius = radius != null
                && (radius < 0 || radius > 20_000 || !hasLatitude);
        if (invalidCoordinates || invalidSort || invalidRadius
                || ("distance".equals(sort) && !hasLatitude)) {
            throw new PlaceException(PlaceErrorCode.INVALID_LOCATION);
        }
    }

    private static void validateDocumentIdentity(KakaoDocument document) {
        if (document == null || document.getId() == null || document.getId().isBlank()) {
            throw new PlaceException(PlaceErrorCode.INVALID_KAKAO_PLACE_DATA);
        }
    }
}
