package com.threestar.trainus.domain.ranking.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.ranking.dto.RankingResponseDto;
import com.threestar.trainus.domain.ranking.service.RankingService;
import com.threestar.trainus.global.unit.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "랭킹 조회 API", description = "랭킹 조회(카테고리 상관없이 전체 1~10등) API")
@RestController
@RequestMapping("/api/v1/rankings")
@RequiredArgsConstructor
public class RankingController {

	private final RankingService rankingService;

	@GetMapping
	@Operation(summary = "전체 랭킹 조회 api", description = "카테고리와 관계없이 전체 랭킹 Top10을 조회")
	public ResponseEntity<BaseResponse<List<RankingResponseDto>>> getRankings() {
		List<RankingResponseDto> rankings = rankingService.getTopRankings();
		return BaseResponse.ok("전체 랭킹 조회 성공", rankings, HttpStatus.OK);
	}
}
