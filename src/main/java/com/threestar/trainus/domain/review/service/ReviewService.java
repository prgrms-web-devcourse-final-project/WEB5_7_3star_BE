package com.threestar.trainus.domain.review.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.lesson.student.service.StudentLessonService;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.service.AdminLessonService;
import com.threestar.trainus.domain.metadata.service.ProfileMetadataService;
import com.threestar.trainus.domain.review.dto.ReviewCreateRequestDto;
import com.threestar.trainus.domain.review.dto.ReviewCreateResponseDto;
import com.threestar.trainus.domain.review.dto.ReviewPageResponseDto;
import com.threestar.trainus.domain.review.entity.Review;
import com.threestar.trainus.domain.review.mapper.ReviewMapper;
import com.threestar.trainus.domain.review.repository.ReviewRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.service.UserService;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;
import com.threestar.trainus.global.utils.PageLimitCalculator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

	private final ProfileMetadataService profileMetadataService;
	private final ReviewRepository reviewRepository;
	private final AdminLessonService adminLessonService;
	private final StudentLessonService studentLessonService;
	private final UserService userService;

	//참여자 테이블에 있는지도 검증 필요 횟수도 한번으로 제한

	@Transactional
	public ReviewCreateResponseDto createReview(ReviewCreateRequestDto reviewRequestDto, Long lessonId, Long userId) {
		Lesson findLesson = adminLessonService.findLessonById(lessonId);

		LocalDateTime reviewEndDate = findLesson.getEndAt().plusDays(7);

		if (LocalDateTime.now().isBefore(findLesson.getEndAt()) && LocalDateTime.now().isAfter(reviewEndDate)) {
			throw new BusinessException(ErrorCode.INVALID_REVIEW_DATE);
		}

		User findUser = userService.getUserById(userId);
		User lessonLeader = userService.getUserById(findLesson.getLessonLeader());

		//참여자 테이블 검증 추후 추가 -> lessonId 와 userId 다 갖고 있는지
		studentLessonService.checkValidLessonParticipant(findLesson, findUser);

		if (reviewRepository.existsByReviewer_IdAndLessonId(findUser.getId(), findLesson.getId())) {
			throw new BusinessException(ErrorCode.INVALID_REVIEW_COUNT);
		}

		Review newReview = reviewRepository.save(Review.builder()
			.reviewer(findUser)
			.reviewee(lessonLeader)
			.lesson(findLesson)
			.content(reviewRequestDto.content())
			.rating(reviewRequestDto.rating())
			.image(reviewRequestDto.reviewImage())
			.build());

		profileMetadataService.increaseReviewCountAndRating(lessonLeader.getId(), reviewRequestDto.rating());
		return ReviewMapper.toReviewResponseDto(newReview);
	}

	@Transactional(readOnly = true)
	public ReviewPageResponseDto readAll(Long userId, int page, int pageSize) {
		return ReviewMapper.toReviewPageResponseDto(userId,
			reviewRepository.findByReviewee_Id(userId).stream()
				.map(ReviewMapper::toReviewViewResponseDto)
				.toList(),
			reviewRepository.count(userId, PageLimitCalculator.calculatePageLimit(page, pageSize, 5))
		);
	}
}
