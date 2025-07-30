package com.threestar.trainus.domain.file.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.file.dto.GetS3UrlDto;
import com.threestar.trainus.domain.file.service.S3Service;
import com.threestar.trainus.global.annotation.LoginUser;

@RestController
@RequiredArgsConstructor
@RequestMapping("/s3")
public class S3Controller {
	private final S3Service s3Service;

	@GetMapping(value = "/posturl")
	public ResponseEntity<GetS3UrlDto> getPostS3Url(@LoginUser Long userId, String filename) {
		GetS3UrlDto getS3UrlDto = s3Service.getPostS3Url("image/"+userId, filename);
		return new ResponseEntity<>(getS3UrlDto, HttpStatusCode.valueOf(200));
	}

	@GetMapping(value = "/geturl")
	public ResponseEntity<GetS3UrlDto> getGetS3Url(@LoginUser Long userId, @RequestParam String key) {
		GetS3UrlDto getS3UrlDto = s3Service.getGetS3Url(userId, key);
		return new ResponseEntity<>(getS3UrlDto, HttpStatusCode.valueOf(200));
	}
}
