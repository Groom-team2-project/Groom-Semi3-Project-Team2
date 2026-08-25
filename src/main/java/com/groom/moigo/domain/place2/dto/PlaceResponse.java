package com.groom.moigo.domain.place2.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class PlaceResponse {

    private Long placeId;
    private String kakaoPlaceId;
    private String name;
    private String category;
    private String address;
    private String roadAddress;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String phone;
    private String placeUrl;
}
