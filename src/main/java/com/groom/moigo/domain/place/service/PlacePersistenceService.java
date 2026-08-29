package com.groom.moigo.domain.place.service;

import com.groom.moigo.domain.place.entity.PlaceEntity;
import com.groom.moigo.domain.place.repository.PlaceRepository;
import com.groom.moigo.domain.place.token.PlaceSelectionClaims;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlacePersistenceService {

    private final PlaceRepository placeRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PlaceEntity create(PlaceSelectionClaims claims) {
        PlaceEntity place = PlaceEntity.create(
                claims.kakaoPlaceId(),
                claims.name(),
                claims.category(),
                claims.address(),
                claims.roadAddress(),
                claims.phone(),
                claims.placeUrl(),
                claims.latitude(),
                claims.longitude()
        );
        return placeRepository.saveAndFlush(place);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PlaceEntity update(Long placeId, PlaceSelectionClaims claims) {
        PlaceEntity place = placeRepository.findByPlaceIdAndDeletedAtIsNull(placeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
        place.update(
                claims.name(),
                claims.category(),
                claims.address(),
                claims.roadAddress(),
                claims.phone(),
                claims.placeUrl(),
                claims.latitude(),
                claims.longitude()
        );
        return place;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public PlaceEntity findByKakaoPlaceId(String kakaoPlaceId) {
        return placeRepository.findByKakaoPlaceId(kakaoPlaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
    }
}
