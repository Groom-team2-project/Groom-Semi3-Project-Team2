package com.groom.moigo.domain.place2.controller;

import com.groom.moigo.domain.place2.service.PlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PlaceController {
    private final PlaceService placeService;


}
