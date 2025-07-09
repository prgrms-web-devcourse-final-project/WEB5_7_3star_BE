package com.threestar.trainus.domain.lesson.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.lesson.admin.dto.LessonCreateRequestDto;
import com.threestar.trainus.domain.lesson.admin.dto.LessonResponseDto;
import com.threestar.trainus.domain.lesson.admin.entity.Lesson;
import com.threestar.trainus.domain.lesson.admin.entity.LessonImage;
import com.threestar.trainus.domain.lesson.admin.mapper.LessonMapper;
import com.threestar.trainus.domain.lesson.admin.repository.LessonImageRepository;
import com.threestar.trainus.domain.lesson.admin.repository.LessonRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminLessonService {

	private final LessonRepository lessonRepository;           // 레슨 DB 접근
	private final LessonImageRepository lessonImageRepository; // 레슨 이미지 DB 접근
	private final LessonMapper lessonMapper;
	// private final UserRepository userRepository; TODO: User 완성되면, UserRepository 추가

	// 새로운 레슨을 생성하는 메서드
	@Transactional
	public LessonResponseDto createLesson(LessonCreateRequestDto requestDto, Long userId) {
		// TODO: User 완성되면,  User 조회 로직 추가
		// User user = userRepository.findById(userId)
		//     .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 일단은, 임시로 Long userId 사용
		Lesson lesson = lessonMapper.toEntity(requestDto, userId);

		// 레슨을 DB에 저장
		Lesson savedLesson = lessonRepository.save(lesson);

		// 레슨 이미지들을 DB에 저장
		List<LessonImage> savedImages = saveLessonImages(savedLesson, requestDto.lessonImages());

		// 저장된 엔티티를 응답 DTO로 변환하여 반환
		return lessonMapper.toResponseDto(savedLesson, savedImages);
	}

	// 레슨 이미지들을 db에 저장하는 메서드
	private List<LessonImage> saveLessonImages(Lesson lesson, List<String> imageUrls) {
		// null 체크: 이미지가 없는 경우 빈 리스트 반환
		if (imageUrls == null || imageUrls.isEmpty()) {
			return List.of();
		}

		List<LessonImage> lessonImages = imageUrls.stream()
			.map(url -> LessonImage.builder()
				.lesson(lesson)
				.imageUrl(url)
				.build())
			.toList();

		return lessonImageRepository.saveAll(lessonImages);

	}

}
