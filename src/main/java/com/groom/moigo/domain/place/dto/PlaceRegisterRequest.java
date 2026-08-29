package com.groom.moigo.domain.place.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@NoArgsConstructor
@Getter
public class PlaceRegisterRequest {
    @NotBlank
    @Size(max = 4096)
    private String selectionToken;
}
