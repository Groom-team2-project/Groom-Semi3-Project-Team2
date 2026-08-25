package com.groom.moigo.domain.place2.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;

//카카오api에서 받은 장소 한 곳의 정보 받음
@Getter
public class KakaoDocument {

    private String id;
    @JsonProperty("place_name")
    private String placeName;
    @JsonProperty("category_name")
    private String categoryName;
    @JsonProperty("category_group_code")
    private String categoryGroupCode;
    @JsonProperty("category_group_name")
    private String categoryGroupName;
    @JsonProperty("road_address_name")
    private String roadAddressName;
    @JsonProperty("address_name")
    private String addressName;
    private BigDecimal x; // longitude
    private BigDecimal y; // latitude
    private String phone;
    @JsonProperty("place_url")
    private String placeUrl;
}
