package com.threestar.trainus.domain.lesson.student.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.threestar.trainus.domain.lesson.student.dto.LessonSearchListResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonSearchResponseDto;
import com.threestar.trainus.domain.lesson.admin.entity.Category;
import com.threestar.trainus.domain.lesson.student.dto.PaginationDto;

@Service
public class StudentLessonService {
	public LessonSearchListResponseDto getLessons(
		int page, int limit, String category, String search,
		String city, String district, String dong
	) {
		// 목데이터 반환중 TODO 구현 필요
		LessonSearchResponseDto lesson = new LessonSearchResponseDto(
			1L,
			"이동국의 축구교실",
			"이동국",
			"https://example.com/leader-image.jpg",
			2,
			24,
			4.5f,
			Category.FOOTBALL,
			40000,
			10,
			8,
			"모집중",
			LocalDateTime.parse("2025-07-04T10:00:00"),
			LocalDateTime.parse("2025-07-04T10:00:00"),
			LocalDateTime.parse("2025-07-04T10:00:00"),
			true,
			"경기도",
			"고양시 덕양구",
			"고양동",
			LocalDateTime.parse("2025-07-04T10:00:00"),
			List.of("https://example.com/image1.jpg", "https://example.com/image2.jpg")
		);

		List<LessonSearchResponseDto> lessonList = List.of(lesson);

		PaginationDto pagination = new PaginationDto(
			2,  // currentPage
			5,  // totalPages
			24, // totalCount
			5   // limit
		);

		return new LessonSearchListResponseDto(lessonList, pagination);
	}
}
