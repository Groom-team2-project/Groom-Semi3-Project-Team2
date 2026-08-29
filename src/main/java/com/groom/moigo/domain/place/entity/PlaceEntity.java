package com.groom.moigo.domain.place.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "places")
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

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "place_url", length = 500)
    private String placeUrl;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static PlaceEntity create(
            String kakaoPlaceId,
            String name,
            String category,
            String address,
            String roadAddress,
            String phone,
            String placeUrl,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        PlaceEntity place = new PlaceEntity();
        place.kakaoPlaceId = kakaoPlaceId;
        place.name = name;
        place.category = category;
        place.address = address;
        place.roadAddress = roadAddress;
        place.phone = phone;
        place.placeUrl = placeUrl;
        place.latitude = latitude;
        place.longitude = longitude;
        return place;
    }

    public void update(
            String name,
            String category,
            String address,
            String roadAddress,
            String phone,
            String placeUrl,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        this.name = name;
        this.category = category;
        this.address = address;
        this.roadAddress = roadAddress;
        this.phone = phone;
        this.placeUrl = placeUrl;
        this.latitude = latitude;
        this.longitude = longitude;
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
