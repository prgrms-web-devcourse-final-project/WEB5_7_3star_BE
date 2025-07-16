package com.threestar.trainus.domain.ranking.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threestar.trainus.domain.lesson.admin.entity.Category;
import com.threestar.trainus.domain.ranking.dto.RankingData;
import com.threestar.trainus.domain.ranking.dto.RankingResponseDto;
import com.threestar.trainus.domain.ranking.repository.RankingRepository;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

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
	private static final String CATEGORY_RANKING_KEY_PREFIX = "ranking:";
	private static final String CATEGORY_RANKING_KEY_SUFFIX = ":top10";

	public List<RankingResponseDto> getTopRankings(String categoryStr) {

		Category category = null;
		String cacheKey = RANKING_KEY;

		if (!"ALL".equalsIgnoreCase(categoryStr)) {
			try {
				category = Category.valueOf(categoryStr.toUpperCase());
				cacheKey = CATEGORY_RANKING_KEY_PREFIX + categoryStr.toLowerCase() + CATEGORY_RANKING_KEY_SUFFIX;
			} catch (IllegalArgumentException e) {
				throw new BusinessException(ErrorCode.INVALID_CATEGORY);
			}
		}

		try {
			String cachedData = redisTemplate.opsForValue().get(cacheKey);
			if (cachedData != null) {
				return objectMapper.readValue(cachedData, new TypeReference<List<RankingResponseDto>>() {
				});
			}
		} catch (Exception e) {
			log.warn("레디스 조회 실패: {}", e.getMessage());
		}

		List<RankingResponseDto> rankings = calculateRankings(category);
		saveToRedis(rankings, cacheKey);

		return rankings;
	}

	private List<RankingResponseDto> calculateRankings(Category category) {
		List<RankingData> data = (category == null) ? rankingRepository.findTopRankings() :
			rankingRepository.findTopRankingsByCategory(category);

		List<RankingResponseDto> rankings = new ArrayList<>();

		for (int i = 0; i < data.size(); i++) {
			RankingData item = data.get(i);
			rankings.add(RankingResponseDto.builder()
				.userId(item.getUserId())
				.userNickname(item.getUserNickname())
				.category(category) //null이면 전체 조회
				.rating(item.getRating())
				.reviewCount(item.getReviewCount())
				.rank(i + 1)
				.profileImage(item.getProfileImage())
				.build());
		}

		return rankings;
	}

	private void saveToRedis(List<RankingResponseDto> rankings, String cacheKey) {
		try {
			String json = objectMapper.writeValueAsString(rankings);
			redisTemplate.opsForValue().set(cacheKey, json, Duration.ofHours(24));
		} catch (Exception e) {
			log.warn("Redis 저장 실패: {}", e.getMessage());
		}
	}

	//매 자정마다 랭킹 업데이트
	@Scheduled(cron = "0 0 0 * * *")
	public void updateRankings() {
		log.info("랭킹 업데이트 시작");

		Map<String, Integer> categoryCounts = new HashMap<>();

		try {
			//전체 랭킹 업데이트
			List<RankingResponseDto> allRankings = calculateRankings(null);
			saveToRedis(allRankings, RANKING_KEY);
			categoryCounts.put("ALL", allRankings.size());

			//카테고리별 랭킹 업데이트
			for (Category category : Category.values()) {
				List<RankingResponseDto> categoryRankings = calculateRankings(category);
				String cacheKey = CATEGORY_RANKING_KEY_PREFIX + category.name().toLowerCase() + CATEGORY_RANKING_KEY_SUFFIX;
				saveToRedis(categoryRankings, cacheKey);
				categoryCounts.put(category.name(), categoryRankings.size());
			}
		} catch (Exception e) {
			log.error("랭킹 업데이트 실패: {}", e.getMessage(), e);
		}
	}
}
