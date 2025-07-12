package com.threestar.trainus.global.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.threestar.trainus.global.utils.S3Uploader;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/test/s3")
@RequiredArgsConstructor
public class S3TestController {

	private final S3Uploader s3Uploader;

	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(
		summary = "S3 파일 업로드",
		description = "profile-images 폴더에 파일 업로드 후 URL 반환"
	)
	public ResponseEntity<String> upload(
		@RequestParam("file") MultipartFile file
	) {
		try {
			String imageUrl = s3Uploader.uploadFile(file, "profile-images");
			return ResponseEntity.ok(imageUrl);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body("업로드 실패: " + e.getMessage());
		}
	}
}