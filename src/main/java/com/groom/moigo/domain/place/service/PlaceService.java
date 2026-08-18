package com.groom.moigo.domain.place.service;

import com.groom.moigo.domain.place.client.KakaoClient;
import com.groom.moigo.domain.place.dto.*;
import com.groom.moigo.domain.place.entity.PlaceEntity;
import com.groom.moigo.domain.place.repository.PlaceRepository;
import com.groom.moigo.domain.place.exception.PlaceErrorCode;
import com.groom.moigo.domain.place.exception.PlaceException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
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
    private final PlaceSynchronizationService synchronizationService;

    public PlaceResponse getPlace(Long placeId) {
        PlaceEntity place = placeRepository
                .findByPlaceIdAndDeletedAtIsNull(placeId)
                .orElseThrow(() -> new PlaceException(PlaceErrorCode.PLACE_NOT_FOUND));
        return PlaceResponse.from(place);
    }
    public PlaceListResponse getPlaces(int page, int size, String keyword, String sort) {
        validateListRequest(page, size);
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Page<PlaceEntity> result = placeRepository.searchActive(normalizedKeyword, pageable);
        return PlaceListResponse.from(result);
    }

    @Transactional
    public void softDeletePlace(Long placeId) {
        PlaceEntity place = placeRepository.findById(placeId)
                .orElseThrow(() -> new PlaceException(PlaceErrorCode.PLACE_NOT_FOUND));
        place.softDelete();
    }

    @Transactional
    public void restorePlace(Long placeId) {
        PlaceEntity place = placeRepository.findById(placeId)
                .orElseThrow(() -> new PlaceException(PlaceErrorCode.PLACE_NOT_FOUND));
        place.restore();
    }

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
                .filter(PlaceService::isValidDocument)
                .collect(Collectors.toMap(
                        document -> document.getId().trim(),
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        List<PlaceEntity> synchronizedPlaces = synchronizeWithConflictRetry(documentsById.values());

        List<PlaceSearchResponse> places = synchronizedPlaces.stream()
                .map(PlaceSearchResponse::from)
                .toList();
        return PlacePageResponse.of(places, page, size, result.getMeta());
    }

    private List<PlaceEntity> synchronizeWithConflictRetry(Collection<KakaoDocument> documents) {
        try {
            return synchronizationService.synchronize(documents);
        } catch (DataIntegrityViolationException conflict) {
            try {
                return synchronizationService.synchronize(documents);
            } catch (DataIntegrityViolationException retryConflict) {
                throw new PlaceException(PlaceErrorCode.KAKAO_PLACE_ID_CONFLICT, retryConflict);
            } catch (DataAccessException retryFailure) {
                throw new PlaceException(PlaceErrorCode.PLACE_SYNC_FAILED, retryFailure);
            }
        } catch (DataAccessException failure) {
            throw new PlaceException(PlaceErrorCode.PLACE_SYNC_FAILED, failure);
        }
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

    private static boolean isValidDocument(KakaoDocument document) {
        return document != null
                && document.getId() != null && !document.getId().isBlank()
                && document.getName() != null && !document.getName().isBlank()
                && document.getX() != null && document.getY() != null
                && document.getX().compareTo(BigDecimal.valueOf(-180)) >= 0
                && document.getX().compareTo(BigDecimal.valueOf(180)) <= 0
                && document.getY().compareTo(BigDecimal.valueOf(-90)) >= 0
                && document.getY().compareTo(BigDecimal.valueOf(90)) <= 0;
    }

    private static void validateListRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new PlaceException(
                    PlaceErrorCode.INVALID_PLACE_PAGE_SIZE,
                    "목록 페이지는 0 이상, 크기는 1~100이어야 합니다."
            );
        }
    }

    private static Sort parseSort(String sort) {
        String[] parts = sort == null ? new String[0] : sort.split(",", -1);
        if (parts.length != 2) {
            throw new PlaceException(PlaceErrorCode.INVALID_PLACE_SORT);
        }
        String property = switch (parts[0]) {
            case "createdAt" -> "createdAt";
            case "updatedAt" -> "updatedAt";
            case "name" -> "name";
            default -> throw new PlaceException(PlaceErrorCode.INVALID_PLACE_SORT);
        };
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(parts[1]);
        } catch (IllegalArgumentException exception) {
            throw new PlaceException(PlaceErrorCode.INVALID_PLACE_SORT);
        }
        return Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "placeId"));
    }
}
