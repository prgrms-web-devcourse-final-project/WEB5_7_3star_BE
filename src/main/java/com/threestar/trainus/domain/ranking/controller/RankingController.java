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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/rankings")
@RequiredArgsConstructor
public class RankingController {

	private final RankingService rankingService;

	@GetMapping
	public ResponseEntity<BaseResponse<List<RankingResponseDto>>> getRankings() {
		List<RankingResponseDto> rankings = rankingService.getTopRankings();
		return BaseResponse.ok("전체 랭킹 조회 성공", rankings, HttpStatus.OK);
	}
}
