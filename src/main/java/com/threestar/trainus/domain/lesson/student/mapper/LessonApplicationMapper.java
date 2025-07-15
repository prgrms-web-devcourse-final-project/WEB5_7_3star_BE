package com.threestar.trainus.domain.lesson.student.mapper;

import java.util.List;
import java.util.stream.Collectors;

import com.threestar.trainus.domain.lesson.admin.entity.LessonApplication;
import com.threestar.trainus.domain.lesson.student.dto.LessonSummaryResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.MyLessonApplicationListResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.MyLessonApplicationResponseDto;

public class LessonApplicationMapper {

	public static MyLessonApplicationResponseDto toMyLessonApplicationDto(LessonApplication app) {
		return MyLessonApplicationResponseDto.builder()
			.lessonApplicationId(app.getId())
			// 레슨 정보
			.lesson(
				LessonSummaryResponseDto.builder()
					.id(app.getLesson().getId())
					.lessonName(app.getLesson().getLessonName())
					.lessonLeader(app.getLesson().getLessonLeader())
					.startAt(app.getLesson().getStartAt())
					.price(app.getLesson().getPrice())
					.addressDetail(app.getLesson().getAddressDetail())
					.build()
			)
			// 신청 상태
			.status(app.getStatus())
			.appliedAt(app.getCreatedAt())
			.build();
	}

	public static List<MyLessonApplicationResponseDto> toDtoList(List<LessonApplication> applications) {
		return applications.stream()
			.map(LessonApplicationMapper::toMyLessonApplicationDto)
			.collect(Collectors.toList());
	}

	public static MyLessonApplicationListResponseDto toDtoListWithCount(List<LessonApplication> applications, int count) {
		return new MyLessonApplicationListResponseDto(
			toDtoList(applications),
			count
		);
	}
}