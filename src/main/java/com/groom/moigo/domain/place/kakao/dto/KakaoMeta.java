package com.groom.moigo.domain.place.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

//검색 결과의 상태와 페이지 정보 받음
@Getter
public class KakaoMeta {

    @JsonProperty("total_count")
    private int totalCount; //검색 조건에 맞는 전체 결과 개수
    @JsonProperty("pageable_count")
    private int pageableCount; //api를 통해 실제 페이지 탐색이 가능한 결과 개수
    @JsonProperty("is_end")
    private boolean isEnd; //현재 응답이 마지막 페이지인지 알려줌
}
