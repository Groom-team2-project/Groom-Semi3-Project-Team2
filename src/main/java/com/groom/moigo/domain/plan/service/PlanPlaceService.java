package com.groom.moigo.domain.plan.service;

import com.groom.moigo.domain.place.dto.PlaceRegisterRequest;
import com.groom.moigo.domain.place.dto.PlaceRegisterResponse;
import com.groom.moigo.domain.place.dto.PlaceResponse;
import com.groom.moigo.domain.place.entity.PlaceEntity;
import com.groom.moigo.domain.place.repository.PlaceRepository;
import com.groom.moigo.domain.place.service.PlacePersistenceService;
import com.groom.moigo.domain.place.service.PlaceService;
import com.groom.moigo.domain.plan.entity.MemberEntity;
import com.groom.moigo.domain.plan.entity.PlanPlaceEntity;
import com.groom.moigo.domain.plan.repository.PlanPlaceRepository;
import com.groom.moigo.domain.plan.repository.PlanRepository;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanPlaceService {

    private final PlanRepository planRepository;
    private final PlanPlaceRepository planPlaceRepository;
    private final PlaceRepository placeRepository;
    private final PlaceService placeService;
    private final PlacePersistenceService placePersistenceService;
    private final PlanAccessService planAccessService;

    @Transactional(readOnly = true)
    public List<PlaceResponse> getPlaces(Long userId, Long planId) {
        requirePlanExists(planId);
        planAccessService.requireJoinedMember(planId, userId);

        List<Long> placeIds = planPlaceRepository.findAllByPlan_PlanIdOrderByCreatedAtDesc(planId).stream()
                .map(PlanPlaceEntity::getPlaceId)
                .toList();
        if (placeIds.isEmpty()) {
            return List.of();
        }

        Map<Long, PlaceEntity> placesById = placeRepository.findAllByPlaceIdInAndDeletedAtIsNull(placeIds).stream()
                .collect(Collectors.toMap(PlaceEntity::getPlaceId, Function.identity()));

        return placeIds.stream()
                .map(placesById::get)
                .filter(Objects::nonNull)
                .map(PlaceResponse::from)
                .toList();
    }

    @Transactional
    public PlaceResponse savePlace(Long userId, Long planId, PlaceRegisterRequest request) {
        requirePlanExists(planId);
        MemberEntity member = planAccessService.requireJoinedMember(planId, userId);
        planAccessService.requireEditable(member);

        PlaceRegisterResponse registered = placeService.registerPlace(request);
        Long placeId = registered.getPlaceId();
        PlaceEntity place = placePersistenceService.findByPlaceId(placeId);
        planPlaceRepository.upsert(planId, placeId);

        return PlaceResponse.from(place);
    }

    @Transactional
    public void removePlace(Long userId, Long planId, Long placeId) {
        requirePlanExists(planId);
        MemberEntity member = planAccessService.requireJoinedMember(planId, userId);
        planAccessService.requireEditable(member);

        PlanPlaceEntity planPlace = planPlaceRepository.findByPlan_PlanIdAndPlaceId(planId, placeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
        planPlaceRepository.delete(planPlace);
    }

    private void requirePlanExists(Long planId) {
        if (!planRepository.existsByPlanIdAndDeletedAtIsNull(planId)) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND);
        }
    }
}
