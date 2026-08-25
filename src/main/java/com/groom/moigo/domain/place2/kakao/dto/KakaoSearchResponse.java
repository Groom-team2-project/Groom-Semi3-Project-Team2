package com.groom.moigo.domain.place2.kakao.dto;

import lombok.Getter;

import java.util.List;

//카카오api에서 받는 JSON전체
@Getter
public class KakaoSearchResponse {

    private KakaoMeta meta;

    private List<KakaoDocument> documents;
}
