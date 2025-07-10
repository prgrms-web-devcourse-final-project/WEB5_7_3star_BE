package com.threestar.trainus.domain.lesson.student.controller;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;

@Tag(name = "수강생 레슨 API", description = "수강생 조회/등록 관련 API")
@RestController
@RequestMapping("/api/lessons")
public class StudentLessonController {

	@Operation(summary = "Swagger 체크 api입니다!", description = "API 설명란 입니다!")
	@GetMapping("/swagger-test")
	public String getLessons(
	) {
		return "Swagger Test Check!";
	}
	@GetMapping("/test-auth")
	public String testAuth(HttpSession session, Principal principal) {
		return "SessionID: " + session.getId() + ", user: " + principal;
	}
}