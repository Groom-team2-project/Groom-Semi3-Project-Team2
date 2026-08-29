package com.groom.moigo.domain.place.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@NoArgsConstructor
@Getter
public class PlaceRegisterRequest {
    /*
    * Place DB 내에 장소가 존재하는지 확인하고, 존재하지 않는다면 생성하기 위한 정보를 받는 DTO입니다.
    * */
    @NotBlank
    @Size(max = 50)
    private String kakaoPlaceId;

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 255)
    private String category;

    @Size(max = 300)
    private String address;

    @Size(max = 300)
    private String roadAddress;

    @Size(max = 30)
    private String phone;

    @Size(max = 500)
    private String placeUrl;

    @NotNull
    private BigDecimal latitude;

    @NotNull
    private BigDecimal longitude;
}
