package com.groom.moigo.domain.vote.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 투표 마감 스케줄러를 돌리기 위한 설정.
 *
 * <p>NOTE {@code @EnableScheduling}은 애플리케이션 전체에 적용된다. 지금은 투표 마감만 스케줄을 쓰기에 투표 도메인에 두었다. 다른 도메인도
 * 스케줄이 필요해지면 공통 설정으로 옮기는 편이 낫다.
 */
@Configuration
@EnableScheduling
public class VoteSchedulingConfig {}
