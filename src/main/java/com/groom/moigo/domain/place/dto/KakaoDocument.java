package com.groom.moigo.domain.place.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoDocument {

    private String id;

    @JsonProperty("place_name")
    private String name;

    @JsonProperty("category_name")
    private String category;

    @JsonProperty("road_address_name")
    private String roadAddress;

    @JsonProperty("address_name")
    private String address;

    private BigDecimal x;
    private BigDecimal y;

    private String phone;

    @JsonProperty("place_url")
    private String placeUrl;
}
