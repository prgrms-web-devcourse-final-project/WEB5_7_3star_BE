package com.threestar.trainus.domain.file.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.file.dto.GetS3UrlDto;
import com.threestar.trainus.domain.file.service.S3Service;
import com.threestar.trainus.global.annotation.LoginUser;
import com.threestar.trainus.global.unit.BaseResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/s3")
public class S3Controller {
	private final S3Service s3Service;

	@GetMapping(value = "/posturl")
	public ResponseEntity<BaseResponse<GetS3UrlDto>> getPostS3Url(@LoginUser Long userId, String filename) {
		GetS3UrlDto response = s3Service.getPostS3Url("image/"+userId, filename);
		return BaseResponse.ok("업로드용 presigned url 발급 완료", response, HttpStatus.OK);
	}

	@GetMapping(value = "/geturl")
	public ResponseEntity<BaseResponse<GetS3UrlDto>> getGetS3Url(@LoginUser Long userId, @RequestParam String key) {
		GetS3UrlDto response = s3Service.getGetS3Url(key);
		return BaseResponse.ok("조회용 presigned url 발급 완료", response, HttpStatus.OK);
	}
}
