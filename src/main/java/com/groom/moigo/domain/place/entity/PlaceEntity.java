package com.groom.moigo.domain.place.entity;

import com.groom.moigo.domain.place.dto.KakaoDocument;
import com.groom.moigo.domain.place.exception.PlaceErrorCode;
import com.groom.moigo.domain.place.exception.PlaceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "places",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_places_kakao_place_id",
                columnNames = "kakao_place_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    private Long placeId;

    @Column(name = "kakao_place_id", nullable = false, unique = true, length = 50)
    private String kakaoPlaceId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "category", length = 255)
    private String category;

    @Column(name = "address", length = 300)
    private String address;

    @Column(name = "road_address", length = 300)
    private String roadAddress;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "place_url", length = 500)
    private String placeUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static PlaceEntity create(KakaoDocument document) {
        validate(document);
        PlaceEntity place = new PlaceEntity();
        place.kakaoPlaceId = document.getId().trim();
        place.updateKakaoInfo(document);
        return place;
    }

    public void updateKakaoInfo(KakaoDocument document) {
        validate(document);
        if (!kakaoPlaceId.equals(document.getId().trim())) {
            throw new PlaceException(PlaceErrorCode.INVALID_KAKAO_PLACE_DATA);
        }
        this.name = document.getName().trim();
        this.category = normalize(document.getCategory());
        this.address = normalize(document.getAddress());
        this.roadAddress = normalize(document.getRoadAddress());
        this.longitude = document.getX();
        this.latitude = document.getY();
        this.phone = normalize(document.getPhone());
        this.placeUrl = normalize(document.getPlaceUrl());
    }

    public void softDelete() {
        if (deletedAt == null) {
            deletedAt = LocalDateTime.now();
        }
    }

    public void restore() {
        deletedAt = null;
    }

    private static void validate(KakaoDocument document) {
        if (document == null || isBlank(document.getId()) || isBlank(document.getName())
                || document.getX() == null || document.getY() == null
                || document.getX().compareTo(BigDecimal.valueOf(-180)) < 0
                || document.getX().compareTo(BigDecimal.valueOf(180)) > 0
                || document.getY().compareTo(BigDecimal.valueOf(-90)) < 0
                || document.getY().compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new PlaceException(PlaceErrorCode.INVALID_KAKAO_PLACE_DATA);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalize(String value) {
        return isBlank(value) ? null : value.trim();
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
