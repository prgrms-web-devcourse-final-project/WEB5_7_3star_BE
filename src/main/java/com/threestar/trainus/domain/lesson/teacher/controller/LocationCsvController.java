package com.threestar.trainus.domain.lesson.teacher.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.threestar.trainus.domain.lesson.teacher.service.LocationCsvService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/location")
public class LocationCsvController {

	private final LocationCsvService locationCsvService;

	// 업로드 api
	@PostMapping("/upload-location")
	@Operation(summary = "법정동명 csv파일 업로드 api", description = "현재 쓰이는 열만 필터링하여 업로드")
	public ResponseEntity<String> uploadCsv(@RequestParam("file") MultipartFile file) {
		locationCsvService.processCsv(file);
		return ResponseEntity.ok("법정동 업로드 완료");
	}

	// 검증용 api
	@GetMapping("/exists")
	@Operation(summary = "법정동명 검증 api", description = "")
	public ResponseEntity<Boolean> checkExists(
		@RequestParam String city,
		@RequestParam String district,
		@RequestParam String dong,
		@RequestParam(required = false) String ri
	) {
		boolean exists = locationCsvService.checkLocation(city, district, dong, ri);
		return ResponseEntity.ok(exists);
	}
}