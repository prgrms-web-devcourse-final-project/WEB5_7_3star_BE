package com.threestar.trainus.domain.ranking.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threestar.trainus.domain.ranking.dto.RankingData;
import com.threestar.trainus.domain.ranking.dto.RankingResponseDto;
import com.threestar.trainus.domain.ranking.repository.RankingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

	private final RankingRepository rankingRepository;
	private final RedisTemplate<String, String> redisTemplate;
	private final ObjectMapper objectMapper;

	private static final String RANKING_KEY = "ranking:all:top10";

	public List<RankingResponseDto> getTopRankings() {
		try {
			String cachedData = redisTemplate.opsForValue().get(RANKING_KEY);
			if (cachedData != null) {
				return objectMapper.readValue(cachedData, new TypeReference<List<RankingResponseDto>>() {
				});
			}
		} catch (Exception e) {
			log.warn("레디스 조회 실패: {}", e.getMessage());
		}

		List<RankingResponseDto> rankings = calculateRankings();
		saveToRedis(rankings);

		return rankings;
	}

	private List<RankingResponseDto> calculateRankings() {
		List<RankingData> data = rankingRepository.findTopRankings();

		List<RankingResponseDto> rankings = new ArrayList<>();

		for (int i = 0; i < data.size(); i++) {
			RankingData item = data.get(i);
			rankings.add(RankingResponseDto.builder()
				.userId(item.getUserId())
				.userNickname(item.getUserNickname())
				.category(null) //카테고리별 분류는 추후 도입
				.rating(item.getRating())
				.reviewCount(item.getReviewCount())
				.rank(i + 1)
				.profileImage(item.getProfileImage())
				.build());
		}

		return rankings;
	}

	private void saveToRedis(List<RankingResponseDto> rankings) {
		try {
			String json = objectMapper.writeValueAsString(rankings);
			redisTemplate.opsForValue().set(RANKING_KEY, json, Duration.ofHours(24));
			log.info("Redis에 랭킹 데이터 저장 완료");
		} catch (Exception e) {
			log.warn("Redis 저장 실패: {}", e.getMessage());
		}
	}

	//매 자정마다 랭킹 업데이트
	@Scheduled(cron = "0 0 0 * * *")
	public void updateRankings() {
		log.info("랭킹 업데이트 시작");
		try {
			List<RankingResponseDto> rankings = calculateRankings();
			saveToRedis(rankings);
			log.info("랭킹 업데이트 완료");
		} catch (Exception e) {
			log.error("랭킹 업데이트 실패: {}", e.getMessage(), e);
		}
	}
}
