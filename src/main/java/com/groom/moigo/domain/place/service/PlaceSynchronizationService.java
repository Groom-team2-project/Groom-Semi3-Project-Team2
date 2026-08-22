package com.groom.moigo.domain.place.service;

import com.groom.moigo.domain.place.dto.KakaoDocument;
import com.groom.moigo.domain.place.entity.PlaceEntity;
import com.groom.moigo.domain.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceSynchronizationService {
    private final PlaceRepository placeRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<PlaceEntity> synchronize(Collection<KakaoDocument> documents) {
        List<String> kakaoPlaceIds = documents.stream()
                .map(document -> document.getId().trim())
                .toList();
        Map<String, PlaceEntity> existingByKakaoId = kakaoPlaceIds.isEmpty()
                ? Map.of()
                : placeRepository.findAllByKakaoPlaceIdIn(kakaoPlaceIds).stream()
                        .collect(Collectors.toMap(PlaceEntity::getKakaoPlaceId, Function.identity()));

        List<PlaceEntity> activePlaces = documents.stream()
                .map(document -> synchronizeOne(document, existingByKakaoId))
                .filter(place -> place.getDeletedAt() == null)
                .toList();
        return placeRepository.saveAllAndFlush(activePlaces);
    }

    private PlaceEntity synchronizeOne(KakaoDocument document,
                                       Map<String, PlaceEntity> existingByKakaoId) {
        PlaceEntity place = existingByKakaoId.get(document.getId().trim());
        if (place == null) {
            return PlaceEntity.create(document);
        }
        if (place.getDeletedAt() == null) {
            place.updateKakaoInfo(document);
        }
        return place;
    }
}
